# koiki-batch-fw

KOIKI Batch Framework は、企業向け Java バッチアプリケーションを Spring Batch で構築するためのプロジェクト雛形です。

このリポジトリは、KOIKI Web と同じ考え方である「共通基盤 + 参照アプリ + 顧客別アプリ」をバッチ領域に適用します。共通処理を `libkoiki-batch` に集約し、参照実装で標準的な使い方を示し、顧客別アプリでは個別要件だけを実装する構成を目指します。

## Status

Current development line: `v0.1.0`

Maven version: `0.1.0-SNAPSHOT`

現時点では、バッチ基盤としての最小構造、Maven マルチモジュール、依存関係のベースライン、責務分離のパッケージ構成、運用ドキュメントの入口までを整備しています。

実ジョブ、Reader / Processor / Writer、監査永続化、標準トランザクションポリシー、障害分類の実装は今後拡張する前提です。ただし、それらを配置するパッケージと設計上の責務は先に定義済みです。

## Technology Baseline

- Java 21
- Maven multi-module project
- Spring Boot 4.0.x
- Spring Batch 6.0.x
- Official package root: `org.koikifw.*`

`koikifw.org` ドメインを正式ドメインとし、Java パッケージは `org.koikifw.*` を標準とします。

## Module Structure

```text
koiki-batch-fw/
├─ pom.xml
├─ docs/
│  ├─ agent/
│  └─ batch/
├─ components/
│  ├─ libkoiki-batch/
│  └─ koiki_ref_batch_app/
├─ apps/
│  └─ customer_a_batch_app/
└─ ops/
   └─ jp1/
```

### `components/libkoiki-batch`

再利用可能な共通バッチ基盤です。

このモジュールには、企業向けバッチで横断的に必要になる機能を配置します。業務固有の処理は置きません。

主な責務:

- Spring Boot auto-configuration
- ジョブ実行制御
- 同時実行制御
- 障害分類
- 終了コード方針
- 構造化ログ
- 監査イベント
- トランザクション境界
- I/O 共通部品
- 入力・パラメータ検証
- セキュリティ、マスキング、機密情報の境界

### `components/koiki_ref_batch_app`

参照業務バッチアプリケーションです。

`libkoiki-batch` の標準的な使い方を示し、顧客アプリを作る際の実装例として機能します。ここにはサンプル業務ジョブ、参照モデル、参照リポジトリ、参照サービスを配置します。

### `apps/*`

顧客別または下流システム別のバッチアプリケーションを配置します。

顧客固有の Job 設定、入出力仕様、業務サービス、連携処理はここに置きます。共通化できるものは `libkoiki-batch` に戻すか、参照実装として `koiki_ref_batch_app` に反映します。

### `ops/jp1`

JP1 などのスケジューラ連携、終了コード、再実行手順、運用 Runbook を配置します。

現時点では JP1 を主な運用連携先として想定しています。

## Framework Package Map

共通基盤のパッケージは `org.koikifw.libkoiki.batch.*` 配下に配置します。

| Package | Responsibility |
| --- | --- |
| `core` | Spring Boot auto-configuration and framework defaults |
| `execution` | Job execution policy, concurrency guard, restart/rerun support |
| `fault` | Exception classification, retry/skip policy, exit code mapping |
| `io` | Reusable readers/writers and adapter contracts |
| `observability` | Structured logging, metrics, job/step lifecycle listeners |
| `audit` | Audit event model and audit trail publication |
| `security` | Secret handling, credential boundaries, masking hooks |
| `transaction` | Transaction policy, transaction manager helpers, commit boundary guidance |
| `validation` | Job parameter, input, and business rule validation support |
| `support` | Small shared utilities that do not own business rules |

## Batch Design Principles

各バッチジョブは、Spring Batch の責務を明示的に分離して実装します。

- Job configuration: フロー、遷移、再実行可否
- Step configuration: chunk/tasklet、commit interval、transaction boundary
- Reader / Processor / Writer: 入力、変換、出力
- Domain service: 業務ルール
- Repository / adapter: DB、ファイル、外部 API
- Observability: 構造化ログ、ジョブ/ステップ単位の診断情報
- Audit: 業務上意味のある更新・制御イベント
- Fault handling: 技術例外と業務例外の分類、終了コードへの変換

共通基盤は業務を知りすぎないようにします。顧客アプリは基盤実装を直接改変せず、設定・拡張ポイント・個別アダプタで差分を表現します。

## Enterprise Concerns

### Logging

通常ログは運用診断を目的とします。ジョブ名、ジョブ実行 ID、ステップ名、営業日、顧客/テナントキー、スケジューラ要求 ID などを相関情報として扱える構造を目指します。

個人情報、秘密情報、認証情報はログに出さない方針です。マスキングや出力抑止の共通フックは `security` および `observability` に配置します。

### Transaction

トランザクション境界は Step 単位で明示します。

Chunk step では commit interval、retry/skip、rollback 対象を明確にします。Tasklet step では冪等性と再実行時の扱いを明記します。

共通の方針やヘルパーは `transaction` に配置し、DB や業務固有の詳細はアプリケーション側で定義します。

### Audit

監査は通常ログとは分離します。

ログは障害解析や運用確認のための情報であり、監査は「何が、いつ、どのバッチ実行により、どの制御文脈で変更されたか」を後から説明するための情報です。

監査イベントのモデル、発行インターフェース、永続化境界は `audit` に配置します。永続化方式は今後の決定事項です。

### Fault Handling

例外クラスをそのまま運用仕様にしません。

共通基盤では、技術障害、入力不備、業務不整合、再実行可能障害などを分類し、安定した終了コードに変換する方針を持ちます。JP1 などのスケジューラは、例外名ではなく終了コードと Runbook を参照します。

## Build

VS Code Extension Pack for Java Auto Config を使っている場合、JDK と Maven は以下のような場所に配置されることがあります。

```powershell
$env:JAVA_HOME="$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\21"
& "$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\maven\latest\bin\mvn.cmd" clean test
```

通常の PATH に Java と Maven が通っている環境では、以下で確認できます。

```bash
mvn clean test
```

現在の構成では、全モジュールの `mvn clean test` が成功することを確認しています。

## Documentation

詳細は以下のドキュメントを参照してください。

- `docs/batch/architecture-batch.md`: バッチ構成と責務分離
- `docs/batch/platform-capabilities.md`: 共通基盤の機能マップ
- `docs/batch/decision-log.md`: 設計判断の履歴
- `docs/batch/operation-runbook.md`: 運用 Runbook 入口
- `docs/batch/naming-conventions.md`: 命名規約
- `docs/batch/migration-guidelines.md`: DB migration 方針
- `docs/agent/*`: Codex / agent 作業向け補助情報

## Current Gaps

以下は今後の実装・整理対象です。

- 実際の Spring Batch Job / Step 実装
- Reader / Processor / Writer の参照実装
- 標準的な exit code catalog
- 構造化ログの出力形式
- audit event publisher
- transaction policy helper
- fault classifier
- validation contracts
- `customer_b_batch_app` など追加顧客アプリ
- `tests/integration` / `tests/e2e`
- OWASP Dependency-Check などの SCA 実行を CI に組み込むこと

## Development Notes

新しい共通機能を追加するときは、まず `docs/batch/platform-capabilities.md` の責務に照らして配置先を決めます。

迷った場合は、業務固有なら `components/koiki_ref_batch_app` または `apps/*`、複数アプリで再利用する横断機能なら `components/libkoiki-batch` に置きます。

設計判断が残るものは `docs/batch/decision-log.md` に追記します。

## 🔒 Fork・利用に関するご案内

このリポジトリはパブリック公開されていますが、以下の条件を遵守いただける方以外の Fork・再利用はご遠慮ください。

- Fork の前に、必ずリポジトリ管理者（@zaziedlm）にご連絡ください

無断でのForkや再利用が確認された場合、GitHubへの削除申請を行うことがあります。ご理解とご協力をお願いします。

## ライセンス

MIT License

https://opensource.org/license/mit
