# libkoiki-batch 共通機能 実装ロードマップ

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| 対象モジュール | `components/libkoiki-batch`（`org.koikifw.libkoiki.batch.*`） |
| ステータス | Draft（Phase 0 を詳細化済み、Phase 1 以降は要約） |
| 関連 | [architecture-batch.md](../batch/architecture-batch.md) / [platform-capabilities.md](../batch/platform-capabilities.md) / [boundaries.md](../agent/boundaries.md) / [decision-log.md](../batch/decision-log.md) / [return-code-mapping.md](../../ops/jp1/jobs/return-code-mapping.md) |

## 目的

`libkoiki-batch` は責務マップ（10 パッケージ）と `package-info` で設計が定義済みだが、実装は全てスタブ（`BatchCoreAutoConfiguration` は空、`AutoConfiguration.imports` 未登録、`ConcurrencyGuardService` は常に `true`、`JobLogListener` は空）。

本ロードマップは、共通機能を**依存順に段階実装**する計画を示す。各 Phase は「参照アプリ `koiki_ref_batch_app` の実ジョブで動作実証してから次へ進む」を原則とし（[AGENTS.md](../../AGENTS.md) の「実ジョブで検証されていない重い抽象化を先行させない」方針）、`docs/batch` の責務マップを正とする。

## 準拠仕様（Spring Batch 6.0.x / Spring 流儀）

実装・タスクは以下の確認済み事実に従う。詳細は各 Phase の plan/task 文書の「準拠仕様」節を参照。

**SB6 パッケージ再編（正しい import）**
- リスナー（`JobExecutionListener` 等）→ `org.springframework.batch.core.listener`
- `Job` / `JobExecution` / `JobInstance` 等 → `org.springframework.batch.core.job`
- `Step` / `StepExecution` / `StepContribution` → `org.springframework.batch.core.step`
- ジョブパラメータ（`JobParametersValidator` / `DefaultJobParametersValidator` / incrementer）→ `org.springframework.batch.core.job.parameters`
- explore 系 → `org.springframework.batch.core.repository.explore`

**SB6 インフラ簡素化**
- `JobRepository` が `JobExplorer` を継承（`JobExplorer` bean 不要・非推奨）
- `JobOperator` が `JobLauncher` を継承（`JobLauncher` bean 不要・非推奨、操作は `JobOperator`）
- `JobRegistry` 任意（自動登録）、トランザクションマネージャ任意（既定 `ResourcelessTransactionManager`）
- 構成入口は `@EnableBatchProcessing` + `@EnableJdbcJobRepository`、`CommandLineJobOperator` が `CommandLineJobRunner` を置換

**Spring 流儀**
- 自動構成は `@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登録。Boot の `BatchAutoConfiguration` が用意するインフラ bean は再定義せず、`@AutoConfiguration(after = BatchAutoConfiguration.class)` で listener / properties / classifier / exit-code generator を**追加**するに留める。
- コンストラクタインジェクション、`@ConfigurationProperties`、`@ConditionalOnMissingBean` / `@ConditionalOnClass` でのオプトアウト可能化。フィールドインジェクション・静的状態は避ける。
- 非推奨 API（`JobExplorer` / `JobLauncher` / `CommandLineJobRunner`）は使わない。

## 段階（Phase）

| Phase | 領域 | 状態 | plan / task |
| --- | --- | --- | --- |
| 0 | `core`, `execution`, `fault` | 実装済み（SB 6.0.3 / Boot 4.0.6、全テスト通過） | [10-core](10-core.md)・[20-execution](20-execution.md)・[30-fault](30-fault.md) / [tasks](../tasks/) |
| 1 | `observability` | 詳細化済み（実装中） | [40-observability](40-observability.md) / [tasks](../tasks/40-observability.md) |
| 2 | `audit` | 詳細化済み（実装中） | [50-audit](50-audit.md) / [tasks](../tasks/50-audit.md) |
| 3 | `security` | 詳細化済み（実装中） | [60-security](60-security.md) / [tasks](../tasks/60-security.md) |
| 4 | `transaction`, `validation`（DB-backed 基盤） | 詳細化済み（実装中） | [db-management-architecture](../batch/db-management-architecture.md) / [70-plan](70-transaction-validation.md)・[70-task](../tasks/70-transaction-validation.md) |
| 5 | `io`, `support` | 詳細化済み（実装中） | [80-io-support](80-io-support.md) / [tasks](../tasks/80-io-support.md) |

### Phase 0 — 基盤（core / execution / fault）

- **狙い**: 自動構成が効き、標準パラメータ（`job.name`/`job.bizDate`/`job.requestId`）が検証され、終了コード `0/10/20/30` が出る「最初に使えるバッチコア」。[decision-log.md](../batch/decision-log.md) の non-SNAPSHOT 化条件（usable batch core + reference job）に対応。
- **主要成果物**: `BatchCoreAutoConfiguration` の実体化 + `AutoConfiguration.imports`、`KoikiBatchProperties`、標準パラメータ契約 + `JobParametersValidator`、`ConcurrencyGuardService` 実装、例外階層 + `FaultClassifier` + 終了コード変換。
- **参照アプリ検証ポイント**: `CustomerDailySyncJobConfig`（現状空）に最小 Job/Step を組み、自動構成が拾われ・パラメータ検証が効き・終了コードが返ることを確認。
- **deferred**: 分散/DB ロックによる同時実行制御、終了コードのサブコード拡張。

### Phase 1 — observability

- **狙い**: 運用診断のための構造化ログと相関コンテキスト。
- **主要成果物**: MDC 相関（ジョブ名・実行ID・ステップ名・営業日・テナント/顧客・リクエストID）、`JobLogListener`/`StepExecutionListener` 実装、構造化ログ書式。
- **検証ポイント**: 参照ジョブ実行ログに相関キーが出力され、PII/秘密情報が出ないこと。
- **deferred**: メトリクスレジストリと命名規約（[platform-capabilities.md](../batch/platform-capabilities.md) の Deferred）。

### Phase 2 — audit

- **狙い**: 通常ログと分離した、業務上意味のある変更・制御イベントの記録。
- **主要成果物**: 監査イベントモデル、`AuditEventPublisher` インターフェース、ログ出力の参照実装。
- **検証ポイント**: 参照ジョブで監査イベントが発行され、ログとは別経路で扱えること。
- **deferred**: 監査レコードの永続化方式。

### Phase 3 — security

- **狙い**: ログ/監査への機密データ漏洩防止。
- **主要成果物**: マスキングインターフェースと、observability/audit へのマスキングフック統合、秘密情報境界。
- **検証ポイント**: マスキング対象がログ・監査に素のまま出ないこと。
- **deferred**: 個人情報クラス別の標準マスキングルール、アプリ固有の認可モデル。

### Phase 4 — transaction / validation（DB-backed 基盤）

- **狙い**: SB6 の DB管理「あるべき論」に基づき、業務 RDBMS アクセス・チャンク管理・トランザクション境界を責務分離して共通化し、再利用可能な検証契約を提供する。
- **前提（あるべき論）**: [db-management-architecture.md](../batch/db-management-architecture.md) を正とする。メタデータは Resourceless 既定・JDBC 標準オプトイン、DB-backed は単一 DataSource 共有（chunk + メタ原子コミット）、スキーマは Flyway 一元。
- **主要成果物**: `koiki.batch.transaction.defaultCommitInterval` とコミット境界/ロールバック方針ガイド（TM bean は作らない）、`Validator` 契約（`ValidationResult`/`ValidationError`/`ValidationException`→終了コード20）、参照アプリの DB-backed chunk ジョブ（H2 + Flyway、標準 SB reader/writer）。
- **検証ポイント**: 参照ジョブの chunk step で境界が `defaultCommitInterval` により明示され、`Validator` 契約が processor で使われ、メタデータが永続（リスタート土台）すること。
- **進め方**: Stage A（DB管理アーキテクチャ文書 + decision-log）を先行・合意 → Stage B（`70-transaction-validation` の plan/task と実装）。
- **deferred**: マルチDBジョブ向け標準トランザクションマネージャ選定（`@BatchDataSource`）、io reader/writer 契約（Phase 5）、本番DB方言（Oracle/PostgreSQL）。

### Phase 5 — io / support

- **狙い**: 共通 I/O アダプタ契約と中立ユーティリティ。実ジョブ要求に駆動される。
- **主要成果物**: Reader/Writer アダプタ契約、ファイル取込のアーカイブ/エラー方針、業務を持たない小ユーティリティ。
- **検証ポイント**: 参照ジョブの入出力が共通契約で表現できること。
- **deferred**: ファイル取込のアーカイブ/エラーディレクトリ標準モデル。

## ドキュメント運用

- 計画は `docs/plans/`、実行可能タスクは `docs/tasks/` に置き、`<番号>-<領域>.md` で対応させる。
- 設計判断が残るものは [decision-log.md](../batch/decision-log.md) に追記する（本ロードマップでは追記しない）。
- Phase 0 完了時に observability 以降の plan/task を本ロードマップから派生して作成する。
