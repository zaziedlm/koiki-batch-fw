# Agent 作業開始準備・参照ジョブ追加 Skill 整備タスク

| 項目 | 値 |
| --- | --- |
| 計画 | [95-agent-readiness-reference-job-skill.md](../plans/95-agent-readiness-reference-job-skill.md) |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| 検証 | Markdown/参照リンク確認、Skill 形式検証。必要に応じて `mvn verify` |
| ステータス | Done |

## A1: `docs/agent/README.md` の現状反映

- **ステータス**: Done（Phase 0-5、3参照ジョブ、テスト階層、deferred 項目を現状反映）
- **優先度**: High
- **対象**:
  - `docs/agent/README.md`
- **作業**:
  - `Reference application skeleton` を、実装済み3参照ジョブの説明へ置き換える。
  - batch core が未実装という記述を削除する。
  - Phase 0-5 の初期スコープが実装済みであることを記載する。
  - unit test と `*IT` integration test の存在を記載する。
  - `mvn clean test` と `mvn verify` の使い分けを要約する。
  - deferred 項目を実装済み機能と分離して記載する。
  - Skill を整備できる現在段階へ説明を更新する。
- **受け入れ条件**:
  - `docs/agent/README.md` が `README.md` とロードマップの現在状態に一致する。
  - logging、audit、transaction、fault、validation、I/O を一律に未実装扱いしていない。
  - Agent が参照ジョブと検証コマンドを文書から発見できる。

## A2: 参照ジョブ追加パターンの抽出

- **ステータス**: Done（tasklet / DB chunk / file chunk の選択条件と opt-in 機能を整理）
- **優先度**: High
- **対象**:
  - `components/koiki_ref_batch_app/src/main/java/org/koikifw/refapp/batch/jobs/customer/CustomerDailySyncJobConfig.java`
  - `components/koiki_ref_batch_app/src/main/java/org/koikifw/refapp/batch/jobs/customer/CustomerImportJobConfig.java`
  - `components/koiki_ref_batch_app/src/main/java/org/koikifw/refapp/batch/jobs/billing/BillingFileJobConfig.java`
  - 対応する validator、processor、model、resource、`*IT`
  - `docs/agent/skills/add-reference-batch-job/references/reference-job-patterns.md`
- **作業**:
  - tasklet、DB chunk、file chunk の共通点と相違点を整理する。
  - 標準パラメータ、listener、transaction、validation、I/O lifecycle の適用条件を整理する。
  - Skill 本文に必要な判断だけを抽出し、コード詳細の重複説明は避ける。
- **受け入れ条件**:
  - Skill が3パターンのどれを参照すべきか判断できる。
  - すべての参照ジョブへ同じ listener や I/O 機能を機械的に追加する指示になっていない。

## A3: `add-reference-batch-job` Skill の作成

- **ステータス**: Done（frontmatter、実装手順、境界、テスト・文書更新・完了確認を定義）
- **優先度**: High
- **対象**:
  - `docs/agent/skills/add-reference-batch-job/SKILL.md`
- **作業**:
  - `name` と `description` のみを持つ YAML frontmatter を追加する。
  - description に、参照ジョブの追加・拡張時に利用する Skill であることを明記する。
  - 必須読書、パターン選択、配置、実装、テスト、文書更新、検証の順で手順を書く。
  - `AGENTS.md`、`docs/agent/boundaries.md`、`docs/agent/testing.md`、既存参照ジョブへリンクする。
  - 顧客アプリや framework 機能追加を対象外として明記する。
  - Skill 本文は簡潔に保ち、必要なら既存リポジトリ文書を直接参照する。
- **受け入れ条件**:
  - Skill ディレクトリ名と frontmatter の `name` が `add-reference-batch-job` で一致する。
  - frontmatter の `description` だけで利用場面を判断できる。
  - Job/Step と Reader/Processor/Writer の責務分離を維持する。
  - standard parameters、exit code、transaction、audit、masking、I/O lifecycle を必要性に応じて判断する。
  - unit test または `*IT`、README 更新、検証コマンドまで手順に含まれる。

## A4: Agent 文書から Skill への導線追加

- **ステータス**: Done（共有 Skill 一覧、用途・対象外、自動発見との違いを明記）
- **優先度**: Medium
- **対象**:
  - `docs/agent/README.md`
  - 必要に応じて `docs/agent/documentation-plan.md`
- **作業**:
  - 利用可能な共有 Skill 一覧を追加する。
  - `add-reference-batch-job` の用途と対象外を短く記載する。
  - `docs/agent/skills` は共有正本であり、各 AI の自動発見設定とは別であることを記載する。
  - `documentation-plan.md` の「Skills は後日」という旧判断を、実装パターンが成立した現在状態に合わせる。
- **受け入れ条件**:
  - Agent が `docs/agent/README.md` から Skill を発見できる。
  - ツール固有ファイルへルールを重複記載する方針になっていない。

## A5: Skill 形式・内容の検証

- **ステータス**: Done（形式、リンク、旧参照、想定 DB chunk 依頼を検証）
- **優先度**: Medium
- **対象**:
  - `docs/agent/skills/add-reference-batch-job/`
- **作業**:
  - `skill-creator` の `quick_validate.py` が利用可能なら実行する。
  - frontmatter、命名、参照リンクを確認する。
  - 次の想定依頼で手順を机上確認する。

```text
components/koiki_ref_batch_app に、取引データをDBから読み、
検証して別テーブルへ登録する参照ジョブを追加してください。
```

  - 上記依頼に対して、DB chunk パターン、配置、validation、transaction、`*IT`、README 更新、`mvn verify` を選択できることを確認する。
- **受け入れ条件**:
  - Skill の形式検証が成功するか、手動確認結果が記録されている。
  - 存在しないファイルや古い package を参照していない。
  - Skill 単体で重い framework 抽象化を追加する方向へ誘導しない。

### 検証結果

- `skill-creator/scripts/quick_validate.py` は利用可能だったが、ローカルの `python` が実体のない WindowsApps alias のため起動できなかった。
- 同スクリプトの検査条件を PowerShell で再現し、以下を確認した。
  - frontmatter は先頭にあり、終了区切りは4行目
  - `name` / `description` は各1件で、余分な frontmatter key はない
  - Skill 名 `add-reference-batch-job` は小文字 kebab-case、23文字、ディレクトリ名と一致
  - description は236文字で、禁止文字 `<` / `>` を含まず、1024文字以内
- `SKILL.md` 内の4つの Markdown link は、すべて実在するファイルへ解決できた。
- `com.koiki.*`、`org.koiki.*`、旧 Spring Batch API などの古い参照は検出されなかった。
- 想定依頼「取引データをDBから読み、検証して別テーブルへ登録する参照ジョブ」では、以下を選択できることを確認した。
  - `customer-import` を基準とする DB chunk パターン
  - `org.koikifw.refapp.batch.*` 配下への配置
  - Spring Batch 標準 JDBC reader/writer
  - `Validator<T>` と processor の分離
  - 明示的な commit interval と transaction manager
  - Resourceless / JDBC JobRepository の要件による選択
  - 正常 commit、validation failure rollback、必要に応じた metadata 永続化の `*IT`
  - README の参照ジョブ一覧更新
  - 完了確認として `mvn verify`

## A6: 最終確認

- **ステータス**: Done（旧記述、相互リンク、差分範囲、全タスク完了を確認）
- **優先度**: Medium
- **作業**:
  - `rg` で skeleton・未実装に関する旧記述を確認する。
  - 変更した Markdown の相互リンクを確認する。
  - `git diff` で計画外の変更がないことを確認する。
  - 文書のみの変更なら Maven 実行は原則省略し、その理由を記録する。
  - build/test コマンドやコード参照を変更した場合は `mvn verify` を実行する。
- **受け入れ条件**:
  - A1-A5 が完了している。
  - task のステータスが実績に合わせて更新されている。
  - 残る複数 AI ツール固有導線が後続課題として明記されている。

### 最終確認結果

- A1-A5 はすべて `Done` である。
- `docs/agent/README.md` と `docs/agent/documentation-plan.md` から、参照アプリや batch core を未実装とする旧記述が解消されている。
- 今回作成・更新した Markdown から参照する相対リンクは、すべて実在するファイルへ解決できた。
- `git diff --check` は問題なし。
- 変更範囲は Agent 文書、共有 Skill、plan/task 文書に限定されている。
- 未追跡の `.claude/` は本計画より前から存在する個人環境設定であり、今回の対象外として変更していない。
- Claude Code、Codex、Kiro、GitHub Copilot 固有の自動発見 adapter は後続課題として残している。
- Java、POM、Maven 設定、build/test コマンドは変更していないため、`mvn verify` は省略した。
