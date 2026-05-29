# Phase 1 / observability タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.observability` |
| 計画 | [40-observability.md](../plans/40-observability.md) |
| 検証コマンド | `.\mvnw.cmd clean verify`（IT 込み） |
| ステータス | Not started |

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。非推奨 API（`JobExplorer` / `JobLauncher` / `CommandLineJobRunner`）は使わない。MDC は SLF4J 標準 `org.slf4j.MDC`、リスナーは `org.springframework.batch.core.listener`。

---

## OBSV-01: CorrelationKeys 定数

- **概要**: MDC キーを定数化（マジックストリング排除）。
- **対象ファイル（新規）**: `.../batch/observability/CorrelationKeys.java`
- **準拠仕様**: 単純な定数ホルダ（インスタンス化不可）。キー名は `koiki.job.*` / `koiki.step.*` 名前空間。
- **受け入れ条件**: 7 キー（job: name/execId/bizDate/requestId/tenant、step: name/execId）が定数として参照可能。
- **依存**: なし。

## OBSV-02: JobLogListener 本実装（既存スタブ差し替え）

- **概要**: `beforeJob` で MDC にジョブ系キーを設定し開始ログ、`afterJob` で完了ログ＋ MDC 解除。
- **対象ファイル（変更）**: `.../batch/observability/JobLogListener.java`
- **準拠仕様**: `org.springframework.batch.core.listener.JobExecutionListener`、`JobExecution.getJobInstance().getJobName()` / `getId()` / `getJobParameters().getString(...)`。任意項目は `null` 時スキップ。`finally` で MDC remove を保証。
- **受け入れ条件**: 開始時に MDC に期待キーが入り、終了後にすべて remove されている。`StandardJobParameters` のキーを参照（execution パッケージ依存）。
- **依存**: OBSV-01。

## OBSV-03: StepLogListener 新規

- **概要**: ステップ実行の MDC 補強と開始/終了ログ。
- **対象ファイル（新規）**: `.../batch/observability/StepLogListener.java`
- **準拠仕様**: `org.springframework.batch.core.listener.StepExecutionListener`、`afterStep` 戻り値は `null`（既存 `ExitStatus` を尊重）。`finally` で MDC remove。
- **受け入れ条件**: `beforeStep` 後にステップ系キーが MDC に存在、`afterStep` 後に remove される。read/write 件数を完了ログに含める。
- **依存**: OBSV-01。

## OBSV-04: core 自動構成への bean 登録

- **概要**: `BatchCoreAutoConfiguration` に `JobLogListener` / `StepLogListener` を条件付き登録。
- **対象ファイル（変更）**: `.../batch/core/BatchCoreAutoConfiguration.java`
- **準拠仕様**: `@Bean @ConditionalOnMissingBean @ConditionalOnProperty(prefix="koiki.batch.logging.correlation", name="enabled", matchIfMissing=true)`。Boot 提供 bean は再定義しない。
- **受け入れ条件**: 既定で2 listener bean が生成され、`koiki.batch.logging.correlation.enabled=false` で生成されない（スライステストで確認）。
- **依存**: OBSV-02, OBSV-03。

## OBSV-05: 単体テスト

- **概要**: MDC の put/remove を検証。
- **対象ファイル（新規）**: `JobLogListenerTest.java` / `StepLogListenerTest.java`、および既存 `BatchCoreAutoConfigurationTest.java` を拡張。
- **準拠仕様**: JUnit 5。MDC は呼び出し前後で読む。`JobExecution`/`StepExecution` は Mockito で組み立て。
- **受け入れ条件**: (a) beforeJob/Step 後に MDC に期待キー、(b) afterJob/Step 後に MDC 解除、(c) `BatchCoreAutoConfigurationTest` に listener 2 件の存在確認と `correlation.enabled=false` で非生成を確認。
- **依存**: OBSV-02, OBSV-03, OBSV-04。

## OBSV-06: 参照ジョブへの opt-in 結線 + IT で MDC 確認

- **概要**: `customer-daily-sync` Job/Step に `.listener(jobLogListener)` / `.listener(stepLogListener)` を opt-in。IT で実ジョブ実行時の MDC を assert。アプリ側 `application.yml` に `logging.pattern.correlation` を設定して標準パターンへの挿入をデモ。
- **対象ファイル（変更）**: `CustomerDailySyncJobConfig.java`、`application.yml`。
- **対象ファイル（新規）**: `CustomerDailySyncMdcIT.java`（または既存 IT 拡張）。
- **準拠仕様**: ロガーは `org.slf4j.LoggerFactory`、Logback `ch.qos.logback.core.read.ListAppender<ILoggingEvent>` を `org.koikifw` ロガーに装着して MDC を取得。
- **受け入れ条件**: IT 実行で `ILoggingEvent.getMDCPropertyMap()` が `koiki.job.name` / `koiki.job.execId` / `koiki.job.bizDate` / `koiki.job.requestId` / `koiki.step.name` を含む。`.\mvnw.cmd clean verify` が通る。
- **依存**: OBSV-01..05。

---

## メモ

- アプリ側の `logging.pattern.correlation` は demo であり、framework は強制しない（PII 観点でアプリが選択）。
- 全 Job への listener 自動適用は本タスクでは行わない（Phase 0 の guard/validator と同様 opt-in）。必要なら別フェーズで検討。
- マスキングは Phase 3（security）扱い。
