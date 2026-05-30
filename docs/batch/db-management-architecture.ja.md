# DB管理アーキテクチャ（日本語版 / Spring Batch 6・Spring Boot 4）

本書は [db-management-architecture.md](db-management-architecture.md)（英語・正典）の**日本語コンパニオン**です。業務・運用チームが「運用要件に応じたバッチDB設定」を判断するための要点を日本語で確認できるようにしたものです。設計判断の最終正典は [decision-log.md](decision-log.md)。内容は英語版と同期させて更新します。

対象: Spring Batch 6.0.x / Spring Boot 4.0.x。`v0.1.0` の合意ベースライン。

## 0. 最初に押さえる前提

- 「**メタデータDBを持たない（resourceless）**」ことと「**業務RDBMSを扱えない**」ことは**別物**です。resourceless でも業務データの読み書き・トランザクションは可能です。
- バッチには独立した2つの永続関心事があります。これを混同しないことが設計の肝です。

| # | 関心事 | 保存対象 | 担い手 |
| --- | --- | --- | --- |
| 1 | **バッチ・メタデータ**（JobRepository） | ジョブ/ステップ実行状態＝**リスタート/履歴**の土台 | フレームワーク自動構成 + アプリの `DataSource` 配線 |
| 2 | **業務データ処理** | chunk の read/process/write による業務データ | アプリ（DAO/SQL）。reader/writer 契約は `io`＝Phase 5 |

横断で **③トランザクション管理**（両者を束ねる）と **④スキーマ管理**（両テーブル群の作成）があります。

## 1. メタデータ永続の2モード

| モード | クラス | DB | リスタート | 並行性 | 用途 |
| --- | --- | --- | --- | --- | --- |
| Resourceless（既定） | `ResourcelessJobRepository` | 不要 | **不可** | 非スレッドセーフ | 開発・スモーク・冪等な軽量バッチ |
| JDBC（オプトイン） | JDBC `JobRepository`（`BATCH_*`） | 必要 | **可** | 安全 | 本番のDB-backedジョブ |

`v0.1.0` の方針:

- **フレームワーク既定は Resourceless**（非破壊・現コードと一致）。
- **JDBC は明示オプトイン**。

### 重要（Boot4/SB6 の実態・裏取り済み）

> **DataSource を置くだけでは JDBC 永続になりません。** Boot 4.0.6 / SB 6.0.3 を jar 実体と実行で確認したところ、Boot の `BatchAutoConfiguration` は SB の `DefaultBatchConfiguration` を継承し、その既定 `jobRepository()` は **`ResourcelessJobRepository`** を返します（Boot 3 までの「DataSource があれば JDBC 化」は Boot 4 では成り立ちません）。

JDBC 永続にするには、アプリ側で明示的にオプトインします（Boot が用意する `dataSource` / `transactionManager` を流用）:

```java
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(dataSourceRef = "dataSource", transactionManagerRef = "transactionManager")
class BatchJobRepositoryConfig {}
```

- `@EnableBatchProcessing` により Boot の既定バッチ構成（resourceless）が退避し、ここで構成した JDBC リポジトリが使われます。
- Boot の**ジョブ起動・終了コード生成は維持**されるため、フレームワークの `0/10/20/30` 終了コードは引き続き機能します（参照アプリの exit-code IT で実証済み）。
- このオプトインは**アプリの関心事**で、フレームワークモジュールには置きません。

## 2. リポジトリモードの運用使い分け

| 機能 | Resourceless（既定） | JDBC（オプトイン） |
| --- | --- | --- |
| 業務RDBMS の read/write・トランザクション | ✅（業務 `DataSource` + TM） | ✅ |
| chunk コミット境界・skip/retry | ✅ | ✅ |
| 終了コード 0/10/20/30 | ✅（分類器由来・リポジトリ非依存） | ✅ |
| observability / audit / masking | ✅ | ✅ |
| **失敗箇所からのリスタート** | ❌ 再実行は頭から | ✅ |
| **プロセス跨ぎの多重起動防止・同時実行ガード** | ⚠️ メモリ内のみ・プロセス単位 | ✅ |
| 実行履歴（`BATCH_*`）の運用/監査参照 | ❌ | ✅ |
| 並列/パーティション/マルチスレッドStep | ❌ 非スレッドセーフ | ✅ |
| 業務+メタの原子コミット | 該当なし（メタ非永続） | ✅（単一DataSource共有時） |

**Resourceless が妥当**: 冪等・全件再実行で良いジョブ（全件洗い替え、ステートレス変換）、単一プロセス・単一スレッド、JP1 が頭から再投入する運用。メタDB運用が不要。

**JDBC が必要**: 失敗箇所からの再開、プロセス跨ぎの多重起動防止（実用的な同時実行ガード）、運用/監査向けの実行履歴、並列/パーティションStep。

> 1プロセス（1 Spring コンテキスト）の `JobRepository` は**1種のみ**。両方を提供するには実行体を分ける（参照アプリは Spring プロファイルで切替え）。

## 3. トランザクションマネージャの役割（`DataSourceTransactionManager` と resourceless の関係）

「resourceless かどうか（=メタデータを永続するか）」と「業務データのトランザクション」は**別レイヤ**です。混同しないでください。

- **業務トランザクション** … chunk step に渡す `PlatformTransactionManager`（業務 `DataSource` 由来の `DataSourceTransactionManager`/`JdbcTransactionManager`）が、業務データの read/process/write を **commit-interval（チャンクサイズ）単位でコミット/ロールバック**します。
- **メタデータトランザクション** … `JobRepository` のメソッド周りのトランザクション。実行状態を永続します（JDBC モード時）。resourceless モードでは内部的に `ResourcelessTransactionManager` を使い、永続しません。

つまり **resourceless でも、業務TM を chunk step に渡せば業務データは正しくトランザクション保護されます**。resourceless が落とすのは「メタデータの永続」だけで、業務データの整合性ではありません。

### TM は誰が用意するか（フレームワークは TM bean を作らない）

| ケース | 業務頻度 | TM の出どころ | アプリの手間 |
| --- | --- | --- | --- |
| **DB-backed**（DataSource あり） | 高 | **Spring Boot が自動構成**（`JdbcTransactionManager`） | ほぼゼロ（chunk step に注入して渡すだけ） |
| **DB-less**（業務DBなし tasklet） | 低 | アプリが `ResourcelessTransactionManager` を1行宣言 | 1行 |
| **マルチDB** | 低・特殊 | `@BatchDataSource` + 複数TM | 本質的に複雑（deferred） |

フレームワークは**フォールバックの TM bean を提供しません**。設定漏れは「黙って非トランザクション実行」より「fail-fast で気づく」方が安全だからです（SB6 では chunk step の TM 未指定時に resourceless へフォールバックする落とし穴があるため、明示配線を推奨）。

## 4. トランザクションモデル（単一DataSource共有＝原子コミット）

**メタデータと業務データを同一 `DataSource`・同一 TM** で扱うと、各チャンクのコミットで**業務行の書き込みとステップ実行メタデータ更新が同一トランザクションでコミット**され、リスタート整合（実質 exactly-once／チャンク）が得られます。これを `v0.1.0` の標準トポロジとします。

```
        ┌──────────── 単一 DataSourceTransactionManager ────────────┐
 read → process → write（業務行）  +  ステップ実行メタ更新（BATCH_*） │
        └───────── commit-interval ごとに 1 回のコミット ───────────┘
```

別DB分離（`@BatchDataSource`/`@BatchTransactionManager`）は、メタは堅牢でも業務↔メタが非原子になるため **deferred**。

## 5. スキーマ管理（Flyway 一元）

- **Flyway を全スキーマの単一管理元**とし、`spring.batch.jdbc.initialize-schema=never`。
- 業務テーブルも `BATCH_*` メタデータも Flyway 移行で作成。
- Flyway と Boot のバッチ初期化の二重実行（順序競合）を回避し、DBA が移行で管理し本番では自動初期化を切る運用と整合。

## 6. H2 と本番RDBMS（重要）

参照アプリ／テストは **H2 のインメモリモード**（`jdbc:h2:mem:...;DB_CLOSE_DELAY=-1`）を使用します。

- **`mem:`** = データは JVM ヒープ上のみ。**プロセス終了で消滅**（`DB_CLOSE_DELAY=-1` は JVM 生存中の保持。JVM をまたぐ永続ではない）。
- これは**テスト/デモ専用**です。

本番では**永続・本番グレードの RDBMS**（PostgreSQL / Oracle / SQL Server 等）を配置してください。

- H2 インメモリは再起動で全消失 → 業務データに致命的。JDBC `JobRepository` ではリスタート/履歴が無意味化。
- H2 ファイルモードは永続だが、エンタープライズ本番には非推奨。
- **resourceless ジョブでも、業務データを扱うなら永続 RDBMS は必要**（メタは持たないが業務テーブルは読み書きする）。

### 本番化で変える箇所（フレームワークは不変）

1. 本番 JDBC ドライバを追加（例 `org.postgresql:postgresql` / `com.oracle.database.jdbc:ojdbc11`）。
2. `DataSource` の URL/認証情報を**外部化**（環境変数・Secret。`application.yml` にベタ書きしない）。
3. **方言別の `BATCH_*` 移行に差し替え**。参照の `V002__create_batch_metadata.sql` は **H2 専用DDL**（`schema-h2.sql` 逐語コピー）。本番は対応する `schema-postgresql.sql` / `schema-oracle10g.sql` 等を使用し、業務テーブルDDLの方言差も確認。
4. `spring.batch.jdbc.initialize-schema=never`（Flyway 一元）は維持。

本番DB方言とロック等の挙動差は別途 **deferred**（[decision-log.md](decision-log.md)）。

## 7. 参照アプリでの実証（両モード）

参照アプリ `koiki_ref_batch_app` は同一ジョブ `customer-import` を**両モードで実証**します。

- **既定プロファイル = resourceless**: 業務テーブル（H2）への read/process/write は機能し、`BATCH_*` は空（メタ非永続）。`CustomerImportResourcelessIT` で確認。
- **`jdbc-repository` プロファイル = JDBC**: 業務データ + メタデータを永続（リスタート土台）。`CustomerImportJobIT`（`@ActiveProfiles("jdbc-repository")`）で確認。`application-jdbc-repository.yml` で独立H2に隔離。

## 8. アンチパターン / 責務の置き場

- TX/retry/終了コードの挙動を無関係なユーティリティに隠さない。
- フレームワークはフォールバック TM を提供しない（設定漏れは fail-fast）。
- フレームワークモジュールに `@EnableBatchProcessing` / `@EnableJdbcJobRepository` / `JobRepository` を置かない（ストア選択はアプリの関心事）。

| 関心事 | 置き場 |
| --- | --- |
| バッチ基盤の自動構成統合 | `org.koikifw.libkoiki.batch.core`（Bootに追加・再定義しない） |
| コミット境界/トランザクション方針 | `org.koikifw.libkoiki.batch.transaction`（プロパティ+ガイド・**TM bean なし**） |
| リスタート/リラン/同時実行ガード | `org.koikifw.libkoiki.batch.execution` |
| reader/writer 契約 | `org.koikifw.libkoiki.batch.io`（**Phase 5**。当面は標準SB reader/writer） |
| 障害→終了コード変換 | `org.koikifw.libkoiki.batch.fault` |
| `DataSource`・TM bean・業務DAO/SQL・Flyway移行 | **アプリ**（参照アプリ / `apps/*`） |
