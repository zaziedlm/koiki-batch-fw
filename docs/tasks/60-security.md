# Phase 3 / security タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.security` |
| 計画 | [60-security.md](../plans/60-security.md) |
| 検証コマンド | `.\mvnw.cmd clean verify`（IT 込み） |
| ステータス | 実装済み（SEC-01〜08 完了、`mvnw clean verify` 通過） |

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。非推奨 API は不使用、Spring 標準イディオム（`@AutoConfiguration` / `@ConditionalOn*` / constructor injection）に揃える。Logback コンバータは `CompositeConverter` 継承＋`logback-spring.xml` の `<conversionRule converterClass=...>` 登録（Logback 1.5.x 仕様、計画 §準拠仕様）。

---

## SEC-01: Masker（インターフェース）

- **概要**: 値 1 個をマスクする最小 SPI。
- **対象ファイル（新規）**: `.../batch/security/Masker.java`
- **準拠仕様**: `String mask(String value);` のみ。`null` 入力の扱い（実装依存だが既定実装は `null` を返す）を javadoc に明記。キー判定は持たない（呼び出し側責務）。
- **受け入れ条件**: コンパイル通過、実装差し替え可能（SPI）。
- **依存**: なし。

## SEC-02: RedactingMasker（デフォルト実装）

- **概要**: 入力によらず固定トークンを返す全伏字マスカ。
- **対象ファイル（新規）**: `.../batch/security/RedactingMasker.java`
- **準拠仕様**: コンストラクタでマスクトークン（既定 `***`）を受ける。`mask(null)` は `null`、それ以外はトークン。
- **受け入れ条件**: 任意入力で同一トークン、`null` は `null`、トークン指定が反映。
- **依存**: SEC-01。

## SEC-03: KoikiBatchProperties への Security.Masking 追加

- **概要**: `koiki.batch.security.masking.*` を束ねる nested プロパティ。
- **対象ファイル（変更）**: `.../batch/core/KoikiBatchProperties.java`
- **準拠仕様**: `Security { Masking masking }`、`Masking { boolean enabled=true; String mask="***"; Set<String> sensitiveKeys=new LinkedHashSet<>(); }`。getter を提供。
- **受け入れ条件**: `koiki.batch.security.masking.enabled` / `mask` / `sensitive-keys` がバインドされる。
- **依存**: なし。

## SEC-04: LoggingAuditEventPublisher へのマスキング適用

- **概要**: 機密キーに該当する attribute 値を `Masker` でマスクして出力。
- **対象ファイル（変更）**: `.../batch/audit/LoggingAuditEventPublisher.java`
- **準拠仕様**: `Masker` と `Set<String> sensitiveKeys` を受ける新コンストラクタを追加。既存の引数なしコンストラクタは「マスクなし」として維持（後方互換）。`format(...)` で attribute のキーが `sensitiveKeys` に含まれる場合のみ値を `masker.mask(value)` に置換。masker が `null` または集合が空なら従来出力。例外は従来どおり握り潰す（業務を止めない）。
- **受け入れ条件**: 機密キー値が `***`、非機密キーは素通し、masker/集合なしで従来出力、マスク中の例外で publish が落ちない。
- **依存**: SEC-01。

## SEC-05: MaskingPatternConverter（Logback コンバータ）

- **概要**: ログメッセージ中の正規表現マッチ箇所をマスクするアプリログ用フック。
- **対象ファイル（新規）**: `.../batch/security/MaskingPatternConverter.java`
- **準拠仕様**: `extends ch.qos.logback.core.pattern.CompositeConverter<ILoggingEvent>`、`transform(ILoggingEvent, String in)` をオーバーライド。`getOptionList()` の各要素を正規表現として `Pattern.compile`（`start()` でプリコンパイル）。マッチをマスクトークン（既定 `***`）で置換。option なしは恒等（原文返却）。不正な正規表現は `start()` で `addError` し、当該パターンを無効化（落とさない）。
- **対象ファイル（変更）**: `.../libkoiki-batch/pom.xml` に `logback-classic` を `optional=true` で追加。
- **受け入れ条件**: 単一/複数パターンで該当箇所が置換、option なしで恒等、マッチなしで原文維持、不正パターンで例外を投げない。
- **依存**: なし（SEC のマスクトークン定数を共有する場合は SEC-02 と整合）。

## SEC-06: core 自動構成への登録

- **概要**: `Masker` bean 登録と、監査 publisher への masker/機密キー配線。
- **対象ファイル（変更）**: `.../batch/core/BatchCoreAutoConfiguration.java`
- **準拠仕様**:
  - `@Bean @ConditionalOnMissingBean @ConditionalOnProperty(prefix="koiki.batch.security.masking", name="enabled", matchIfMissing=true)` で `RedactingMasker`（トークンは `KoikiBatchProperties` から）。
  - 既存 `auditEventPublisher()` を変更し、`ObjectProvider<Masker>` と `KoikiBatchProperties`（`sensitiveKeys`）から `LoggingAuditEventPublisher` を配線。masker 不在時は素通し版。
- **受け入れ条件**: 既定で `Masker` 単一 bean。`koiki.batch.security.masking.enabled=false` で `Masker` 非生成かつ監査が素通し。アプリが `Masker` を定義すれば優先（`@ConditionalOnMissingBean`）。
- **依存**: SEC-02, SEC-03, SEC-04。

## SEC-07: 単体テスト

- **概要**: Masker / publisher マスク / converter / 自動構成を検証。
- **対象ファイル（新規）**: `RedactingMaskerTest.java` / `MaskingPatternConverterTest.java`、既存 `LoggingAuditEventPublisherTest.java` / `BatchCoreAutoConfigurationTest.java` の拡張。
- **準拠仕様**: JUnit 5。converter は直接 `start()`→`convert(event)` で検証（Logback `LoggingEvent` を組む）。publisher は ListAppender で出力文字列を検証。
- **受け入れ条件**:
  - RedactingMasker: 固定トークン、`null` 透過、トークン設定反映。
  - Publisher: 機密キー `***`、非機密素通し、masker/集合なしで従来、例外握り潰し。
  - Converter: 置換・恒等・複数パターン・不正パターン非throw。
  - 自動構成: 既定 `Masker` 単一 bean、opt-out で非生成かつ監査素通し。
- **依存**: SEC-01..06。

## SEC-08: 参照ジョブへの適用 + IT

- **概要**: `customer-daily-sync` で機密 attribute を発行し、監査ログで `***` 化を IT 確認。
- **対象ファイル（変更）**: `CustomerDailySyncJobConfig.java`（機密 attribute を 1 つ付与して発行）、`application.yml`（`koiki.batch.security.masking.sensitive-keys` に当該キー設定）。
- **対象ファイル（新規）**: `CustomerDailySyncMaskingIT.java`（監査 ListAppender で機密値 `***`・非機密素通しを assert）。
- **準拠仕様**: 既存 `CustomerDailySyncAuditIT` のパターン踏襲（`org.koikifw.audit` ロガー + Marker）。
- **受け入れ条件**: IT 単体／`.\mvnw.cmd clean verify` の双方で通過。機密値が素のまま出ないこと。
- **依存**: SEC-01..06。

---

## メモ

- PII クラス別の標準マスキングルール・部分マスク・資格情報ストア連携は本タスクではスコープ外（plan §スコープ外参照）。
- `MaskingPatternConverter` は Spring 管理外（Logback が生成）。`logback-classic` を `optional` 依存で追加するため、Logback を使わないアプリ（log4j2 等）には影響しない。
- アプリログのテキストマスクは `logback-spring.xml` の `<conversionRule converterClass=...>` ＋ `%mask(...)` で opt-in。デモは任意（IT はコンバータ直接検証で安定化）。
- 永続化や認可は本フェーズ対象外。decision-log には実装確定時に「マスキング境界の方針（値レベル＝キー指定／テキストレベル＝Logback コンバータ）」を追記する。
</content>
