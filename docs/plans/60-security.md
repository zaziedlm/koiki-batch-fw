# Phase 3 / security 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.security` |
| ステータス | Done（`v0.1.0` 初期スコープを実装済み） |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/60-security.md) / [platform-capabilities](../batch/platform-capabilities.md) / [decision-log](../batch/decision-log.md) / [audit plan](50-audit.md) |

## 狙い

**機密データ（PII・秘密情報）がログ・監査に素のまま漏れる事故を防ぐ**ための共通フックを提供する。現在は `Masker`、`RedactingMasker`、`MaskingPatternConverter` を実装済みで、audit attribute masking と Logback pattern masking のテストを持つ。

本フェーズは「マスキングの**仕組み（IF とフック）**」を提供することに限定し、「どの値がどの PII クラスか」という**標準ルールのカタログ化は deferred**（[platform-capabilities](../batch/platform-capabilities.md) §Deferred Decisions「Standard masking rules for personal data classes」）。framework はマスキングの**境界と差し込み口**を持ち、具体的なルールはアプリが供給する。

マスキングが効く 2 つの経路（Phase 1/2 と整合）:

| 経路 | 対象 | 仕組み | 制御方式 |
| --- | --- | --- | --- |
| 値レベル（監査） | `AuditEvent.attributes` の値 | `Masker` IF を `LoggingAuditEventPublisher` が適用 | **キー指定**（機密キー集合に載った attribute 値をマスク） |
| テキストレベル（アプリログ） | 任意のログメッセージ文字列 | Logback `MaskingPatternConverter`（正規表現置換） | アプリの `logback-spring.xml` で opt-in、パターンはアプリ供給 |

監査ログ（`org.koikifw.audit`）もアプリの Logback パターンを通るため、アプリが `%mask` を監査 appender にも適用すれば、`message` 等の自由記述も同時にテキストマスクできる（相乗効果）。

## 準拠仕様（Spring Boot 4.0.x / Logback 1.5.x / SLF4J）

- Logback のカスタム変換は `ch.qos.logback.core.pattern.CompositeConverter<ILoggingEvent>` を継承し `transform(ILoggingEvent, String)` をオーバーライドするのが定石。`%mask(%msg)` のように内側コンバータ出力をラップできる。
- カスタムコンバータの登録は `logback-spring.xml` の `<conversionRule conversionWord="mask" converterClass="...">`。**Logback 1.5.x（Spring Boot 4 同梱）では旧 `class` 属性ではなく `converterClass` 属性が必須**（1.3.12 / 1.5.0 のセキュリティ修正で変更）。
- `application.properties`/`yml` のみでコンバータを登録する仕組みは Spring Boot で安定提供されていない（[spring-boot#38687](https://github.com/spring-projects/spring-boot/issues/38687) は未確定）。本計画は**アプリの `logback-spring.xml` 登録方式**を正とし、プロパティのみ登録には依存しない。
- `libkoiki-batch` は現状 Logback を直接依存していない（starter 経由の推移依存）。コンバータを main で提供するため `logback-classic` を **`optional` 依存**として明示追加する（log4j2 等を使うアプリに Logback を強制しない）。
- Spring 流儀（Phase 0〜2 と同一）: `@AutoConfiguration` への追記、`@ConditionalOnMissingBean` / `@ConditionalOnProperty(matchIfMissing=true)` でオプトアウト可能化、constructor injection、Boot のインフラ bean は再定義しない。

## 設計

### 1. `Masker`（インターフェース）

値 1 個を受け取りマスク済み文字列を返す最小 SPI。

```java
public interface Masker {
    String mask(String value);
}
```

- 単一責務（値→マスク値）。「どのキーが機密か」は持たない（呼び出し側＝publisher が判定）。
- PII クラス別ルール（メール・電話・カード番号で挙動を変える等）は本 IF の**実装差し替え**で将来対応（deferred）。

### 2. `RedactingMasker`（デフォルト実装）

- 入力値によらず固定のマスクトークン（既定 `***`）を返す**全伏字**。
- トークンは構築時に指定（`KoikiBatchProperties` から注入）。`null` 入力は `null` のまま返す（NPE 回避、`appendIfNotNull` 互換）。

### 3. `KoikiBatchProperties` への `Security.Masking` 追加

```yaml
koiki.batch.security.masking:
  enabled: true            # マスキング機能全体の有効/無効
  mask: "***"              # マスクトークン
  sensitive-keys: []       # 監査 attribute のうちマスク対象とするキー集合
```

- `Security { Masking masking }` を nested で追加。`sensitiveKeys` は `Set<String>`（既定空）。

### 4. 監査統合 — `LoggingAuditEventPublisher` のマスキング適用

- `LoggingAuditEventPublisher` に**オプションの `Masker` と機密キー集合 `Set<String>`**を受ける新コンストラクタを追加。既存の引数なしコンストラクタは「マスクなし（後方互換）」として残す。
- `format(...)` の attributes 出力時、**キーが機密キー集合に含まれる場合のみ値を `masker.mask(value)` に置換**してから出力。masker が `null` または集合が空なら従来どおり素通し。
- `message`（自由記述）はキー指定の対象外。運用ガイドで「message に PII を入れない」を継続（テキストマスクが必要ならアプリの `%mask` で対応）。
- publisher は従来どおり**例外を投げない**（マスク処理失敗も warn ログのみ）。

### 5. アプリログ統合 — `MaskingPatternConverter`（Logback）

- `org.koikifw.libkoiki.batch.security.MaskingPatternConverter extends CompositeConverter<ILoggingEvent>`。
- パターン引数（コンバータの option list）で**正規表現を受け取り**、内側出力中のマッチ箇所をマスクトークンで置換: 例 `%mask(%msg){\d{12,19}}`。
- option 未指定時は**何も置換しない（恒等）**＝安全側。これにより「framework は仕組み、PII パターンはアプリ供給（deferred）」を満たす。
- マスクトークンは既定 `***`（`Masker` と概念共有のため定数化）。Spring 管理外（Logback が生成）なので DI せず自己完結。

### 6. `BatchCoreAutoConfiguration` への登録

- `Masker` を `@Bean @ConditionalOnMissingBean @ConditionalOnProperty(prefix="koiki.batch.security.masking", name="enabled", matchIfMissing=true)` で `RedactingMasker` 登録。
- 監査 publisher bean を、`ObjectProvider<Masker>` と `KoikiBatchProperties` から機密キー集合を取得して配線（masker 在不在に関わらず動く。masking 無効時は masker bean なし→素通し）。
- `MaskingPatternConverter` は bean ではない（Logback が生成）ため自動構成対象外。

### 7. 参照アプリ適用方式（demo、framework は強制しない）

- `customer-daily-sync` タスクレットで、機密 attribute（例 `accountId`）を 1 つ付けて発行し、`koiki.batch.security.masking.sensitive-keys` に当該キーを設定 → 監査ログで値が `***` になることをデモ。
- アプリログのテキストマスクは `logback-spring.xml` に `<conversionRule>` + `%mask(%msg){...}` を置くデモ（IT は ListAppender で直接コンバータを検証する方が安定。logback-spring.xml デモは任意）。

## 検証

- **単体テスト**:
  - `RedactingMasker`: 任意入力で固定トークン、`null` は `null`。トークン設定が効く。
  - `LoggingAuditEventPublisher`: 機密キーの値が `***`、非機密キーは素通し、機密キーなし／masker なしで従来出力。マスク処理が例外を投げても publish が落ちない。
  - `MaskingPatternConverter`: 正規表現マッチ箇所が置換される、option 未指定で恒等、複数パターン、マッチなしで原文維持。
  - 自動構成: 既定で `Masker` 単一 bean、`koiki.batch.security.masking.enabled=false` で非生成かつ監査が素通し（スライステスト）。
- **参照アプリ IT**:
  - `customer-daily-sync` 実行で、機密 attribute が監査ログ上 `***` で出ること（`org.koikifw.audit` に ListAppender）。
  - 非機密 attribute（jobName 等）は素のまま出ること（過剰マスクの回帰防止）。
- **検証コマンド**: `.\mvnw.cmd clean verify`。

## スコープ外 / deferred

- **PII クラス別の標準マスキングルール**（メール／電話／カード番号などの組込み正規表現・部分マスク）: 本フェーズは IF とフックのみ。標準ルールカタログは別ラウンド（[platform-capabilities](../batch/platform-capabilities.md) §Deferred）。
- **秘密情報（資格情報）ストア／Vault 連携**: 本フェーズは「ログ・監査への漏洩防止」に限定。資格情報の取得・保管境界は別途。
- **アプリ固有の認可モデル**: framework 共通要件が固まるまでアプリ側（[platform-capabilities](../batch/platform-capabilities.md) §Security）。
- **`application.properties` のみでの Logback コンバータ登録**: Spring Boot 安定機能化を待つ。当面は `logback-spring.xml` 登録。
- **MDC 値のマスキング**: 現状 MDC キーは運用識別子のみで PII を入れない規約（[CorrelationKeys](../../components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/observability/CorrelationKeys.java) javadoc）。MDC マスクは需要が出たら検討。

## 運用ノート

- `RedactingMasker` は**全伏字**（`***`）。末尾数桁残し等の部分マスクが要るアプリは `Masker` を実装し `@Bean` で差し替える（`@ConditionalOnMissingBean` によりアプリ実装が優先）。
- アプリログのテキストマスクは appender ごとに `%mask` を適用する箇所を選べる（コンソールのみ、ファイルのみ等）。監査専用 appender に適用すれば監査メッセージのテキストマスクも可能。
- 機密キー集合・正規表現の取りこぼしは事故に直結するため、アプリ側で「PII を attribute/ログに出す箇所」をレビューする運用を推奨（framework は強制しない）。
</content>
</invoke>
