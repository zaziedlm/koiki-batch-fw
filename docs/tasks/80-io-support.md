# Phase 5 / io・support タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.io` / `.../support` |
| 計画 | [80-io-support.md](../plans/80-io-support.md) |
| 検証コマンド | `.\mvnw.cmd clean verify`（IT 込み） |
| ステータス | Not started |

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) に従う。標準 SB reader/writer を使い、framework は **A:文字コード / B:取込ライフサイクル / C:アトミック出力** の汎用 IO 付加価値に限定。SB6 フラットファイルは `org.springframework.batch.infrastructure.item.file.*`。

---

## IOS-01: io プロパティ（charset / archive-dir / error-dir / temp-suffix）

- **対象（変更）**: `core/KoikiBatchProperties.java`（nested `Io { File file }`、`File { String charset="MS932"; String archiveDir=""; String errorDir=""; String tempSuffix=".inprogress"; }` + getter/setter）。
- **準拠**: `koiki.batch.io.file.*`。
- **受け入れ**: 既定 `charset=MS932`・`temp-suffix=.inprogress`、各値がバインド。
- **依存**: なし。

## IOS-02: 文字コードヘルパー（A）

- **対象（新規・任意）**: `io/BatchCharsets.java`（`static Charset resolve(String name)`、不正名は分かりやすい例外）。
- **受け入れ**: 正常名で `Charset`、不正名で明確な例外。
- **依存**: なし。

## IOS-03: FileArchivePolicy + DirectoryFileArchivePolicy（B）

- **対象（新規）**: `io/FileArchivePolicy.java`（`void archive(Path); void moveToError(Path);`）、`io/DirectoryFileArchivePolicy.java`。
- **準拠**: dir 空で no-op、タイムスタンプ付き `Files.move`、移動失敗は warn のみ（非throw）。
- **受け入れ**: 成功/失敗で適切な dir へ移動、no-op、非throw。
- **依存**: IOS-01。

## IOS-04: FileIngestionLifecycleListener（B）

- **対象（新規）**: `io/FileIngestionLifecycleListener.java`（opt-in `JobExecutionListener`）。
- **準拠**: `FileArchivePolicy` を DI。`beforeJob` で入力（パラメータ `INPUT_FILE_PARAMETER="koiki.io.inputFile"`）の存在・非空を検査、欠落/空は `BusinessException`（→20）。`afterJob` で `COMPLETED`→archive、他→moveToError。入力未指定で no-op。
- **受け入れ**: 欠落/空で BusinessException、完了で archive、失敗で moveToError。
- **依存**: IOS-03、fault。

## IOS-05: AtomicFileOutput + AtomicOutputListener（C）

- **対象（新規）**: `io/AtomicFileOutput.java`（`Path tempPath(Path finalPath, String suffix)`、`void promote(Path temp, Path finalPath)`、`void discard(Path temp)`）、`io/AtomicOutputListener.java`（opt-in `JobExecutionListener`）。
- **準拠**: listener は最終パス（パラメータ `OUTPUT_FILE_PARAMETER="koiki.io.outputFile"`）に対し `afterJob`：`COMPLETED`→`promote(temp,final)`、他→`discard(temp)`。temp は `tempSuffix` 規約。出力未指定で no-op。promote/discard 失敗は warn のみ。
- **受け入れ**: 完了で temp→final 昇格、失敗で temp 破棄、no-op、非throw。
- **依存**: IOS-01。

## IOS-06: core 自動構成への登録

- **対象（変更）**: `core/BatchCoreAutoConfiguration.java`。
- **準拠**: `DirectoryFileArchivePolicy`（dir 注入）、`FileIngestionLifecycleListener`、`AtomicOutputListener` を `@Bean @ConditionalOnMissingBean`。
- **受け入れ**: 既定で各 bean 単一。アプリ実装が優先。
- **依存**: IOS-03..05。

## IOS-07: 単体テスト

- **対象（新規）**: `io/DirectoryFileArchivePolicyTest`、`io/FileIngestionLifecycleListenerTest`、`io/AtomicFileOutputTest`／`AtomicOutputListenerTest`、`io/BatchCharsetsTest`、`core/BatchCoreAutoConfigurationTest` 拡張。
- **準拠**: JUnit 5、`@TempDir`、listener はモック policy + 合成 `JobExecution`。
- **受け入れ**: 移動/昇格/破棄・no-op・非throw、precondition の BusinessException、プロパティ既定/バインド、自動構成の単一 bean。
- **依存**: IOS-01..06。

## IOS-08: 参照 file→file ジョブ（A/B/C 実証）

- **対象（新規）**: `jobs/billing/BillingFileJobConfig.java`、`model/BillingFileRecord.java`、`jobs/billing/BillingFileValidator.java`。
- **準拠**: `FlatFileItemReader`（`DelimitedLineTokenizer`、設定 charset）→ `Validator` processor → `FlatFileItemWriter`（`DelimitedLineAggregator`、**一時パス**、設定 charset）。`FileIngestionLifecycleListener`＋`AtomicOutputListener` を attach。chunk は `defaultCommitInterval`。
- **受け入れ**: 正常データで最終出力生成・入力 archive 移動。
- **依存**: IOS-01..06、Phase4 validation/transaction。

## IOS-09: 参照アプリ IT

- **対象（新規）**: `jobs/billing/BillingFileJobIT.java`、テストリソース（MS932/UTF-8 サンプル）。
- **準拠**: `@TempDir`/設定で input/archive/error/output ディレクトリ。(a) 正常→最終出力生成（一時消滅）・入力 archive、(b) charset 切替（MS932/UTF-8）、(c) 入力欠落/空→business error(20)、(d) 検証失敗→business error(20)・入力 error 移動・最終出力なし。
- **受け入れ**: IT 単体／`clean verify` 双方通過。既存 IT 全件グリーン維持。
- **依存**: IOS-01..08。

---

## メモ

- リジェクトファイル(D)・パス解決規約(E)・固定長多レコード・標準ディレクトリ命名・外部API I/O・広い Reader/Writer 層は本タスク対象外（plan 参照）。
- `support` は実需が出るまでスタブ維持。
- 後処理失敗は業務を止めない（warn）。確実性要件はアプリで強化。
