# Phase 0 / fault タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.fault` |
| 計画 | [30-fault.md](../plans/30-fault.md) |
| 検証コマンド | `mvn -pl components/libkoiki-batch test` |
| ステータス | Done（主経路を実装・全テスト通過。FAULT-05 スケジューラ経路は設計のみで実装 deferred） |

> 実装時の確定事項: 主経路の終了コードは `KoikiBatchExitCodeGenerator`（`ApplicationListener<org.springframework.boot.batch.autoconfigure.JobExecutionEvent>` + `org.springframework.boot.ExitCodeGenerator`）が、`JobExecution.getStatus()` と `getAllFailureExceptions()` を `FaultClassifier` で分類して `0/10/20/30` を返す。別途 `ExitStatus` を書き換えるリスナーは設けず、失敗例外を直接読む方式に簡素化した。`JobExecution` は `org.springframework.batch.core.job`、`ExitStatus`/`BatchStatus` は `org.springframework.batch.core`。
>
> E2E 点検での追加（2026-05-27）: 起動前例外（`InvalidJobParametersException` 等、`JobExecution` 生成前 throw で `JobExecutionEvent` が出ないケース）は主経路で拾えず Boot 既定 exit 1 になる。これを補う **`KoikiExitCodeExceptionMapper`**（Spring Boot `ExitCodeExceptionMapper`）を追加。`InvalidJobParametersException`→20、それ以外は `FaultClassifier` 準拠（未分類→30）。core で exit-code 有効時に自動登録。FAULT-05（スケジューラ経路）は引き続き deferred。

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。終了コードは [return-code-mapping.md](../../ops/jp1/jobs/return-code-mapping.md) の `0/10/20/30` を正とする。非推奨 API（`CommandLineJobRunner`）は使わない。

---

## FAULT-01: 例外階層

- **概要**: framework 例外の基底と業務/システム分類。
- **対象ファイル（新規）**: `KoikiBatchException.java` / `BusinessException.java` / `SystemException.java`
- **準拠仕様**: unchecked 例外。`BusinessException`→コード20、`SystemException`→コード30 の意図を javadoc に明記。警告用の専用例外は作らない。
- **受け入れ条件**: 階層がコンパイルされ、業務/システムを型で区別できる。
- **依存**: なし。

## FAULT-02: FaultCategory enum

- **概要**: 分類と終了コードの対応を保持。
- **対象ファイル（新規）**: `FaultCategory.java`
- **準拠仕様**: `NORMAL(0)`/`WARNING(10)`/`BUSINESS_ERROR(20)`/`SYSTEM_ERROR(30)`。各値が終了コードを保持。
- **受け入れ条件**: 各カテゴリが `return-code-mapping.md` の値と一致。
- **依存**: なし。

## FAULT-03: FaultClassifier + DefaultFaultClassifier

- **概要**: `Throwable` をカテゴリに分類。
- **対象ファイル（新規）**: `FaultClassifier.java`（IF）/ `DefaultFaultClassifier.java`
- **準拠仕様**: `BusinessException`→`BUSINESS_ERROR`、`SystemException`→`SYSTEM_ERROR`、未分類→`SYSTEM_ERROR`（安全側）。core で `@ConditionalOnMissingBean` 差し替え可能。
- **受け入れ条件**: 各分岐が期待カテゴリを返す。
- **依存**: FAULT-01, FAULT-02。

## FAULT-04: 終了コードマッピング（主経路: ExitStatus 反映 + ExitCodeGenerator）

- **概要**: 分類結果を Job の `ExitStatus` に反映し、Boot のプロセス終了コードへ写像。
- **対象ファイル（新規）**: 分類結果を `ExitStatus` へ反映する `JobExecutionListener` 実装 / `org.springframework.boot.ExitCodeGenerator` 実装。
- **準拠仕様**: `JobExecution`=`org.springframework.batch.core.job`。Boot の `JobExecutionExitCodeGenerator` と同系の手法。`FaultCategory`→`0/10/20/30` の単一マッピングを共有。
- **受け入れ条件**: 分類済み `JobExecution`/`ExitStatus` を与えると期待コードを返す。
- **依存**: FAULT-02, FAULT-03。

## FAULT-05: スケジューラ経路の設計メモ（実装は deferred）

- **概要**: `CommandLineJobOperator` + `ExitCodeMapper` による JP1 経路を計画文書として確定。
- **対象ファイル**: 本タスク文書 / [30-fault.md](../plans/30-fault.md) への追記のみ（コードは JP1 ランチャ実装ラウンドで）。
- **準拠仕様**: `org.springframework.batch.core.launch.support.ExitCodeMapper`、`CommandLineJobOperator`。
- **受け入れ条件**: v0.1.0 既定は主経路（FAULT-04）と明記され、スケジューラ経路の段取りが残る。
- **依存**: FAULT-04。

## FAULT-06: 単体テスト

- **概要**: classifier とマッピングのテスト。
- **対象ファイル（新規）**: 各クラス対応のテスト。
- **準拠仕様**: JUnit 5。
- **受け入れ条件**: `FaultCategory`↔終了コードが `return-code-mapping.md` と一致することを固定し、`mvn -pl components/libkoiki-batch test` が通る。
- **依存**: FAULT-03, FAULT-04。

---

## メモ

- core への bean 登録（classifier / exit-code generator）は CORE-03 と連携。
- サブコード拡張・retry/skip 実装は deferred。
