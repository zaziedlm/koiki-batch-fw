# 参照ジョブ実装パターン

この文書は、`components/koiki_ref_batch_app` に参照ジョブを追加するときに、既存実装から最も近いパターンを選ぶための資料である。

## パターン選択

| パターン | 既存ジョブ | 選択条件 |
| --- | --- | --- |
| Tasklet | `customer-daily-sync` | 単発処理、制御処理、件数の少ない処理、framework 機能の最小実証 |
| DB chunk | `customer-import` | DB から複数件を読み、項目単位で検証・変換し、chunk transaction で DB 更新する |
| File chunk | `billing-file` | ファイルを複数件読み、検証・変換し、ファイルへ出力する |

新しい抽象化を先に作らず、最も近い既存ジョブを起点にする。複数パターンを組み合わせる必要がある場合も、Job/Step と入出力の責務を明示する。

## パターン選択時の設計観点

次の観点は、毎回すべてを回答させる必須質問票ではない。パターン、トランザクション、復旧方式、opt-in 機能の選択を変える不明点がある場合に確認する。

- 目的と完了条件: 何を完了とし、業務上どの状態を残すか。
- 入出力契約: 入力元、出力先、データ量、順序、ファイルやデータの確定タイミング。
- 整合性: commit 単位、部分成功の可否、重複・欠損を防ぐ方法。
- 復旧: rerun、restart、冪等性、失敗データの扱い。
- 障害方針: retry、skip、rollback、終了コードへ影響する失敗分類。
- 実行制御: 並行実行、排他、スケジュール、処理時間やタイムアウトの制約。
- 統制: 機密情報のマスキング、監査対象、実行者や権限の要件。

名称、サンプル値、局所的な形式など、後から容易に変更できる不明点は、暫定前提を明示した小さな試行で確認できる。データ整合性、復旧、安全性、運用契約を変える不明点は、実装前に設計確認する。

## 入出力の組み合わせ

現在の3パターン名は既存参照ジョブを基準にしたものであり、入出力の全組み合わせを固定する分類ではない。Reader の種類と復旧方式を主な基準にし、Writer とトランザクション方式を組み合わせる。

| 入力 | 出力 | 現在の扱い |
| --- | --- | --- |
| DB | DB | `customer-import` を基準とする既存 DB chunk |
| File | File | `billing-file` を基準とする既存 File chunk |
| File | DB | File chunk の Reader・lifecycle と、DB Writer・chunk transaction を組み合わせる候補 |
| DB | File | DB chunk の Reader と、File Writer・atomic output を組み合わせる候補 |

`File → DB` と `DB → File` は、既存要素の組み合わせとして設計できる可能性が高い。ただし、現時点では専用の参照ジョブで検証されていないため、実装済みパターンとして扱わない。

特に `File → DB` では、再実行時の重複防止、chunk rollback 後の入力位置、skip 方針、入力ファイルの archive/error 移動時点、JobRepository の方式を先に決める。具体的な標準構成は、独立した計画とタスクで参照ジョブを使って検証する。

## 既存パターンに適合しない場合

tasklet、DB chunk、file chunk のいずれかへ無理に分類せず、次の順序で扱う。

1. 複数の既存 Step を組み合わせる Job flow として表現できるか確認する。
2. 既存パターンの一部だけが未検証なら、その部分に限定した暫定試行を検討する。
3. framework 共通機能または新しいジョブパターンが必要なら、参照ジョブの完成実装を開始せず、設計確認へ切り替える。

次に該当するだけでは、新しいパターンとはみなさない。

- 一つの Job に tasklet Step と chunk Step が含まれる。
- File から DB、DB から File など、既存 I/O の組み合わせである。
- 顧客固有の SQL、file layout、validation、外部接続設定が異なる。

次のように実行モデルや共通責務が変わる場合は、新しいパターン候補として扱う。

- partition、並列 Step、非同期 chunk など、並行実行モデルを追加する。
- 外部 API やメッセージングを主要な I/O とし、再試行や重複排除の共通方針が必要になる。
- remote chunking など、複数プロセス間の実行制御を必要とする。
- 既存の rerun/restart 方針では整合性を保証できない。

新しいパターン候補では、最初に目的、未決事項、検証したい仮説、対象外、完了条件を示す。検証結果から再利用性が確認されるまでは、`libkoiki-batch` に共通抽象化を追加しない。

## 全パターン共通

- `org.koikifw.refapp.batch.*` 配下に配置する。
- Job 名は `public static final String JOB_NAME` として一か所に定義する。
- `JobBuilder` に `KoikiJobParametersValidator` を登録する。
- 標準パラメータとして `job.name`、`job.bizDate`、`job.requestId` を受け取る。
- `JobLogListener` を Job、`StepLogListener` を Step に登録する。
- Step 構築時に `PlatformTransactionManager` を明示する。
- Job configuration、Step、I/O、検証、業務ルールの責務を分離する。
- 実顧客情報を使わず、架空で決定的なモデル・データを使う。
- `spring.batch.job.enabled=false` の統合テストから対象 `Job` を明示的に起動する。
- テストでは起動ごとに一意な `job.requestId` を使う。
- 参照ジョブ追加後はルート `README.md` の参照ジョブ一覧を更新する。

## Tasklet パターン

参照先:

- `jobs/customer/CustomerDailySyncJobConfig.java`
- `jobs/customer/CustomerDailySyncJobIT.java`

使用条件:

- Reader/Processor/Writer に分割する必要がない。
- 単一の業務操作または framework 機能の結線を示す。

実装上の判断:

- パラメータを業務処理で使う場合は `JobParametersAccessor` で型付き取得する。
- Tasklet が再実行されても問題がないか、冪等性または rerun 方針を明確にする。
- 業務上意味のある完了・変更イベントがある場合だけ `AuditEventPublisher` を使う。
- 同一 Job の多重起動を拒否する要件がある場合だけ `ConcurrencyGuardJobListener` を登録する。

最低限のテスト:

- 正常パラメータで `COMPLETED` になる。
- 必須パラメータ欠落時に検証エラーになる。
- 追加した監査、MDC、マスキング、多重起動制御がある場合は、それぞれ独立した観点で確認する。

## DB chunk パターン

参照先:

- `jobs/customer/CustomerImportJobConfig.java`
- `jobs/customer/CustomerValidatingProcessor.java`
- `jobs/customer/CustomerRecordValidator.java`
- `jobs/customer/CustomerImportJobIT.java`
- `jobs/customer/CustomerImportResourcelessIT.java`

使用条件:

- 複数レコードを read/process/write する。
- chunk 単位の commit/rollback が必要である。

実装上の判断:

- Spring Batch 標準の JDBC reader/writer を優先する。
- `KoikiBatchProperties.transaction.defaultCommitInterval` を chunk size として明示的に使う。
- 業務入力検証は `Validator<T>` と processor に分離する。
- 検証失敗は `ValidationException` を経由して business error として扱う。
- SQL、table、row mapping は参照アプリ側に置き、`libkoiki-batch` へ移さない。
- JobRepository の方式を実行要件から選ぶ。
  - Resourceless: 先頭から安全に rerun でき、永続メタデータが不要
  - JDBC: restart、実行履歴、永続的な多重起動判定が必要
- JDBC JobRepository を使う場合は、アプリ側で明示的に opt-in し、Flyway で `BATCH_*` schema を管理する。

最低限のテスト:

- 正常データが destination へ commit され、read/write count が期待どおりになる。
- 不正データで Job が `FAILED` になり、対象 chunk が rollback される。
- JDBC repository を採用する場合は step metadata の永続化を確認する。
- Resourceless を採用する場合は、業務更新は commit されても metadata が永続化されないことを必要に応じて確認する。

## File chunk パターン

参照先:

- `jobs/billing/BillingFileJobConfig.java`
- `jobs/billing/BillingFileValidator.java`
- `jobs/billing/BillingFileJobIT.java`
- `jobs/billing/BillingFileUtf8IT.java`

使用条件:

- 複数レコードのファイルを read/process/write する。
- 入力ファイルの処理済み管理または出力ファイルの安全な公開が必要である。

実装上の判断:

- job parameter を Reader/Writer に注入する場合は `@StepScope` を使う。
- 文字コードは `BatchCharsets` と `koiki.batch.io.file.charset` から解決する。
- 入力ファイルの存在・空ファイル判定、成功時 archive、失敗時 error 移動が必要な場合だけ `FileIngestionLifecycleListener` を登録する。
- 出力途中のファイルを公開してはならない場合だけ `AtomicOutputListener` を登録し、writer は temp path へ書く。
- ファイルレコードの業務検証は `Validator<T>` と processor に分離する。
- 現在の file lifecycle / atomic output は rerun-from-scratch 型であり、JDBC restart と安易に組み合わせない。

最低限のテスト:

- 正常時に最終出力が作成され、temp が残らず、入力が archive される。
- 入力不存在・空入力が business error になる。
- 不正レコード時に最終出力が作られず、入力が error directory へ移動する。
- configurable charset を要件に含める場合は、デフォルト以外の文字コードも確認する。

## Opt-in 機能の判断

| 機能 | 適用条件 | 常時適用しない理由 |
| --- | --- | --- |
| `ConcurrencyGuardJobListener` | 同一 Job の多重起動拒否が必要 | 実行方式や repository mode に依存する |
| `AuditEventPublisher` | 業務変更・制御イベントを後から説明する必要がある | 通常ログと監査は目的が異なる |
| Masking | audit/log に機微値が含まれ得る | 対象キー・pattern はデータ契約に依存する |
| JDBC JobRepository | restart、履歴、cross-process 制御が必要 | rerun 型ジョブには運用負荷が増える |
| `FileIngestionLifecycleListener` | 入力ファイルの archive/error 管理が必要 | DB・tasklet ジョブには不要 |
| `AtomicOutputListener` | 最終ファイルの途中公開を防ぐ必要 | ファイル出力を持たないジョブには不要 |

## 完了時の確認

- 新しい Job が既存3パターンのどれを基準にしたか、または既存パターンに適合しない理由を説明できる。
- opt-in 機能を採用または不採用にした理由を説明できる。
- Job/Step/I/O/validation の責務が一つの汎用 utility に混在していない。
- 参照ジョブの統合動作を確認する `*IT` がある。
- `README.md` の参照ジョブ一覧が更新されている。
- 参照アプリに触れる変更として `mvn verify` が成功する。
