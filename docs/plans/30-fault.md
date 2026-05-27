# Phase 0 / fault 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.fault` |
| ステータス | Draft |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/30-fault.md) / [return-code-mapping.md](../../ops/jp1/jobs/return-code-mapping.md) / [architecture-batch.md](../batch/architecture-batch.md) |

## 狙い

例外をそのまま運用仕様にせず、技術障害・業務不整合・警告を分類し、安定した終了コード `0/10/20/30` に変換する。JP1 等のスケジューラは例外名でなく終了コードと Runbook を参照する（[return-code-mapping.md](../../ops/jp1/jobs/return-code-mapping.md)）。`fault` パッケージが終了コードマッピング実装を所有する（同文書の Notes）。

終了コード規約（[return-code-mapping.md](../../ops/jp1/jobs/return-code-mapping.md)）:

| Code | 意味 |
| --- | --- |
| 0 | 正常終了 |
| 10 | 警告終了 |
| 20 | 業務エラー |
| 30 | システムエラー |

## 準拠仕様（Spring Batch 6.0.x / Spring Boot 4）

- `ExitStatus` / `JobExecution` は `org.springframework.batch.core.*`（`JobExecution` は `...core.job`）。
- スケジューラ起動の終了コード写像は `org.springframework.batch.core.launch.support.ExitCodeMapper`。起動は `CommandLineJobOperator`（`CommandLineJobRunner` の後継）を用い、**非推奨の `CommandLineJobRunner` は使わない**。
- Spring Boot 起動時のプロセス終了コードは `org.springframework.boot.ExitCodeGenerator` 実装 bean で制御（Boot 標準の `JobExecutionExitCodeGenerator` と同系統の手法）。

## 設計

### 1. 例外階層

- `KoikiBatchException`（基底, unchecked）
- `BusinessException extends KoikiBatchException` … 入力不備・業務ルール違反・回復可能な業務条件（→ コード 20）
- `SystemException extends KoikiBatchException` … インフラ/想定外/回復不能な技術障害（→ コード 30）
- 警告は専用例外を作らず、`FaultCategory.WARNING` を返す分類経路で表現（過剰な型を作らない）。

### 2. `FaultCategory` enum

- `NORMAL`(0) / `WARNING`(10) / `BUSINESS_ERROR`(20) / `SYSTEM_ERROR`(30)。各値が対応する終了コードを保持。

### 3. `FaultClassifier` + `DefaultFaultClassifier`

- インターフェース: `FaultCategory classify(Throwable t)`。
- デフォルト: `BusinessException`→`BUSINESS_ERROR`、`SystemException`→`SYSTEM_ERROR`、その他/未分類→`SYSTEM_ERROR`（安全側）。
- アプリ側で `@ConditionalOnMissingBean` により差し替え可能（core でワイヤリング）。

### 4. 終了コード変換（2 経路を整理）

`FaultCategory` → `0/10/20/30` を一元的に持つマッピングを定義し、以下 2 経路で利用する。

- **主経路（Spring Boot 起動 / 参照アプリ既定。実装済み）**:
  - `org.springframework.boot.ExitCodeGenerator` を実装した bean（`KoikiBatchExitCodeGenerator`）が `JobExecutionEvent`（`org.springframework.boot.batch.autoconfigure`）を購読する。
  - `JobExecution.getStatus()` と `getAllFailureExceptions()` を `FaultClassifier` で分類し、`FaultCategory` 由来の `0/10/20/30` を返す。失敗例外を直接読むため、別途 `ExitStatus` を書き換えるリスナーは設けない。
- **スケジューラ経路（JP1）**:
  - `CommandLineJobOperator` ＋ `ExitCodeMapper` 実装で `ExitStatus`（exit code 文字列）→ プロセス終了コード `0/10/20/30` を写像。
  - `ops/jp1/scripts/run-job.sh` の起動方式に合わせる。

- v0.1.0 の既定: 参照アプリが Spring Boot 起動のため**主経路を既定**とする。スケジューラ経路は JP1 ランチャ実装ラウンドで具体化する。
- いずれの経路でも `return-code-mapping.md` の `0/10/20/30` と一致させる。

## 検証

- `DefaultFaultClassifier` の単体テスト（業務/システム/未分類）。
- `FaultCategory` ↔ 終了コードのマッピングが `return-code-mapping.md` の値と一致することをテストで固定。
- 主経路の `KoikiBatchExitCodeGenerator#resolve(BatchStatus, List<Throwable>)` に各組み合わせを与え、期待カテゴリ/コードを返すかテスト。
- `mvn -pl components/libkoiki-batch test`。

## スコープ外 / deferred

- `0/10/20/30` を超えるサブコード拡張（[platform-capabilities.md](../batch/platform-capabilities.md) の Deferred）。
- retry/skip ポリシーの具体実装（実ジョブ要求に応じて後続で）。
- JP1 ランチャ（`run-job.sh`）の終了コード受け渡し実装（スケジューラ経路の確定）。
