# Phase 1 / observability 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.observability` |
| ステータス | Draft |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/40-observability.md) / [platform-capabilities](../batch/platform-capabilities.md) / [decision-log](../batch/decision-log.md) |

## 狙い

運用診断のための **MDC 相関コンテキスト** と **Job/Step ライフサイクルの構造化ログ**をフレームワークとして提供する。現状 [JobLogListener.java](../../components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/observability/JobLogListener.java) は `beforeJob`/`afterJob` が空のスタブ。

### 出力したい相関項目（[platform-capabilities](../batch/platform-capabilities.md) §Enterprise Concerns / Logging 準拠）
- ジョブ名 / ジョブ実行 ID / 営業日 / リクエスト ID / テナント（任意）
- ステップ名 / ステップ実行 ID

PII / 秘密情報は MDC に乗せない。`StandardJobParameters` のキー（操作上の識別子）のみを相関項目として扱う。

## 準拠仕様（Spring Batch 6.0.x / Spring Boot 4）

- `JobExecutionListener` / `StepExecutionListener` は `org.springframework.batch.core.listener`（既定メソッドあり）。`StepExecutionListener.afterStep(StepExecution)` は `ExitStatus` を返す（`null` で既存維持）。
- `StepExecution` から `getStepName()` / `getId()` / `getJobExecution()` / `getStatus()` / `getExitStatus()` / `getReadCount()` / `getWriteCount()` を取得。
- MDC は SLF4J 標準 `org.slf4j.MDC`。
- ログパターンへの MDC 反映は **Spring Boot 4 の `logging.pattern.correlation`** で実現する（フレームワークが MDC を埋め、アプリ側が `application.yml` で挿入位置を制御）。

## 設計

### 1. `CorrelationKeys`

MDC キー定数（実装の値はマジックストリング排除のため定数化）:

| 定数 | MDC キー | 出所 |
| --- | --- | --- |
| `JOB_NAME` | `koiki.job.name` | `JobInstance.getJobName()` |
| `JOB_EXEC_ID` | `koiki.job.execId` | `JobExecution.getId()` |
| `JOB_BIZ_DATE` | `koiki.job.bizDate` | `StandardJobParameters.BIZ_DATE`（存在時） |
| `JOB_REQUEST_ID` | `koiki.job.requestId` | `StandardJobParameters.REQUEST_ID`（存在時） |
| `JOB_TENANT` | `koiki.job.tenant` | `job.tenant` パラメータ（任意・存在時のみ） |
| `STEP_NAME` | `koiki.step.name` | `StepExecution.getStepName()` |
| `STEP_EXEC_ID` | `koiki.step.execId` | `StepExecution.getId()` |

### 2. `JobLogListener`（現スタブを本実装に差し替え）

- `beforeJob`:
  - 上記ジョブ系キーを MDC に `put`（任意項目はパラメータが無ければスキップ）。
  - 開始ログ `Job started` を出力。
- `afterJob`:
  - 終了ログ `Job ended: status={}, exitCode={}` を出力。
  - `finally` で MDC からジョブ系キーを `remove`（必ず後片付け）。
- 例外で異常終了しても MDC を残さない（`finally` 保証）。

### 3. `StepLogListener`（新規）

- `beforeStep`:
  - `STEP_NAME` / `STEP_EXEC_ID` を MDC に `put`。
  - 開始ログ `Step started`。
- `afterStep`:
  - 終了ログ `Step ended: status={}, read={}, write={}`。
  - `finally` で MDC からステップ系キーを `remove`。
  - 戻り値は `null`（Spring Batch の既存 `ExitStatus` を尊重）。

### 4. `BatchCoreAutoConfiguration` への登録

`logging.correlation.enabled`（既存プロパティ）が `true`（既定）かつ `@ConditionalOnMissingBean` で登録。Phase 0 と同じパターン。

```java
@Bean @ConditionalOnMissingBean
@ConditionalOnProperty(prefix = "koiki.batch.logging.correlation", name = "enabled", matchIfMissing = true)
public JobLogListener jobLogListener() { return new JobLogListener(); }

@Bean @ConditionalOnMissingBean
@ConditionalOnProperty(prefix = "koiki.batch.logging.correlation", name = "enabled", matchIfMissing = true)
public StepLogListener stepLogListener() { return new StepLogListener(); }
```

### 5. 適用方式（opt-in、Phase 0 と整合）

ジョブ／ステップは `JobBuilder.listener(jobLogListener)` / `StepBuilder.listener(stepLogListener)` で**opt-in**。フレームワークは listener を提供するだけで、全 Job 自動適用は行わない（Phase 0 の guard/validator と同じ哲学。全 Job 強制が要れば別フェーズで検討）。

### 6. アプリ側のログパターン設定（ref app の役割）

参照アプリ `application.yml` に `logging.pattern.correlation` を設定し、MDC が標準の console/file パターンに自動挿入されることを示す:

```yaml
logging:
  pattern:
    correlation: "[%X{koiki.job.name:-} %X{koiki.job.execId:-} %X{koiki.step.name:-}] "
```

`%X{key:-}` は MDC 未設定時に空。フォーマット詳細はアプリの選択（フレームワークは指図しない）。

## 検証

- **単体テスト**: `JobLogListener` / `StepLogListener` を `beforeJob/Step` 呼び出し前後で MDC 状態を `assertThat(MDC.get(...))` で検証。`afterJob/Step` 後に `MDC.get(...)` が `null` になることを確認。例外シナリオ（after 内で例外を起こすなどは過剰、`finally` の単純性で代用）。
- **参照アプリ IT**: 既存の `CustomerDailySyncJobIT` 構造を踏襲し、Logback `ListAppender<ILoggingEvent>` を `org.koikifw` ロガーに装着してジョブを実行、`ILoggingEvent.getMDCPropertyMap()` が期待キー（`koiki.job.name`, `koiki.job.execId`, `koiki.job.bizDate`, `koiki.job.requestId`, `koiki.step.name`）を含むことを assert。
- **検証コマンド**: `.\mvnw.cmd clean verify`。

## スコープ外 / deferred

- メトリクス（Micrometer 連携・命名規約）→ 別フェーズ／[platform-capabilities](../batch/platform-capabilities.md) の Deferred。
- JSON 構造化ログ（logstash-logback-encoder 等）→ アプリ選択肢で扱い、フレームワークは強制しない。
- 全 Job への listener 自動適用（現状は opt-in）。
- マスキング・PII 抽象（Phase 3 security で扱う。今フェーズは「MDC に PII を載せない」運用ガイドのみ）。
- **並列ステップでの MDC 伝播は未対応**: SLF4J MDC はスレッドローカルのため、`TaskExecutor` 並列やパーティション分割でワーカースレッドへは自動継承されない。同期 tasklet 前提の Phase 1 ではスコープ外。並列対応が必要になった時点で MDC 伝播戦略（`TaskDecorator` 等）を別ラウンドで設計する。
- **MDC キーの所有権**: フレームワークは `CorrelationKeys` の MDC キーを**自身が所有する**前提で `afterJob`/`afterStep` で無条件に `remove` する。呼び出し元が同名キーを事前に設定していた場合、その値もジョブ終了時に消える（バッチ＝1 JVM 1 ジョブの前提では実害なし）。共有スレッドプール経由でリスナーを再利用する用途が出てきた場合は「保存→復元」戦略へ要再設計。
