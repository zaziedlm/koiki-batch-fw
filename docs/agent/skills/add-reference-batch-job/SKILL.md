---
name: add-reference-batch-job
description: KOIKI Batch Framework の `components/koiki_ref_batch_app` に参照実装バッチジョブを追加または拡張する。要件確認ゲートを通し、tasklet、DB chunk、file chunk の既存パターンの採用・組み合わせ・非適合を判断して、標準パラメータ、ログ、監査、検証、トランザクション、I/O lifecycle、テスト、README 更新までを既存境界に沿って実施するときに使用する。顧客固有ジョブ、共通基盤の新機能設計、JP1 launcher 実装には使用しない。
---

# 参照実装バッチジョブの追加

## 事前確認

1. [AGENTS.md](../../../../AGENTS.md) を読み、リポジトリ共通ルールを確認する。
2. [boundaries.md](../../boundaries.md) を読み、参照アプリと共通基盤・顧客アプリの境界を確認する。
3. [testing.md](../../testing.md) を読み、完了時の検証範囲を確認する。
4. [reference-job-patterns.md](references/reference-job-patterns.md) を読み、既存3パターンの採用・組み合わせ、または適合しない可能性を判断する。

## 要件確認ゲート

実装前に、依頼、関連文書、既存コードから確認できる事実と、実装判断へ影響する不明点を短く整理する。情報の不足数ではなく、影響度と変更の可逆性から次の進行モードを選ぶ。

- `実装開始`: 目的、主要な入出力、正常・失敗条件、rerun/restart 方針を判断できる。
- `暫定試行`: 不明点が低リスクで、局所的に変更を戻せる。暫定前提と試行の対象外を明示し、実装範囲を最小化する。
- `設計確認`: 不明点がデータ整合性、トランザクション、障害復旧、セキュリティ、監査、並行実行、終了コード、共通基盤の責務へ影響する。

情報が少ないことだけを理由に停止しない。反対に、情報が多くても重要な判断が未確定なら実装を開始しない。

`設計確認` では、実装判断を変える質問を原則1から3件に絞り、回答後に開始できる最小の作業を示す。リポジトリに記載のない業務要件を推測して既定仕様にしない。

## 進行モードの伝え方

依頼が明確な場合は、進行モードを形式的に宣言せず、そのまま作業を進めてよい。不明点が実装範囲や結果へ影響する場合だけ、必要な項目を簡潔に伝える。

- `実装開始`: 採用するパターンと主要な設計判断を示して作業へ進む。
- `暫定試行`: 確認できた事実、置く暫定前提、試行の対象外、前提を見直す条件を示す。
- `設計確認`: 実装を保留する理由、決定が必要な事項、回答後に開始できる最小の作業を示す。

事実、暫定前提、要決定事項が混同されるおそれがある場合は区別して記載する。依頼が小さく自明な場合まで同じ見出しや定型文を強制しない。

## 作業手順

1. ジョブの目的、入力、出力、失敗条件、rerun/restart 方針を整理する。
2. tasklet、DB chunk、file chunk の採用・組み合わせを決める。自然に適合しない場合は設計確認へ戻る。
3. `org.koikifw.refapp.batch.*` 配下へ Job、Step、I/O、model、validation を責務別に配置する。
4. Job 名を `public static final String JOB_NAME` に定義する。
5. `KoikiJobParametersValidator` を Job に登録し、`job.name`、`job.bizDate`、`job.requestId` を標準パラメータとして扱う。
6. `JobLogListener` を Job、`StepLogListener` を Step に登録する。
7. tasklet または chunk Step に `PlatformTransactionManager` を明示する。
8. 業務入力検証が必要なら `Validator<T>` と processor/tasklet の責務を分離する。
9. 監査、多重起動ガード、マスキング、JDBC JobRepository、file lifecycle、atomic output は要件がある場合だけ追加する。
10. 正常系と主要な失敗系を確認する unit test または `*IT` を追加する。
11. ルート `README.md` の「参照実装バッチジョブ」を更新する。
12. 重要な設計判断や deferred 項目が生じた場合は、該当する `docs/batch` または plan/task 文書を更新する。

## 実装判断

- 既存コードを固定テンプレートとして丸ごと複製せず、最も近いジョブから必要な構成だけを採用する。
- Spring Batch 標準 reader/writer で十分なら、新しい framework abstraction を作らない。
- 業務固有の SQL、file layout、model、validation を `libkoiki-batch` に置かない。
- transaction、retry/skip、exit code の挙動を汎用 utility に隠さない。
- 実顧客情報や実データを参照実装・テストへ持ち込まない。
- file lifecycle / atomic output を JDBC restart と組み合わせる場合は、既存の rerun-from-scratch 方針と矛盾しないか先に設計確認する。
- 顧客固有ジョブの依頼なら、この Skill を中止して `apps/*` の境界に従う。
- 共通基盤の新機能が必要なら、参照ジョブ追加と同時に安易に実装せず、`docs/batch/platform-capabilities.md` と decision log の要否を評価する。

## テスト選択

- 小さな validator、processor、変換ロジックには unit test を追加する。
- Job/Step 結線、標準パラメータ、transaction、DB/Flyway、監査、MDC、file lifecycle、終了コードは `*IT` で確認する。
- DB chunk は commit、rollback、read/write count、必要に応じて metadata 永続化を確認する。
- File chunk は final/temp file、archive/error 移動、文字コード、失敗時に不完全な出力を公開しないことを確認する。
- `job.requestId` はテスト起動ごとに一意にする。

## 完了確認

1. package と module の配置が境界文書に適合していることを確認する。
2. opt-in 機能を採用・不採用にした理由を説明できることを確認する。
3. README と関連文書が実装状態に一致していることを確認する。
4. 参照アプリに触れる変更として `mvn verify` を実行する。
5. 実行コマンド、結果、未実施テスト、残存リスクを報告する。
