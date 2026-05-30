# レビュー指摘対応タスク

| 項目 | 値 |
| --- | --- |
| 計画 | [90-review-findings-remediation.md](../plans/90-review-findings-remediation.md) |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| ブランチ | `topic/review-findings-plan` |
| 検証 | 短時間確認は `mvn clean test`、完了確認は `mvn verify` |
| ステータス | Done |

## R1: ロードマップとステータス文書の現状反映

- **ステータス**: Done（ロードマップ/Phase plan/task の現状反映を実施）
- **優先度**: High
- **対象**:
  - `docs/plans/00-libkoiki-batch-roadmap.md`
  - `docs/plans/10-core.md` から `80-io-support.md`
  - `docs/tasks/10-core.md` から `80-io-support.md`
  - `docs/tasks/retrospective/phase0-retrospective.md`
- **作業**:
  - 実装済み領域をスタブとして説明している記述を置き換える。
  - コードとテストが存在する phase/task は、実装済みとしてステータスを更新する。
  - deferred 項目は削除せず、意図的に残す。
  - `v0.1.0` 時点の current-state summary を追加する。
- **受け入れ条件**:
  - `BatchCoreAutoConfiguration`、`AutoConfiguration.imports`、`ConcurrencyGuardService`、`JobLogListener` が空スタブであるという記述が残っていない。
  - `rg "実装は全てスタブ|現状空|未着手" docs/plans docs/tasks` を確認し、残る一致は意図的なものとして説明できる。

## R2: 顧客アプリのパッケージ/依存境界整理

- **ステータス**: Done（`org.koikifw.customer.a.batch.*` へ移動し、参照アプリ依存を削除）
- **優先度**: Medium
- **対象**:
  - `apps/customer_a_batch_app/pom.xml`
  - `apps/customer_a_batch_app/src/main/java/...`
  - `apps/customer_a_batch_app/src/test/java/...`
  - `docs/agent/boundaries.md`
  - 必要に応じて `README.md`
- **作業**:
  - 顧客別サンプルアプリの公式パッケージルートを決める。
  - 採用方針に従い、`com.customer.a.batch.*` を `org.koikifw.*` 配下へ移動する。
  - `koiki-ref-batch-app` への依存を削除する。残す場合は、一時的なサンプル目的として明記する。
  - placeholder test を、最小 wiring test または境界確認 test へ置き換える。顧客アプリを skeleton として残す場合は、その扱いを明記する。
- **受け入れ条件**:
  - `rg "package com\\.|com/customer" apps/customer_a_batch_app` に一致がない、または残る一致が顧客所有パッケージとして意図的に説明されている。
  - 顧客アプリの依存方向が、原則として「顧客アプリ -> framework」になっている。
  - `mvn verify` が成功する。

## R3: atomic output の失敗時セマンティクス強化

- **ステータス**: Done（promote 失敗を `SystemException` とし、`JobExecution` を failed に更新）
- **優先度**: Medium
- **対象**:
  - `components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/io/AtomicFileOutput.java`
  - `components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/io/AtomicOutputListener.java`
  - `components/libkoiki-batch/src/test/java/org/koikifw/libkoiki/batch/io/AtomicOutputTest.java`
  - 必要に応じて参照アプリの `billing-file` IT
- **作業**:
  - 最終出力ファイルへの昇格失敗を warn-only ではなく fail-fast に変更する。
  - 終了コード `30` に分類される framework 例外を使う。
  - temp 破棄や cleanup 失敗は、より強い運用要件がない限り best-effort のままにする。
  - 最終ファイル公開失敗がジョブ結果へ反映されることをテストで確認する。
- **受け入れ条件**:
  - promote 失敗時に、ジョブが正常成功に見えない。
  - 既存の成功/失敗 cleanup テストが通る。
  - `billing-file` 参照アプリ統合テストが通る。

## R4: audit formatter の堅牢化

- **ステータス**: Done（quote/改行/制御文字の escape と null attribute value をテスト済み）
- **優先度**: Medium
- **対象**:
  - `components/libkoiki-batch/src/main/java/org/koikifw/libkoiki/batch/audit/LoggingAuditEventPublisher.java`
  - `components/libkoiki-batch/src/test/java/org/koikifw/libkoiki/batch/audit/LoggingAuditEventPublisherTest.java`
  - `docs/plans/50-audit.md`
  - `docs/tasks/50-audit.md`
- **作業**:
  - quote 付き audit field 内に出力する値を escape または正規化する。
  - 少なくとも quote、backslash、CR/LF、tab、null attribute value を扱う。
  - sensitive-key masking の挙動は維持する。
  - `publish(...)` は例外を外へ投げない契約を維持する。
- **受け入れ条件**:
  - audit log が 1 event = 1 logical line として維持される。
  - 値によって `key=value` 構造が壊れない。
  - masked value と unmasked value の両方で escape 済み出力をテストしている。

## R5: 検証方針の明文化

- **ステータス**: Done（README/AGENTS/testing に `test` と `verify` の使い分けを明記）
- **優先度**: Low
- **対象**:
  - `README.md`
  - `AGENTS.md`
  - `docs/agent/testing.md`
  - `docs/plans/00-libkoiki-batch-roadmap.md`
- **作業**:
  - `mvn clean test` を短時間のローカル標準確認として維持する。
  - 参照アプリ統合、Failsafe、起動経路、終了コード、DB-backed job、I/O lifecycle に触れる変更では、`mvn verify` を完了確認として定義する。
  - VS Code Extension Pack の Java/Maven 明示コマンドが有用であれば、引き続き記載する。
- **受け入れ条件**:
  - 開発者または AI エージェントが、`test` で十分な場合と `verify` が必要な場合を判断できる。
  - `*IT` クラスが `mvn test` で実行されるかのような誤解を招く記述がない。

## 最終確認

- **ステータス**: Done（`mvn verify` 成功）
- **コマンド**:

```powershell
$env:JAVA_HOME="$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\21"
& "$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\maven\latest\bin\mvn.cmd" verify
```

- **受け入れ条件**:
  - build が成功する。
  - 残りの `git diff` を確認する。
  - merge 前に、この task file のステータスを更新する。
