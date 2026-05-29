# Phase 2 / audit タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.audit` |
| 計画 | [50-audit.md](../plans/50-audit.md) |
| 検証コマンド | `.\mvnw.cmd clean verify`（IT 込み） |
| ステータス | Not started |

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。非推奨 API は不使用、Spring 標準イディオム（`@AutoConfiguration` / `@ConditionalOn*` / constructor injection）に揃える。

---

## AUDIT-01: AuditEvent（record）

- **概要**: 業務上の 1 監査イベントを表す不変値オブジェクト。
- **対象ファイル（新規）**: `.../batch/audit/AuditEvent.java`
- **準拠仕様**: Java 21 record。必須: `occurredAt` / `eventType` / `message`、任意: `jobName` / `jobExecutionId` / `bizDate` / `requestId` / `attributes`。compact ctor で `attributes` を `Map.copyOf` で unmodifiable 化（null は空マップ既定）。
- **受け入れ条件**: 必須欠落（null）でコンストラクト不可、任意は null 許容、`attributes` 改変不可。
- **依存**: なし。

## AUDIT-02: AuditEventBuilder

- **概要**: 多数の任意フィールドを伴う event 構築を簡潔にする fluent API。
- **対象ファイル（新規）**: `.../batch/audit/AuditEventBuilder.java`
- **準拠仕様**: コンストラクタは private、`AuditEventBuilder.builder()` で取得。setter は自身を返す。`context(JobExecution)` で `jobName` / `jobExecutionId` と `StandardJobParameters` の `BIZ_DATE` / `REQUEST_ID` を一括設定。`attribute(k,v)` で `attributes` に追加。`build()` で `occurredAt` 未設定なら `Instant.now()` を埋めて `AuditEvent` を返す。
- **受け入れ条件**: 各 setter が連鎖可能、`context(...)` で 4 フィールドが埋まる、`occurredAt` 自動付与、`build` 後に内部 map を変更しても event の `attributes` は不変。
- **依存**: AUDIT-01。

## AUDIT-03: AuditEventPublisher（インターフェース）

- **概要**: 監査イベントの発行口。
- **対象ファイル（新規）**: `.../batch/audit/AuditEventPublisher.java`
- **準拠仕様**: `void publish(AuditEvent event);` のみ。例外は基本投げない契約を javadoc に明記。
- **受け入れ条件**: コンパイル通過、SPI として実装差し替え可能。
- **依存**: AUDIT-01。

## AUDIT-04: LoggingAuditEventPublisher（デフォルト実装）

- **概要**: 専用ロガー `org.koikifw.audit` に Marker `AUDIT` 付きで INFO 出力する参照実装。
- **対象ファイル（新規）**: `.../batch/audit/LoggingAuditEventPublisher.java`
- **準拠仕様**: `org.slf4j.LoggerFactory.getLogger("org.koikifw.audit")`、`org.slf4j.MarkerFactory.getMarker("AUDIT")`。フォーマットは key=value 構造化（plan §4 参照）。publisher 内で発生した例外は warn ログのみで握り潰す（業務を止めない）。
- **受け入れ条件**: ロガー名が `org.koikifw.audit`、Marker `AUDIT` 付き、出力に必須＋任意フィールドが含まれる。null フィールドは "-" 等で省略表記。
- **依存**: AUDIT-01。

## AUDIT-05: core 自動構成への登録

- **概要**: `KoikiBatchProperties` に `Audit.enabled` を追加し、`LoggingAuditEventPublisher` を bean 登録。
- **対象ファイル（変更）**: `.../batch/core/KoikiBatchProperties.java`（nested `Audit` 追加）、`.../batch/core/BatchCoreAutoConfiguration.java`（`@Bean @ConditionalOnMissingBean @ConditionalOnProperty(prefix="koiki.batch.audit", name="enabled", matchIfMissing=true)` で登録）。
- **準拠仕様**: 既存 Phase 1 と同じ条件付き登録パターン。Boot のインフラ bean は再定義しない。
- **受け入れ条件**: 既定で `AuditEventPublisher` が1個 bean 登録される。`koiki.batch.audit.enabled=false` で生成されない（スライステストで確認）。
- **依存**: AUDIT-04。

## AUDIT-06: 単体テスト

- **概要**: AuditEvent / Builder / LoggingAuditEventPublisher の不変性・契約・出力を検証。
- **対象ファイル（新規）**: `AuditEventTest.java` / `AuditEventBuilderTest.java` / `LoggingAuditEventPublisherTest.java`、および既存 `BatchCoreAutoConfigurationTest.java` の拡張。
- **準拠仕様**: JUnit 5。Logback `ch.qos.logback.core.read.ListAppender` を `org.koikifw.audit` に貼り、`ILoggingEvent.getMarker()` / `getFormattedMessage()` で検証。
- **受け入れ条件**:
  - AuditEvent: 必須 null で `NullPointerException`、`attributes` 変更不可。
  - Builder: `context(JobExecution)` が 4 フィールドを埋める、`occurredAt` 自動付与、内部 map と event の隔離。
  - Publisher: ロガー名 `org.koikifw.audit`、Marker `AUDIT`、フィールドが出力に現れる。例外を内部で握り潰す。
  - 自動構成: 既定で `AuditEventPublisher` 単一 bean、opt-out で非生成。
- **依存**: AUDIT-01..05。

## AUDIT-07: 参照ジョブから発行 + IT

- **概要**: `customer-daily-sync` タスクレットから完了時に `CUSTOMER_DAILY_SYNC_COMPLETED` を発行し、IT で監査ロガーに乗ることを確認。
- **対象ファイル（変更）**: `CustomerDailySyncJobConfig.java`（`AuditEventPublisher` を DI、タスクレットで `publish(...)`）。
- **対象ファイル（新規）**: `CustomerDailySyncAuditIT.java`（または既存 IT 拡張、ただし MDC IT と独立した方が見通しが良いので新規推奨）。
- **準拠仕様**: ListAppender を `org.koikifw.audit` ロガーに装着、ジョブ実行後にイベントが 1 件以上採集されることを assert。eventType と jobName を最低限確認。
- **受け入れ条件**: IT 単体／`.\mvnw.cmd clean verify` の双方で通過。
- **依存**: AUDIT-01..05。

---

## メモ

- 永続化（DB / 外部キュー）版 publisher は本タスクではスコープ外（plan §スコープ外参照）。
- アプリ側の logback ファイル分離（audit を別ファイルに分割）は demo の範囲外。必要な顧客アプリで logback-spring.xml を導入する。
- Phase 1 の MDC は監査出力にも自動的に付与される（同一スレッド・SLF4J 仕様）。ロガー名で経路を分けつつ、相関キーは共有できる。
