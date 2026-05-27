# Phase 0 / core タスク

| 項目 | 値 |
| --- | --- |
| 対象パッケージ | `org.koikifw.libkoiki.batch.core` |
| 計画 | [10-core.md](../plans/10-core.md) |
| 検証コマンド | `mvn -pl components/libkoiki-batch test` |
| ステータス | Done（Spring Batch 6.0.3 / Spring Boot 4.0.6 で実装・全テスト通過） |

> 実装時の確定事項: `BatchAutoConfiguration` は Boot 4 で `org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration`（`spring-boot-batch` モジュール）に移動。`@AutoConfiguration`・`@ConditionalOn*` は `org.springframework.boot.autoconfigure(.condition)`、`@ConfigurationProperties`/`@EnableConfigurationProperties` は `org.springframework.boot.context.properties`、`ExitCodeGenerator` は `org.springframework.boot` のまま。`AutoConfiguration.imports` は既存（内容も正）だった。

共通の準拠事項は [ロードマップの準拠仕様](../plans/00-libkoiki-batch-roadmap.md) と各計画の「準拠仕様」節に従う。非推奨 API（`JobExplorer` / `JobLauncher` / `CommandLineJobRunner`）は使わない。

---

## CORE-01: AutoConfiguration.imports の作成

- **概要**: 自動構成が拾われるよう登録ファイルを新規作成。
- **対象ファイル（新規）**: `components/libkoiki-batch/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **準拠仕様**: Spring Boot 4 の自動構成登録方式（`spring.factories` ではなく `AutoConfiguration.imports`）。
- **内容**: `org.koikifw.libkoiki.batch.core.BatchCoreAutoConfiguration` の1行。
- **受け入れ条件**: 登録後、自動構成クラスが Boot コンテキストでインポートされる。
- **依存**: なし（最優先）。

## CORE-02: KoikiBatchProperties の追加

- **概要**: framework の振る舞いトグルを `@ConfigurationProperties` でバインド。
- **対象ファイル（新規）**: `.../batch/core/KoikiBatchProperties.java`
- **準拠仕様**: `@ConfigurationProperties(prefix = "koiki.batch")`、コンストラクタ/レコードバインド、ネストは内部クラス。
- **内容**: `concurrencyGuard.enabled` / `exitCode.enabled` / `logging.correlation.enabled`（初期最小）。
- **受け入れ条件**: `koiki.batch.*` がバインドされ、既定値が定義される。
- **依存**: なし。

## CORE-03: BatchCoreAutoConfiguration の実体化

- **概要**: 空の `@Configuration` を `@AutoConfiguration` 化し、framework bean 登録の受け皿にする。
- **対象ファイル（変更）**: `.../batch/core/BatchCoreAutoConfiguration.java`
- **準拠仕様**: `@AutoConfiguration(after = org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration.class)` + `@EnableConfigurationProperties(KoikiBatchProperties.class)`。Boot のインフラ bean（JobRepository/JobOperator/transactionManager）は**再定義しない**。framework bean は `@Bean @ConditionalOnMissingBean`（必要に応じ `@ConditionalOnProperty`）。
- **受け入れ条件**: コンテキストが起動し、Boot のインフラ bean と衝突しない。
- **依存**: CORE-01, CORE-02。

## CORE-04: スライステスト

- **概要**: 自動構成の挙動を `ApplicationContextRunner` で検証。
- **対象ファイル（変更/新規）**: `.../batch/core/BatchCoreAutoConfigurationTest.java`（既存プレースホルダを拡張）
- **準拠仕様**: `org.springframework.boot.test.context.runner.ApplicationContextRunner`。
- **受け入れ条件**: (a) `KoikiBatchProperties` バインド、(b) framework bean 登録、(c) `*.enabled=false` でオプトアウト、(d) Boot インフラ bean と非衝突、を検証。`mvn -pl components/libkoiki-batch test` が通る。
- **依存**: CORE-03。

---

## メモ

- 後続 Phase の bean（execution の `ConcurrencyGuardService`、fault の classifier / exit-code generator）は本 AutoConfiguration に条件付き登録を追記していく。タスクは各 Phase 側で管理。
