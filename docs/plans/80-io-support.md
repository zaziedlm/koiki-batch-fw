# Phase 5 / io・support 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.io` / `org.koikifw.libkoiki.batch.support` |
| ステータス | Done（`v0.1.0` 初期スコープを実装済み。追加運用モデルは deferred） |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/80-io-support.md) / [DB管理アーキテクチャ](../batch/db-management-architecture.md) / [decision-log](../batch/decision-log.md) |

## 狙い

**標準 Spring Batch の reader/writer はそのまま使う**前提で、その上にバッチ基盤が**汎用的に**足せる IO 付加価値を整備する。個社固有のファイルレイアウト（固定長の多レコード種別・ヘッダ/トレーラ照合等）は要求が見えていないため**今回は扱わない**。現在は charset helper、取込ファイル lifecycle、archive/error policy、atomic output の初期実装と参照 file job の integration test を持つ。

整備する付加価値（業務非依存・再利用可能、合意済み）:

- **A. 文字コード方針** — 取込/出力ファイルの charset を設定で柔軟切替（既定 MS932）。
- **B. 取込ファイルのライフサイクル** — 入力の存在/空チェック（fail-fast）＋ 成功=アーカイブ / 失敗=エラー隔離。
- **C. アトミック出力** — 一時ファイルへ書き出し→ジョブ成功時にリネーム。下流が部分ファイルを拾わない。

ロードマップ Phase 5 の方針「実ジョブ要求に駆動される／重い抽象化を先行させない」に従い、各機能は**最小の参照 file→file ジョブ**で実証する。

## 準拠仕様（SB6.0.x / Spring Boot 4・裏取り済み）

SB6 のフラットファイル系（`spring-batch-infrastructure-6.0.3`、jar 実体で確認）:
- `org.springframework.batch.infrastructure.item.file.FlatFileItemReader` / `builder.FlatFileItemReaderBuilder`
- `...item.file.FlatFileItemWriter` / `builder.FlatFileItemWriterBuilder`（実装時に最終確認）
- `...item.file.transform.DelimitedLineTokenizer` / `DelimitedLineAggregator` / `LineAggregator`
- `...item.file.mapping.DefaultLineMapper` / `FieldSetMapper`
- reader/writer は `encoding(Charset)` 指定可、writer は `FileSystemResource` への書込。

## 設計

### 0. 合成性・非干渉の原則（将来の file→DB / DB→file への配慮）

A/B/C は**互いに独立した opt-in 部品**として設計し、入出力の片側が DB でも**干渉しない**ようにする。これにより将来 file→DB / DB→file を「部品の組み合わせ」だけで実装でき、framework の追加実装は不要（サポート範囲は限定的でよい）。

| 機能 | 駆動キー | file→file | file→DB | DB→file |
| --- | --- | --- | --- | --- |
| A 文字コード | リソース単位で適用 | 入力+出力 | 入力(reader)のみ | 出力(writer)のみ |
| B 取込ライフサイクル | `koiki.io.inputFile` の有無 | 適用 | 適用 | 未指定→**no-op** |
| C アトミック出力 | `koiki.io.outputFile` の有無 | 適用 | 未指定→**no-op** | 適用 |

- B は出力先（file/DB）を問わず、C は入力元（file/DB）を問わない（**互いに独立**）。
- 3つとも opt-in（`JobBuilder.listener(...)`）＋**該当パラメータが無ければ no-op**。アタッチしても他に干渉しない。
- 将来 file→DB は A(reader)+B、DB→file は A(writer)+C を組むだけ。今回は参照を file→file に限定し、これらは実装しない（部品の合成性のみ担保）。

### A. 文字コード方針

- `KoikiBatchProperties.Io.File.charset`（既定 `"MS932"`）。
- 参照ジョブの reader/writer に `Charset.forName(charset)` を適用。MS932/UTF-8 双方をテストで実証。
- 解決の明確化のため小ヘルパー（`io` 内に `BatchCharsets.resolve(String)` 程度）を任意で提供（不正名を分かりやすく失敗させる）。

### B. 取込ファイルのライフサイクル

1. プロパティ: `koiki.batch.io.file.{archive-dir, error-dir}`（空なら no-op）。
2. `FileArchivePolicy`（契約）＋ `DirectoryFileArchivePolicy`（参照実装）: 入力ファイルを成功時 archive / 失敗時 error ディレクトリへタイムスタンプ付きで移動。dir 空で no-op、移動失敗は warn のみ（後処理で業務を止めない）。
3. `FileIngestionLifecycleListener`（opt-in `JobExecutionListener`）:
   - `beforeJob`: 入力ファイル（ジョブパラメータ `koiki.io.inputFile`）の**存在・非空**を検査。欠落/空なら `BusinessException`（→終了コード20）で fail-fast。
   - `afterJob`: `COMPLETED`→`archive`、それ以外→`moveToError`。
   - 入力パラメータ未指定なら no-op。Phase 0 の listener と同じ opt-in（`JobBuilder.listener(...)`）。

### C. アトミック出力

1. `AtomicFileOutput`（ユーティリティ）: 最終パスから一時パスを規約生成（既定 `finalPath + ".inprogress"`、サフィックスは `koiki.batch.io.file.temp-suffix` で変更可）、`promote(temp, final)`（`Files.move` ATOMIC/REPLACE）、`discard(temp)`。
2. `AtomicOutputListener`（opt-in `JobExecutionListener`）:
   - `afterJob`: 最終パス（ジョブパラメータ `koiki.io.outputFile`）に対し、`COMPLETED`→一時→最終へ昇格、それ以外→一時を破棄。
   - 参照ジョブの writer は `AtomicFileOutput.tempPath(final)` の一時ファイルへ書く（listener が同じ規約で昇格）。

### D. core 自動構成への登録

- `FileArchivePolicy`（`DirectoryFileArchivePolicy`）、`FileIngestionLifecycleListener`、`AtomicOutputListener` を `@Bean @ConditionalOnMissingBean`（プロパティ注入、opt-in でジョブが attach）。
- 既存 Phase の条件付き登録パターンと整合。

### E. 参照アプリ — file→file ジョブ（A/B/C を実証）

- `jobs/billing/BillingFileJobConfig`（job 名 `billing-file`）。
- 入力: 区切り（`customerId,amount`）テキスト。`FlatFileItemReader`（`DelimitedLineTokenizer`）+ 設定 charset。
- processor: Phase4 の `Validator`（例: amount >= 0）。
- writer: `FlatFileItemWriter`（`DelimitedLineAggregator`）を **`AtomicFileOutput` の一時パス**へ、設定 charset で出力。
- listener: `FileIngestionLifecycleListener`（B）＋ `AtomicOutputListener`（C）を attach。
- chunk: commit-interval は `koiki.batch.transaction.defaultCommitInterval`。
- JobRepository は resourceless でよい（メタ永続は本ジョブの主題でない）。

### F. support パッケージ

- 実需が出た中立ユーティリティのみ。`AtomicFileOutput` は io 固有のため `io` に置く。現時点で support への追加予定はなく、スタブ維持（package-info のみ）。

## 検証

- **単体（libkoiki-batch）**:
  - `DirectoryFileArchivePolicy`（@TempDir）: 成功/失敗で適切な dir へ移動、空 dir で no-op、移動失敗で非throw。
  - `FileIngestionLifecycleListener`: 入力欠落/空で `BusinessException`、`COMPLETED` で archive、失敗で moveToError（モック policy + 合成 JobExecution、@TempDir）。
  - `AtomicFileOutput` / `AtomicOutputListener`: 一時パス規約、`COMPLETED` で昇格、失敗で破棄（@TempDir）。
  - `KoikiBatchProperties.Io.File` の既定（charset=MS932）とバインド、`BatchCoreAutoConfigurationTest` 拡張。
- **参照アプリ IT（Failsafe）**:
  - `billing-file` 正常: 入力読込→検証→出力が**最終パス**に生成（一時が消えている）、入力が archiveDir へ移動。
  - 文字コード切替（MS932/UTF-8）で読み書きできること。
  - 入力欠落/空 → business error(20)。検証失敗 → business error(20) かつ入力が errorDir へ移動・最終出力が生成されない。
- **検証コマンド**: `.\mvnw.cmd clean verify`。

## スコープ外 / deferred

- 個社固有のファイルレイアウト（固定長の多レコード種別・ヘッダ/トレーラ件数照合）。
- リジェクトファイル（skip レコード隔離・候補D）、パス解決規約（候補E）。有用だが要求が出てから。
- アーカイブ/エラーディレクトリの標準命名・世代管理モデル（mechanism と契約のみ提供）。
- 外部API/メッセージング I/O アダプタ契約、広い Reader/Writer 抽象層（標準 SB を使う方針）。

## 将来の考慮事項（最終点検で抽出・今回は実装しない）

実コードに即した最終点検で洗い出した、現時点では出していないが将来（特に file→DB / DB→file・リスタート対応・本番ファイル交換）で効いてくる検討点。重要度順。

1. **リスタートとファイル後処理の非互換（最重要）**。本フェーズの後処理（失敗時に入力を error 退避・出力 temp を破棄）は **resourceless / rerun-from-scratch 前提**で正しい。将来このファイルジョブを **JDBC リポジトリでリスタート可能**にすると、初回失敗で入力が error へ移動・temp 破棄され、リスタートが入力を見つけられない／writer の保存状態と不整合になる。**ファイルジョブはリラン型を前提**とし、リスタート型にする場合は「最終確定時のみ移動」など後処理方針を別設計する（[decision-log](../batch/decision-log.md) 参照）。
2. **入力ファイルの“配信完了”検知**。`FileIngestionLifecycleListener` の `beforeJob` は存在＋非空のみ検査。上流が書込中の不完全ファイルを拾う恐れ。定石は**センチネル(.done)ファイル**または**サイズ安定確認**。将来オプションとして検討。
3. **出力の改行コード・BOM 非決定性**。`FlatFileItemWriter` の既定改行は `System.lineSeparator()`（OS依存：Windows=CRLF / Linux=LF）。下流連携ファイルは改行を仕様で固定すべき。UTF-8 入力の **BOM** も既定では除去されない。`charset` 同様に「改行コード方針」をプロパティ化する余地。
4. **パースエラーの分類（20 vs 30）**。数値不正等の `FlatFileParseException` は Koiki 例外でないため `DefaultFaultClassifier` で **SYSTEM_ERROR(30)**。入力フォーマット不正を business(20) に寄せたい場合は skip 方針 or 分類拡張を将来検討。
5. **`STOPPED` 等の非COMPLETED時の挙動**。現状 COMPLETED 以外は一律 error 退避/temp 破棄。運用停止(STOPPED) まで error 扱いでよいか将来要検討。
6. （軽微）空結果時に空の最終ファイルを出すか（`shouldDeleteIfEmpty`）、file-only ジョブの TM は概念上 resourceless が自然、等。

## 運用ノート

- 文字コードは `koiki.batch.io.file.charset` で切替（レガシー=MS932、新規=UTF-8 等）。
- アーカイブ/エラー先は `archive-dir`/`error-dir`。未設定なら後処理 no-op。本番のディレクトリ運用・世代管理はアプリ/運用設計（deferred）。
- アトミック出力により、ジョブ成功まで最終パスにファイルが現れない。JP1/下流の取り込みは最終パス出現をトリガにできる。temp は最終と同一ディレクトリ（同一ボリューム）に置き、ATOMIC_MOVE を成立させる。
- 後処理（移動/昇格）失敗は warn ログ。確実性が要件ならアプリで方針強化。
- **ファイルジョブはリラン（頭から再投入）型を前提**とする。失敗時に入力を error 退避・出力 temp を破棄するため、リスタート型とは両立しない（上記「将来の考慮事項」1 / [decision-log](../batch/decision-log.md)）。
