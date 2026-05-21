# Architecture (Spring Batch)

このプロジェクトは、Web API 中心のレイヤード構成ではなく、Spring Batch の実行モデルに沿った構成を採用します。

## 1. 構成原則

- `components/libkoiki-batch`:
  - 再利用可能なバッチ基盤（共通設定、実行制御、監視、共通 I/O、障害制御）
- `components/koiki_ref_batch_app`:
  - 基盤の使い方を示す参照実装（標準パターンの実コード）
- `apps/*`:
  - 顧客固有ジョブ実装（業務ロジック、顧客固有連携）

## 2. Spring Batch レイヤー（標準）

本プロジェクトでは、ジョブ実装を以下の責務で分離する。

1. Job 定義層
   - Job/Step の構成、遷移、再実行ポリシー定義
2. Step 実装層
   - Chunk / Tasklet の選択、処理単位、トランザクション境界
3. I/O 層
   - Reader / Processor / Writer の組み合わせ
4. Domain Service 層
   - 業務変換・業務判定ロジック
5. Infrastructure 層
   - DB、ファイル、外部 API、ログ、メトリクス、監査

依存は原則として上位から下位へ一方向とし、下位層から Job 構成へ逆依存させない。

## 3. Job 実行モデル

- 起動:
  - JP1 などのスケジューラから `ops/jp1/scripts/run-job.sh` を経由して実行
- パラメータ:
  - `job.name`, `job.bizDate`, `job.requestId` を標準キーとする
- 終了コード:
  - 0: 正常 / 10: 警告 / 20: 業務エラー / 30: システムエラー
- 運用:
  - リラン手順と監視方針は `ops/jp1/runbook` と `ops/jp1/jobs` を正とする

## 4. 例外・再実行・冪等性

- 業務エラーとシステムエラーを明確に分離する
- 再実行方式（restart / rerun）をジョブ単位で明示する
- 冪等性を確保し、同一業務日付での再実行でも重複更新を防止する

## 5. 実装配置ガイド

- 共通部品化するものは `components/libkoiki-batch` へ昇格
- 参照アプリで成立する実装パターンを `components/koiki_ref_batch_app` へ集約
- 顧客要件は `apps/<customer>_batch_app` に閉じ込める

この分離により、基盤改修と顧客実装の変更影響を最小化する。
