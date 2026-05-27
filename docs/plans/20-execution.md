# Phase 0 / execution 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.execution` |
| ステータス | Draft |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/20-execution.md) / [architecture-batch.md](../batch/architecture-batch.md) / [rerun-procedure.md](../../ops/jp1/runbook/rerun-procedure.md) |

## 狙い

ジョブ実行制御の共通部品。標準ジョブパラメータ契約の確立、パラメータ検証、型付きアクセス、同時実行ガードの実体化。現状 [ConcurrencyGuardService.java](../../components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/execution/ConcurrencyGuardService.java) は `acquire()` が常に `true` を返すスタブ。

標準パラメータ（[architecture-batch.md](../batch/architecture-batch.md) §3）:
- `job.name` … ジョブ名
- `job.bizDate` … 営業日（再実行の安定キー。[rerun-procedure.md](../../ops/jp1/runbook/rerun-procedure.md) 参照）
- `job.requestId` … 起動ごとに一意なリクエストID

## 準拠仕様（Spring Batch 6.0.x）

- `JobParametersValidator` / `DefaultJobParametersValidator` は `org.springframework.batch.core.job.parameters` に存在。
- 実行中 execution の検出は `JobRepository.findRunningJobExecutions(String)`（`JobRepository` は `JobExplorer` を継承）。**`JobOperator.getRunningExecutions(String)` は 6.0 で非推奨化（removal 予定）のため使わない**。非推奨の `JobExplorer` も直接は使わない。
- `JobExecution` / `JobInstance` は `org.springframework.batch.core.job`。

## 設計

### 1. 標準パラメータキー定数 `StandardJobParameters`

- `job.name` / `job.bizDate` / `job.requestId` を定数化（マジックストリング排除）。
- `bizDate` の書式は `yyyyMMdd`（[rerun-procedure.md](../../ops/jp1/runbook/rerun-procedure.md) の営業日キー方針に合わせる）。

### 2. `KoikiJobParametersValidator implements JobParametersValidator`

- import: `org.springframework.batch.core.job.parameters.JobParametersValidator`
- 検証: `job.name` 必須、`job.bizDate` 必須かつ `yyyyMMdd` としてパース可能、`job.requestId` 必須。
- 必須/任意キーの素朴な存在検証は `DefaultJobParametersValidator`（同 `job.parameters` パッケージ）を内部に合成し、書式チェックのみ独自に追加する。
- 不正時は `JobParametersInvalidException` を送出（Spring Batch 標準の検証契約に従う）。

### 3. `JobParametersAccessor`

- `JobParameters` から型付き取得: `bizDate()` → `LocalDate`、`requestId()` → `String`、`jobName()` → `String`。
- 書式不正は明確な例外で通知（fault の例外階層と整合）。

### 4. `ConcurrencyGuardService` の実体化

- インターフェース（`ConcurrencyGuardService#canRun(JobExecution)`）+ デフォルト実装（`JobRepositoryConcurrencyGuardService`）。`canRun` は「**自分以外**の同名ジョブ実行が走っていないか」を返す（`beforeJob` 時点では自実行が既に running に数えられるため自分を除外する）。
- v0.1.0 デフォルト実装: `JobRepository.findRunningJobExecutions(String)` で対象ジョブ名の実行中 execution を問い合わせ、自実行 ID を除いて他に running があれば不可とする（`JobOperator.getRunningExecutions` は 6.0 で非推奨のため不使用）。
- **実効化**: `ConcurrencyGuardJobListener`（`JobExecutionListener`）が `beforeJob` で `canRun` を呼び、不可なら `SystemException` で失敗させる（→ 終了コード 30）。ジョブはこのリスナーを opt-in（`JobBuilder.listener(...)`）で適用する（バリデータの適用方法と同様）。
- deferred: 分散環境での DB ロック・排他制御は本 Phase では実装しない（単一プロセス前提の検出のみ）。

## 検証

- `KoikiJobParametersValidator` の単体テスト（正常 / 各必須欠落 / `bizDate` 書式不正）。
- `JobParametersAccessor` の単体テスト（型変換・書式不正）。
- `ConcurrencyGuardService` は `JobRepository` をモックして実行中検出の分岐をテスト。
- `mvn -pl components/libkoiki-batch test`。

## スコープ外 / deferred

- 分散ロック / DB 排他による多重起動防止。
- restart と rerun のジョブ別ルール（[rerun-procedure.md](../../ops/jp1/runbook/rerun-procedure.md) の deferred 項目）。
