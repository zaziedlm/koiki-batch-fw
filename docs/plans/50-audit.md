# Phase 2 / audit 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.audit` |
| ステータス | Done（`v0.1.0` 初期スコープを実装済み。formatter escape 強化済み） |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/50-audit.md) / [platform-capabilities](../batch/platform-capabilities.md) / [decision-log](../batch/decision-log.md) |

## 狙い

**業務上意味のある変更・制御イベント**を、通常ログとは独立して記録するための共通基盤を提供する。現在は `AuditEvent`、`AuditEventBuilder`、`AuditEventPublisher`、`LoggingAuditEventPublisher` を実装済みで、参照アプリから監査イベントを発行する integration test も通過している。

監査と通常ログの違い（[platform-capabilities](../batch/platform-capabilities.md) §Enterprise Concerns / Audit）:

| | 通常ログ（Phase 1） | 監査（本フェーズ） |
| --- | --- | --- |
| 目的 | 運用診断・障害解析 | 「いつ・どのバッチが・何を変更したか」の事後説明 |
| 受け手 | 運用者 | 監査担当・統制部門 |
| 保存要件 | 比較的短期、可読性重視 | 改ざんなく長期保存、再現可能性重視 |
| 出力先 | アプリのロガー（`org.koikifw` 系） | 専用ロガー `org.koikifw.audit`（Marker `AUDIT` 付与） |

## 準拠仕様（Spring Batch 6.0.x / Spring Boot 4 / SLF4J）

- 監査は Spring Batch のリスナーではなく **業務サービス／タスクレットからの明示的な発行**で動かす（Spring Batch のジョブ完了イベントは粒度が粗いため、業務意味の単位で発火させる）。
- 出力経路は `org.slf4j.Logger` ＋ `org.slf4j.MarkerFactory.getMarker("AUDIT")`。アプリの logback-spring.xml で `org.koikifw.audit` ロガーや `AUDIT` Marker を別ファイル appender に振り分けられる。
- 永続化（DB / S3 等）方式は v0.1.0 では **deferred**（Publisher インターフェースを差し替え可能な構造にして、後続で実装）。

## 設計

### 1. `AuditEvent`（record）

不変な値オブジェクトとして、業務上の 1 イベントを表す。

```java
public record AuditEvent(
    Instant occurredAt,                  // 発生時刻（既定: Instant.now())
    String eventType,                    // 例: "CUSTOMER_SYNCED" / 業務語彙の大文字スネーク
    String message,                      // 人間可読サマリ
    String jobName,                      // optional
    Long jobExecutionId,                 // optional
    String bizDate,                      // optional（yyyyMMdd 文字列）
    String requestId,                    // optional
    Map<String, String> attributes       // optional・unmodifiable・空マップ既定
)
```

- 必須は `occurredAt` / `eventType` / `message`。残りは省略可（null 可、attributes は空マップ既定）。
- record の compact constructor で `attributes` を unmodifiable コピーに正規化。

### 2. `AuditEventBuilder`（fluent API）

8 フィールドのうち多くが optional なので、可読性のため Builder を提供。

```java
AuditEvent event = AuditEventBuilder.builder()
    .eventType("CUSTOMER_SYNCED")
    .message("Daily customer sync completed")
    .context(jobExecution)               // jobName / jobExecutionId / bizDate / requestId を一括設定
    .attribute("customerCount", "1234")
    .build();
```

- `context(JobExecution)` 便利メソッド: `JobExecution.getJobInstance().getJobName()` / `getId()` と、`StandardJobParameters.BIZ_DATE` / `REQUEST_ID` をパラメータから取り出して埋める。MDC との一貫性。
- `occurredAt` 未指定時は `Instant.now()` を自動付与。

### 3. `AuditEventPublisher`（インターフェース）

```java
public interface AuditEventPublisher {
    void publish(AuditEvent event);
}
```

- 同期発行・戻り値なし。例外は基本的に呼び出し元に投げない（監査出力で業務を止めない）。
- 差し替え可能（DB 永続化版・Kafka 版などへの将来拡張は同 IF 実装で対応）。

### 4. `LoggingAuditEventPublisher`（デフォルト実装）

- 専用ロガー `org.koikifw.audit` に **INFO** で出力。
- `Marker AUDIT` 付き（`MarkerFactory.getMarker("AUDIT")`）。
- 出力フォーマット例（人間可読・key=value の構造化スタイル。JSON 化はアプリ選択肢）:
  ```
  occurredAt=2026-05-30T01:23:45Z eventType=CUSTOMER_SYNCED message="Daily customer sync completed"
   jobName=customer-daily-sync jobExecutionId=42 bizDate=20260530 requestId=... attr.customerCount=1234
  ```
- 例外発生時は内部で warn ログを出すのみ（throw しない）。

### 5. `BatchCoreAutoConfiguration` への登録

- `KoikiBatchProperties` に `Audit { boolean enabled = true; }` を追加。
- `LoggingAuditEventPublisher` を `@Bean @ConditionalOnMissingBean @ConditionalOnProperty(prefix="koiki.batch.audit", name="enabled", matchIfMissing=true)` で登録。
- Phase 1 と同じく Boot のインフラ bean は再定義しない。

### 6. 適用方式（opt-in、Phase 0/1 と整合）

- 参照ジョブのタスクレット／業務サービスから `AuditEventPublisher` を **DI** して呼ぶ（明示発行）。
- 参照アプリ `customer-daily-sync` タスクレットで `CUSTOMER_DAILY_SYNC_COMPLETED` イベントを 1 件発行することでデモ。

### 7. アプリ側の routing（demo のみ、framework は強制しない）

参照アプリの `application.yml` で audit ロガーのレベルを INFO に設定（既定でも INFO 以上）。本格的なファイル分離は logback-spring.xml の追加が必要になるが、これは将来課題として deferred に明記。

## 検証

- **単体テスト**:
  - `AuditEvent` の必須／任意フィールド・compact ctor の attributes 不変化。
  - `AuditEventBuilder` の `context(JobExecution)` がジョブ系コンテキストを埋めること。`occurredAt` 自動設定。
  - `LoggingAuditEventPublisher` が `org.koikifw.audit` ロガーへ Marker 付きの INFO 出力をすること（Logback `ListAppender` で採集）。
- **参照アプリ IT**:
  - `customer-daily-sync` 実行で監査イベントが 1 件発行されること（`org.koikifw.audit` ロガーに ListAppender を貼って assert）。
  - MDC（Phase 1）と同時に動作してログイベントが分離していること（イベントは `org.koikifw.audit` 経由、通常ログは `org.koikifw` 経由）。
- **検証コマンド**: `.\mvnw.cmd clean verify`。

## スコープ外 / deferred

- **監査レコードの永続化**（DB / オブジェクトストレージ / 専用キュー）: Phase 2 では Publisher インターフェース＋ログ参照実装まで。永続化版実装は別ラウンド（DB 設計・改ざん耐性要件を伴うため）。
- **JSON 構造化フォーマッタ / Filebeat 等の集約パイプライン**: アプリ側で実装する選択肢（framework は強制しない）。
- **Spring Batch のジョブ完了イベントからの自動発行**: 業務粒度と合わないため取らない。将来「ジョブ完了監査」のような汎用イベントを足す場合は別途検討。
- **マスキング**: Phase 3 security で扱う。本フェーズは「PII を `attributes` に入れない」運用ガイドのみ。
- **複数 Publisher の合成**（例: ログ＋DB を同時発行）: 明示的に複数 bean を束ねる仕組みは未提供。必要になったときに `CompositeAuditEventPublisher` 等を追加。

## 運用ノート

- **Logback ロガー継承（appender additivity）**: 既定では `org.koikifw.audit` への出力は親ロガー `org.koikifw` の appender にも伝播する。監査を**完全に独立したファイル／宛先**に分離したい場合、アプリの `logback-spring.xml` で audit 専用 appender を定義し、当該ロガーで `<logger name="org.koikifw.audit" additivity="false">` を設定する。framework 側では additivity を強制しない（アプリ要件で選択）。
