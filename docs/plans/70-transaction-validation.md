# Phase 4 / transaction・validation 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.transaction` / `org.koikifw.libkoiki.batch.validation` |
| ステータス | Draft |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/70-transaction-validation.md) / [DB管理アーキテクチャ](../batch/db-management-architecture.md) / [decision-log](../batch/decision-log.md) |

## 狙い

SB6 の DB管理「あるべき論」（[db-management-architecture.md](../batch/db-management-architecture.md)）に基づき、**業務 RDBMS アクセス・チャンク管理・トランザクション境界**を責務分離して共通化し、**再利用可能な検証契約**を提供する。参照アプリに **DB-backed chunk ジョブ**を追加し、コミット境界の明示・検証契約の使用・メタデータ永続（リスタート土台）を実証する。

## 準拠仕様（確定方針・Stage A 合意）

- メタデータ: `ResourcelessJobRepository` を framework 既定維持。**Boot 4/SB6 では DataSource だけでは JDBC 化しない**（既定 resourceless）。参照アプリが `@EnableBatchProcessing @EnableJdbcJobRepository(dataSourceRef="dataSource", transactionManagerRef="transactionManager")` で明示オプトイン（Boot の exit-code/launcher は維持）。framework モジュールには置かない。詳細は [db-management-architecture.md](../batch/db-management-architecture.md)。
- トポロジ: **単一 DataSource 共有**（メタ+業務を同一 TM、chunk write とステップメタ更新を原子コミット）。
- スキーマ: **Flyway 一元**、`spring.batch.jdbc.initialize-schema=never`。`BATCH_*` も Flyway 移行で作成（SB 同梱 `schema-h2.sql` を**逐語コピー**、推測しない）。
- TM bean は framework で作らない（decision 継続）。
- validation: 軽量 framework 契約（Bean Validation 不採用）。

## 設計

### A. transaction（境界・方針）

1. `KoikiBatchProperties` に nested `Transaction { int defaultCommitInterval = 100; }`（既定コミット境界＝チャンクサイズ）+ getter。プレフィックス `koiki.batch.transaction.*`。
2. `transaction/package-info.java` を充実させ、コミット境界/ロールバック/リスタート方針と [db-management-architecture.md](../batch/db-management-architecture.md) への参照を記述（**TX 振る舞いはコードに隠さず**、値は properties 経由でジョブが明示使用）。
3. TM bean・ヘルパーは作らない。

### B. validation（軽量契約）

1. `Validator<T>`（`@FunctionalInterface`）… `ValidationResult validate(T target);`（**非throw**）。Spring Batch の `JobParametersValidator` とは別物（入力レコード/業務前提）。
2. `ValidationError`（record: `String field, String message`）。
3. `ValidationResult`（record: `List<ValidationError> errors`、compact ctor で不変化、`isValid()`、static `valid()` / `of(List)` / `withError(field,msg)` 程度の生成補助、`throwIfInvalid(String context)` で `ValidationException` を投げる便宜メソッド）。
4. `ValidationException extends BusinessException`（`fault` 階層・終了コード **20**）。`List<ValidationError>` を保持し、メッセージに要約。
5. 既存 `KoikiJobParametersValidator`（`execution`）は据え置き。役割差を javadoc 明記。
6. 自動構成 bean は作らない（契約はアプリ実装）。

### C. reference app — DB-backed chunk ジョブ

1. 依存追加: `spring-boot-starter-jdbc`、`com.h2database:h2`、`org.flywaydb:flyway-core`。
2. `application.yml`: H2 在メモリ DataSource、`spring.batch.jdbc.initialize-schema=never`、Flyway 有効（`spring.flyway.locations=classpath:db/migration/refapp`）。
3. Flyway 移行（`src/main/resources/db/migration/refapp/`）:
   - 業務テーブル: 入力 `customer_input(id, external_id, email, created_at)` / 出力 `customer(id, external_id, email)`（placeholder `customer_work` は刷新）。
   - `BATCH_*` メタデータ: SB 同梱 `org/springframework/batch/core/schema-h2.sql` を逐語コピーした移行。
4. `CustomerImportJobConfig`（job 名 `customer-import`）:
   - model `CustomerRecord(Long id, String externalId, String email)`。
   - reader `JdbcCursorItemReader<CustomerRecord>`（`customer_input`）。
   - processor `CustomerValidatingProcessor implements ItemProcessor<CustomerRecord,CustomerRecord>` … framework `Validator<CustomerRecord>`（`CustomerRecordValidator`：external_id 必須・email 形式など）で検証、`result.throwIfInvalid(...)`（→ `ValidationException`→20）。skip も選択可だがデモは fail-fast。
   - writer `JdbcBatchItemWriter<CustomerRecord>`（`customer`）。
   - step `chunk(properties.getTransaction().getDefaultCommitInterval(), transactionManager)`（**境界の明示**）。Boot 提供の `PlatformTransactionManager` を注入。
   - job: 既存同様 `KoikiJobParametersValidator` + observability listener を付与。
5. 既存 `customer-daily-sync`（tasklet）は維持。`RefBatchInfrastructureConfig` の `ResourcelessTransactionManager` は DataSource 導入で自然後退（残置・javadoc 補足）。

## 検証

- **単体（libkoiki-batch）**:
  - `ValidationResult`/`ValidationError`/`ValidationException`：不変性、`isValid()`、`throwIfInvalid` が `ValidationException`（→`BusinessException`）を投げ `FaultClassifier` で 20 に分類されること。
  - `Validator<T>` 実装の最小例。
  - `KoikiBatchProperties.Transaction.defaultCommitInterval` の既定とバインド、`BatchCoreAutoConfigurationTest` 拡張。
- **IT（ref app, Failsafe, H2）**:
  - `CustomerImportJobIT`：`customer_input` に正常データ→実行→`COMPLETED`、`customer` に全件コミット、`BATCH_STEP_EXECUTION` の write 件数一致（コミット境界・メタ永続）。
  - 検証失敗ケース：不正データ→`ValidationException`→ジョブ FAILED かつ business error(20) 系（exit-code/fault 経路）。
  - 既存 IT（observability/audit/masking/exit-code/mdc）全件グリーン維持（DataSource 導入の影響確認：各 `@SpringBootTest` で H2 在メモリ、ジョブパラメータは UUID requestId で一意）。
- **検証コマンド**: `.\mvnw.cmd clean verify`。

## スコープ外 / deferred

- 別DB分離（`@BatchDataSource`/`@BatchTransactionManager`）と非原子トポロジ。
- io reader/writer の framework 契約（Phase 5）。本フェーズは標準 SB reader/writer を使用。
- 本番DB方言（Oracle/PostgreSQL）・ロック差。テストは H2。
- skip/retry の高度ポリシー、`TransactionTemplate` ヘルパー、isolation/timeout 標準化（必要時に別途）。
- Bean Validation 連携（アプリが自前で併用可能）。

## 運用ノート

- DB-backed では業務 write とステップメタ更新が**同一 TX で原子コミット**される（単一 DataSource 共有）。リスタート時は `BATCH_*` の永続状態から再開。
- 本番は `initialize-schema=never` 前提でスキーマを Flyway 管理。`BATCH_*` を Flyway に含める（SB バージョン更新時はメタスキーマ差分を移行追加）。
