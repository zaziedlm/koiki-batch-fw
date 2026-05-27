# Phase 0 / execution タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.execution` |
| 計画 | [20-execution.md](../plans/20-execution.md) |
| 検証コマンド | `mvn -pl components/libkoiki-batch test` |
| ステータス | Done（Spring Batch 6.0.3 で実装・全テスト通過） |

> 実装時の確定事項: 多重起動検出は `JobRepository.findRunningJobExecutions(String):Set<JobExecution>` を使用（`JobOperator.getRunningExecutions(String)` は 6.0 で非推奨/removal 予定のため不使用）。デフォルト実装クラス名は `JobRepositoryConcurrencyGuardService`。`JobParametersValidator.validate` は SB6 で `InvalidJobParametersException`（旧 `JobParametersInvalidException` から改名）を送出。
>
> E2E 点検での精緻化（2026-05-27）: ガードは `acquire(String)` から **`canRun(JobExecution)`** に変更（`beforeJob` 時点で自実行が running に数えられるため自分を除外）。実効化のため `ConcurrencyGuardJobListener`（`JobExecutionListener`）を追加し、不可時は `SystemException`→終了コード 30。ジョブは `JobBuilder.listener(...)` で opt-in。参照ジョブ `customer-daily-sync` に結線済み。

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。非推奨 API（`JobExplorer` / `JobLauncher`）は使わない。

---

## EXEC-01: 標準パラメータキー定数 StandardJobParameters

- **概要**: `job.name` / `job.bizDate` / `job.requestId` を定数化。
- **対象ファイル（新規）**: `.../batch/execution/StandardJobParameters.java`
- **準拠仕様**: 単純な定数ホルダ（インスタンス化不可）。`bizDate` 書式は `yyyyMMdd`。
- **受け入れ条件**: 3 キーが定数として参照でき、マジックストリングを排除できる。
- **依存**: なし。

## EXEC-02: KoikiJobParametersValidator

- **概要**: 標準パラメータの必須・書式検証。
- **対象ファイル（新規）**: `.../batch/execution/KoikiJobParametersValidator.java`
- **準拠仕様**: `implements org.springframework.batch.core.job.parameters.JobParametersValidator`。必須/任意の素朴検証は同パッケージの `DefaultJobParametersValidator` を合成。不正時は `JobParametersInvalidException`。
- **受け入れ条件**: `job.name`/`requestId` 欠落・`bizDate` 欠落/書式不正で例外、正常時は通過。
- **依存**: EXEC-01。

## EXEC-03: JobParametersAccessor

- **概要**: `JobParameters` からの型付き取得ヘルパー。
- **対象ファイル（新規）**: `.../batch/execution/JobParametersAccessor.java`
- **準拠仕様**: `bizDate()`→`LocalDate`、`requestId()`→`String`、`jobName()`→`String`。書式不正は明確な例外（fault 階層と整合）。
- **受け入れ条件**: 正常な型変換と、不正値での明確な失敗。
- **依存**: EXEC-01。

## EXEC-04: ConcurrencyGuardService の実体化

- **概要**: 常に `true` を返すスタブを、実行状態に基づく多重起動検出へ。
- **対象ファイル（変更）**: `.../batch/execution/ConcurrencyGuardService.java`（インターフェース化 + デフォルト実装）
- **準拠仕様**: `JobOperator`（`JobLauncher` 継承）/`JobRepository`（`JobExplorer` 継承）で対象ジョブの実行中 execution を問い合わせる。**`JobExplorer` を直接使わない**。実行中検出の正確なメソッドは 6.0.x javadoc で確定。
- **受け入れ条件**: 実行中ありで取得不可、なしで取得可。単一プロセス前提（分散ロックは対象外）。
- **依存**: なし（core への bean 登録は CORE-03 と連携）。

## EXEC-05: 単体テスト

- **概要**: validator / accessor / guard の単体テスト。
- **対象ファイル（新規）**: 各クラス対応のテスト。
- **準拠仕様**: JUnit 5。`ConcurrencyGuardService` は `JobOperator`/`JobRepository` をモック。
- **受け入れ条件**: 正常系・異常系を網羅し `mvn -pl components/libkoiki-batch test` が通る。
- **依存**: EXEC-02, EXEC-03, EXEC-04。

---

## メモ

- 取得不可時の終了コード扱いは fault（[30-fault.md](../plans/30-fault.md)）と連携。
- restart/rerun のジョブ別ルールは deferred（[rerun-procedure.md](../../ops/jp1/runbook/rerun-procedure.md)）。
