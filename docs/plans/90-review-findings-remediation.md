# レビュー指摘対応プラン

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| 対象範囲 | ロードマップ/文書整合、顧客アプリ境界、I/O堅牢化、監査ログ堅牢化、検証方針 |
| ブランチ | `topic/review-findings-plan` |
| ステータス | Done |
| タスク一覧 | [90-review-findings-remediation.md](../tasks/90-review-findings-remediation.md) |

## 目的

本プランは、プロジェクト点検で見つかった指摘事項を、順序立てて対応するための計画である。

`libkoiki-batch` の実装は、当初のスタブ中心のロードマップから進み、`core`、`execution`、`fault`、`observability`、`audit`、`security`、`validation`、`io` に実装とテストが入っている。以降の作業では、新機能を広げる前に、現状の整合性、企業向け運用での堅牢性、下流アプリとの境界を整理する。

目的はフレームワークを重くすることではない。現在の `v0.1.0` ベースラインを、人間と AI コーディングエージェントの双方が信頼して読める状態にし、下流バッチアプリが安全に採用できる形へ整えることである。

## 対応テーマ

| テーマ | 優先度 | 概要 |
| --- | --- | --- |
| R1 | High | 実装済みコードをスタブとして説明しているロードマップ/ステータス文書を現状に合わせる。 |
| R2 | Medium | `customer_a_batch_app` を `org.koikifw.*` パッケージ方針に合わせ、下流アプリの依存境界を明確にする。 |
| R3 | Medium | atomic output の最終ファイル昇格失敗が、成果物なしの正常終了にならないよう堅牢化する。 |
| R4 | Medium | audit log の構造が quote、改行、制御文字で壊れないよう formatter を堅牢化する。 |
| R5 | Low | `mvn clean test` と `mvn verify` の使い分けを明文化する。 |

## 現在の指摘事項

### R1: ロードマップが陳腐化している

対応着手前の `docs/plans/00-libkoiki-batch-roadmap.md` には、「実装は全てスタブ」という説明が残っていた。一方で実際には、`core`、`execution`、`fault`、`observability`、`audit`、`security`、`validation`、`io` に実装とテストが存在する。

ロードマップは、人間だけでなく AI コーディングエージェントにとっても作業判断の入口になる。ここが古いままだと、完了済み機能を再実装したり、誤った前提で設計判断したりするリスクがある。

### R2: 顧客アプリの境界が不明確

`apps/customer_a_batch_app` は、現時点で `com.customer.a.*` パッケージを使っている。リポジトリ全体では `org.koikifw.*` を正式パッケージ標準としているため、方針とのズレがある。

対応着手前は、`customer_a_batch_app` が `koiki-ref-batch-app` に依存していた。これは顧客アプリが参照アプリの実装を継承する構造になるため、境界としては強い結合であった。

`v0.1.0` 時点で望ましい依存方向は以下とする。

- 顧客アプリ -> `libkoiki-batch`
- 参照アプリ -> `libkoiki-batch`
- 顧客アプリは、明示的な一時サンプル目的を除き、参照アプリへ依存しない

### R3: atomic output の昇格失敗が静かすぎる

`AtomicFileOutput.promote(...)` は、最終出力ファイルへの移動失敗を warn ログにして握り潰している。アーカイブやクリーンアップの失敗なら best-effort として許容できるが、最終成果物の生成失敗は別である。

JP1 や企業向けスケジューラから見ると、「ジョブは正常終了したが成果物がない」という状態は運用上危険である。

望ましい振る舞いは以下である。

- 最終出力ファイルへの昇格失敗は、ジョブ失敗または終了コード `30` へ分類される framework 例外に変換する
- temp ファイルの破棄や後処理の失敗は、必要に応じて best-effort のまま維持する
- 成功、fallback、失敗時の挙動をテストで確認する

### R4: audit log formatter が呼び出し側の文字列に依存しすぎている

`LoggingAuditEventPublisher` は audit event を `key=value` 形式で出力するが、`message` や attribute value は quote されるだけで escape されていない。現状の javadoc では quote や改行を呼び出し側が入れない前提にしている。

監査ログは後続の機械処理や検索対象になりやすいため、publisher 側で防御的に整形するほうがよい。

望ましい振る舞いは以下である。

- quote、backslash、CR/LF、tab、その他の制御文字を escape または正規化する
- 構造化された key は読み取り可能なまま維持する
- sensitive-key masking の挙動は維持する
- `publish(...)` は引き続き例外を外へ投げない

### R5: 検証コマンドの階層が明文化されていない

現在のプロジェクトには unit test と integration test がある。root `pom.xml` では、`*IT` テストを Failsafe により `verify` フェーズで実行する。

ドキュメント上は、以下を明確に区別する。

- `mvn clean test`: 日常的な短時間の回帰確認
- `mvn verify`: 参照アプリ統合、起動経路、終了コード、DB-backed job、I/O lifecycle まで含めた完了確認

## 対応順序

1. **文書の正確性を先に直す**: ロードマップと検証方針を現状に合わせ、以降の作業者や AI エージェントが正しい前提を読めるようにする。
2. **境界を整理する**: `customer_a_batch_app` のパッケージと依存関係を整理し、顧客別アプリの責務を明確にする。
3. **実行時の堅牢性を上げる**: atomic output と audit formatter を、企業運用で問題になりやすい失敗パターンに対して強化する。
4. **最終検証する**: `mvn verify` を実行し、残る deferred 項目は `decision-log.md` または該当 plan/task に明記する。

## スコープ外

- 今回の指摘事項を超える新しい platform capability は追加しない。
- audit、I/O、scheduling に重い plugin architecture は導入しない。
- JP1 固有の実行時振る舞いを `libkoiki-batch` に入れない。JP1 関連資材は `ops/jp1` 配下に置く。
- Spring Batch インフラの所有権は変えない。framework は `JobRepository`、`JobOperator`、transaction-manager bean を生成しない。

## 完了条件

本プランは、以下を満たした時点で完了とする。

- ロードマップ/ステータス文書が `v0.1.0` の実装状態と一致している
- `customer_a_batch_app` のパッケージ/依存境界が修正済み、または一時的な扱いとして明記されている
- atomic output が、最終ファイル公開失敗を正常終了として扱わない
- audit formatter が、構造を壊す文字列値に対して堅牢である
- `mvn verify` が成功する
- 残る deferred decision が意図的に記録されている
