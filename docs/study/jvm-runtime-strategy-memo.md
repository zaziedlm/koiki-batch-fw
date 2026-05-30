# JVM実行戦略 検討メモ

> 目的：バッチ実行基盤の選択に関する検討経緯の備忘記録  
> 作成日：2026-05-22

---

## 背景・課題

`apps/` 配下に顧客ごと・システムごとのバッチアプリを複数モジュールとして追加していく構成を採用した場合、JP1などのジョブスケジューラから各JARファイルを個別JVMプロセスで起動運用することになる。

複数JVMを同時起動する運用では以下のコストが発生する：

- **メモリ消費**：1プロセスあたりヒープ＋Metaspace＋スレッドスタックで200MB〜700MB超
- **起動時間**：Spring Boot初期化で3〜10秒程度
- **リソース競合**：ジョブ同時実行時のサーバーリソース逼迫

---

## 検討した改善手段

### 1. AppCDS（Application Class Data Sharing）

- JDK標準機能（JDK 12以降）
- クラスロード情報をファイルにキャッシュし、次回起動から再利用
- 起動時間を30〜50%短縮できる実績あり
- Spring Boot 3.x以降は公式サポート済み
- **導入コスト低**：JVMオプション追加のみ、コード変更不要
- アーカイブは実行アプリケーション単位（`customer-a.jsa`, `customer-b.jsa` ...）で管理

```bash
# アーカイブ生成（デプロイ後に一度だけ実行）
java -XX:ArchiveClassesAtExit=customer-a.jsa \
     -cp "lib/*:customer-a-batch-app-0.1.0-SNAPSHOT.jar" \
     org.koikifw.customer.a.batch.CustomerABatchApp

# 通常起動（JP1ジョブ定義に追加）
java -XX:SharedArchiveFile=customer-a.jsa \
     -cp "lib/*:customer-a-batch-app-0.1.0-SNAPSHOT.jar" \
     org.koikifw.customer.a.batch.CustomerABatchApp
```

### 2. JVMヒープのサイジング

- バッチ処理は常駐しないため `-Xmx` を実績に基づき小さく絞れる（例：`-Xmx256m`）
- JP1の同時実行制御と組み合わせてリソース上限を管理する

### 3. GraalVM Native Image

- AOTコンパイルでJVMを排除し、OS依存のネイティブバイナリを生成
- 起動時間：3〜10秒 → 50〜200ms
- メモリ使用量：200MB+ → 30〜80MB程度
- **制約**：ビルド環境 = 実行環境OS（クロスコンパイル不可）
- **制約**：リフレクション多用箇所に `reflect-config.json` などの追加設定が必要
- Spring Batch 5.x / Spring Boot 3.x 以降でAOTヒントが整備され実用段階
- 導入コスト・ビルド時間（数分〜10分超）は高いが、効果も大きい

### 4. 単一アプリ・マルチジョブ構成

- 複数ジョブを1つのアプリに内包し、JP1からジョブ名をパラメータで渡す
- JVMプロセス数を減らせるが、顧客をまたぐ場合はモジュール境界が崩れるリスクあり
- このプロジェクトの設計方針（顧客ごとに独立した `apps/` モジュール）とは相性が悪いため不採用方向

---

## フレームワーク選択（Spring Batch vs 代替）

### Micronaut の検討

- Native Image対応が優秀（AOTを設計から考慮）
- ただし **バッチ専用フレームワーク機能がない**
  - チャンク処理、JobRepository、Retry/Skip、パーティション並列などを自前実装する必要あり
  - エンタープライズバッチ要件には不向き

### Quarkus + JBeret の検討

- JSR-352（Java EE Batch標準）ベースでSpring Batchに近い概念
- Native Image対応は優秀
- JP1連携の実績が少なく、移行コスト大

### 判断

**Spring Batch を維持する。**

Spring Batchが提供する再実行制御・Skip/Retry・JobRepository・パーティション並列などのエンタープライズバッチ機能は、このプロジェクトの価値の核である。フレームワーク乗り換えのコストとリスクは、Native Image対応のメリットを上回らない。

---

## 結論・方針

| フェーズ | 対応 |
|---|---|
| **短期** | AppCDSを各アプリに適用（JVMオプション追加のみ） |
| **中期** | JVMヒープを各ジョブの実績値から適切にサイジング |
| **長期** | GraalVM Native Imageをケースに応じて検討（Spring Boot 4.x + Spring Batch 6.x の対応状況を見ながら） |

Spring Batchベースの開発を基本とし、AppCDSや将来的なNative Image化などの最適化をAI駆動開発の素早いサイクルで取り込んでいく方針とする。
