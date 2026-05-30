# Phase 0 / core 実装計画

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0` |
| 対象パッケージ | `org.koikifw.libkoiki.batch.core` |
| ステータス | Done（`v0.1.0` 初期スコープを実装済み） |
| 関連 | [ロードマップ](00-libkoiki-batch-roadmap.md) / [タスク](../tasks/10-core.md) / [platform-capabilities.md](../batch/platform-capabilities.md) / [boundaries.md](../agent/boundaries.md) |

## 狙い

`core` は framework の自動構成入口。現在は [BatchCoreAutoConfiguration.java](../../components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/core/BatchCoreAutoConfiguration.java) が `@AutoConfiguration` として実体化され、`AutoConfiguration.imports` にも登録済みである。後続 Phase の listener / classifier / exit-code generator / concurrency guard / audit / security / io 関連 bean を、Spring Boot の Batch インフラを再定義せず追加する受け皿になっている。

## 準拠仕様（Spring Batch 6.0.x / Spring Boot 4）

- 自動構成は `@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登録。
- Boot の `BatchAutoConfiguration`（**Boot 4 では `org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration`**、`spring-boot-batch` モジュール。旧 `...autoconfigure.batch` から移動）が用意するインフラ bean（`JobRepository` / `JobOperator` 等）は**再定義しない**。`@AutoConfiguration(after = BatchAutoConfiguration.class)` で framework 由来 bean を**追加**するに留める。
- `JobOperator` が `JobLauncher` を、`JobRepository` が `JobExplorer` を継承（後続 Phase で利用）。非推奨 API は使わない。
- bean はコンストラクタインジェクション、`@ConfigurationProperties` バインド、`@ConditionalOnMissingBean` / `@ConditionalOnClass` でオプトアウト可能化。

## 設計

### 1. `KoikiBatchProperties`

`@ConfigurationProperties(prefix = "koiki.batch")` で framework の振る舞いトグルをバインド。初期は最小:

```text
koiki.batch:
  concurrency-guard.enabled: true   # 同時実行ガード（execution）
  exit-code.enabled: true           # 終了コードマッピング（fault）
  logging.correlation.enabled: true # ログ相関（observability）
```

- 各 Phase が必要なキーのみ段階的に追加する。
- ネストは機能ごとの内部クラスで表現（`Concurrency` / `ExitCode` / `Logging`）。

### 2. `BatchCoreAutoConfiguration` の実体化

- `@AutoConfiguration(after = org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration.class)`
- `@EnableConfigurationProperties(KoikiBatchProperties.class)`
- framework 由来 bean を `@Bean @ConditionalOnMissingBean`（必要に応じ `@ConditionalOnProperty`）で登録する受け皿。Phase 0 時点では execution（`ConcurrencyGuardService`）・fault（classifier / exit-code generator）の bean をここで条件付き登録する想定。
- インフラ bean（JobRepository/JobOperator/transactionManager）は宣言しない。

### 3. 自動構成の登録

- 新規ファイル: `components/libkoiki-batch/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 内容: `org.koikifw.libkoiki.batch.core.BatchCoreAutoConfiguration` の1行。

## 検証

- `ApplicationContextRunner` を使ったスライステストで、(a) `KoikiBatchProperties` がバインドされる、(b) framework bean が登録される、(c) `koiki.batch.*.enabled=false` でオプトアウトできる、(d) Boot のインフラ bean と衝突しない、を確認。
- `mvn -pl components/libkoiki-batch test`。

## スコープ外 / deferred

- 実際の listener / classifier の中身は observability・fault 側で実装（core はワイヤリングのみ）。
- プロパティの本格的な階層設計は各 Phase 確定後に拡張。
