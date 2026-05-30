# Phase 4 / transaction・validation タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.transaction` / `.../validation` |
| 計画 | [70-transaction-validation.md](../plans/70-transaction-validation.md) |
| 検証コマンド | `.\mvnw.cmd clean verify`（IT 込み・H2） |
| ステータス | Not started |

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) と [DB管理アーキテクチャ](../batch/db-management-architecture.md) に従う。非推奨 API 不使用、Spring 標準イディオム。Boot に DataSource を渡し JDBC JobRepository を**自動構成**させる（`@EnableBatchProcessing` 手書き禁止）。`BATCH_*` DDL は SB 同梱 `schema-h2.sql` を逐語コピー（推測禁止）。

---

## TXV-01: transaction プロパティ + package-info

- **概要**: 既定コミット境界プロパティと境界/ロールバック方針ガイド。
- **対象（変更）**: `core/KoikiBatchProperties.java`（nested `Transaction { int defaultCommitInterval = 100; }` + getter）、`transaction/package-info.java`（方針と architecture 文書参照を追記）。
- **準拠**: `koiki.batch.transaction.*`。TM bean・ヘルパーは作らない。
- **受け入れ**: `koiki.batch.transaction.default-commit-interval` がバインドされ既定 100。
- **依存**: なし。

## TXV-02: Validator 契約

- **概要**: 入力/業務前提の検証契約。
- **対象（新規）**: `validation/Validator.java`（`@FunctionalInterface`、`ValidationResult validate(T target);`、非throw）。
- **準拠**: Spring Batch `JobParametersValidator` と別物である旨を javadoc 明記。
- **受け入れ**: コンパイル・実装差し替え可能。
- **依存**: TXV-03。

## TXV-03: ValidationResult / ValidationError

- **概要**: 検証結果の不変値オブジェクト。
- **対象（新規）**: `validation/ValidationError.java`（record `field, message`）、`validation/ValidationResult.java`（record `List<ValidationError> errors`、compact ctor 不変化、`isValid()`、static `valid()`/`of(List)`/`withError(field,msg)`、`throwIfInvalid(String context)`）。
- **準拠**: `throwIfInvalid` は無効時 `ValidationException` を投げる。
- **受け入れ**: errors 不変、`isValid()` 正、`throwIfInvalid` の throw/non-throw。
- **依存**: TXV-04（throwIfInvalid のため）。

## TXV-04: ValidationException

- **概要**: 検証失敗の fail-fast 例外（終了コード20）。
- **対象（新規）**: `validation/ValidationException.java`（`extends org.koikifw.libkoiki.batch.fault.BusinessException`、`List<ValidationError>` 保持、メッセージ要約）。
- **準拠**: `FaultClassifier` で `BUSINESS_ERROR(20)` に分類されること（`BusinessException` 継承で自動）。
- **受け入れ**: errors 取得可、`BusinessException` として分類。
- **依存**: なし（fault 既存）。

## TXV-05: validation package-info

- **概要**: 3種の検証（パラメータ/入力/業務前提）の住み分けを記述。
- **対象（変更）**: `validation/package-info.java`。
- **受け入れ**: パラメータ検証は execution 据え置き、入力/業務前提は本パッケージ、と明記。
- **依存**: TXV-02..04。

## TXV-06: reference app の DB 有効化（依存・yml・Flyway）

- **概要**: H2 + Flyway + JDBC を導入し、単一 DataSource 共有で JDBC JobRepository を自動構成。
- **対象（変更）**: `koiki_ref_batch_app/pom.xml`（`spring-boot-starter-jdbc`/`h2`/`flyway-core`）、`application.yml`（H2 datasource、`spring.batch.jdbc.initialize-schema=never`、`spring.flyway.locations=classpath:db/migration/refapp`）。
- **対象（新規/刷新）**: `db/migration/refapp/` 配下: 業務テーブル移行（`customer_input`/`customer`、placeholder 刷新）、`BATCH_*` 移行（SB `schema-h2.sql` 逐語コピー）。
- **受け入れ**: アプリ起動で Flyway が両スキーマを作成、JDBC JobRepository が有効。
- **依存**: なし。

## TXV-07: customer-import チャンクジョブ

- **概要**: read→validate→write の DB-backed chunk ジョブで境界明示・検証契約使用を実証。
- **対象（新規）**: `jobs/customer/CustomerImportJobConfig.java`、`model/CustomerRecord.java`、`jobs/customer/CustomerRecordValidator.java`（framework `Validator` 実装）、`jobs/customer/CustomerValidatingProcessor.java`。
- **準拠**: `JdbcCursorItemReader`→processor（`Validator` で検証、`throwIfInvalid`→20）→`JdbcBatchItemWriter`。`chunk(properties.getTransaction().getDefaultCommitInterval(), txManager)`。Boot 提供 TM を注入。
- **受け入れ**: 正常データで COMPLETED・出力表へコミット。
- **依存**: TXV-01..06。

## TXV-08: 単体テスト

- **対象（新規）**: `validation/ValidationResultTest.java`、`validation/ValidationExceptionTest.java`、必要なら `ValidatorTest`。`core/BatchCoreAutoConfigurationTest`/`KoikiBatchProperties` バインドの拡張。
- **準拠**: JUnit 5。`ValidationException` が `DefaultFaultClassifier` で 20 に分類されることを確認。
- **受け入れ**: 不変性・`isValid`・`throwIfInvalid`・分類・プロパティ既定/バインド。
- **依存**: TXV-01..05。

## TXV-09: IT（H2）

- **対象（新規）**: `jobs/customer/CustomerImportJobIT.java`。
- **準拠**: H2 在メモリ、`customer_input` を seed。(a) 正常→COMPLETED・`customer` 全件・`BATCH_STEP_EXECUTION` write 件数一致、(b) 不正データ→`ValidationException`→FAILED・business error(20)。
- **受け入れ**: IT 単体／`clean verify` 双方通過。既存 IT 全件グリーン維持。
- **依存**: TXV-01..07。

---

## メモ

- 別DB分離・io 契約(Phase 5)・本番方言・高度 skip/retry は本タスク対象外（plan 参照）。
- DataSource 導入は全 `@SpringBootTest` の文脈に影響。ジョブパラメータの `requestId` を UUID 一意にして JobInstance 衝突を避ける（既存 IT 準拠）。
- decision-log の DB-backed 姿勢（2026-05-30）を実装の根拠とする。
