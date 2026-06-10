# Agent 作業開始準備・参照ジョブ追加 Skill 整備プラン

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| 対象範囲 | Agent 文書の現状反映、参照ジョブ追加 Skill の初期実装 |
| ステータス | Done |
| タスク一覧 | [95-agent-readiness-reference-job-skill.md](../tasks/95-agent-readiness-reference-job-skill.md) |

## 目的

本プランは、AI コーディングエージェントが古いプロジェクト状態を前提に作業するリスクを下げ、既存の実装パターンに沿って参照ジョブを追加できる状態を作るための計画である。

現在の `docs/agent/README.md` には、参照アプリや共通機能を skeleton または未実装として扱う記述が残っている。一方、実際には `libkoiki-batch` の Phase 0-5、3 種類の参照ジョブ、単体テスト、統合テストが実装済みである。

また、`docs/agent/skills` 配下には Skill の種があるが、現状の `SKILL.md` は説明が一行のみで、AI が反復可能な作業手順として利用できる状態ではない。

この差を解消し、次の開発作業に入る前に Agent 向けの入口を現在のコードと一致させる。

## 対応テーマ

| テーマ | 優先度 | 概要 |
| --- | --- | --- |
| A1 | High | `docs/agent/README.md` を `v0.1.0` の実装状態に合わせる。 |
| A2 | High | 最初の実用 Skill として `add-reference-batch-job` を作成する。 |
| A3 | Medium | Skill の検証方法と複数 AI ツールにおける位置づけを明文化する。 |

## A1: Agent 文書の現状反映

`docs/agent/README.md` を Agent 向けの信頼できる入口として更新する。

更新後は、少なくとも以下を正しく説明する。

- `libkoiki-batch` の Phase 0-5 初期スコープは実装済みである
- 参照アプリには `customer-daily-sync`、`customer-import`、`billing-file` がある
- unit test と `*IT` integration test が存在する
- `mvn clean test` と `mvn verify` の使い分け
- 顧客アプリは `libkoiki-batch` に依存し、参照アプリ実装には依存しない
- 分散ロック、監査永続化、本番 DB 方言などは deferred である
- 実装済みパターンに対しては、軽量な Agent Skills を整備できる段階に入った

文書内容は `README.md`、`AGENTS.md`、`docs/agent/boundaries.md`、`docs/agent/testing.md`、`docs/plans/00-libkoiki-batch-roadmap.md` と矛盾させない。

## A2: 参照ジョブ追加 Skill

Skill 名は、動作を表す小文字 kebab-case として以下を採用する。

```text
add-reference-batch-job
```

正本は以下に配置する。

```text
docs/agent/skills/add-reference-batch-job/SKILL.md
```

### Skill の対象

この Skill は、`components/koiki_ref_batch_app` に新しい参照ジョブを追加・拡張するときに使用する。

対象例:

- 新しい tasklet 型参照ジョブを追加する
- DB chunk 型参照ジョブを追加する
- file-to-file chunk 型参照ジョブを追加する
- 既存フレームワーク機能の利用例を参照ジョブで実証する

対象外:

- 顧客固有ジョブを `apps/*` に実装する作業
- `libkoiki-batch` に新しい横断機能を設計する作業
- JP1 launcher や本番運用設計
- 実ジョブ要求のない重い抽象化の追加

### Skill の必須内容

`SKILL.md` は有効な YAML frontmatter を持つ。

```yaml
---
name: add-reference-batch-job
description: ...
---
```

本文には、次の作業手順を簡潔に含める。

1. `AGENTS.md`、境界文書、テスト方針、既存参照ジョブを読む
2. tasklet / DB chunk / file chunk のどのパターンに近いかを判断する
3. `org.koikifw.refapp.batch.*` 配下へ責務別に配置する
4. 標準パラメータ、ログ、監査、検証、トランザクション、I/O lifecycle の必要性を判断する
5. Job/Step/Reader/Processor/Writer の責務を明示する
6. unit test または `*IT` を追加する
7. README の参照ジョブ一覧など、影響する文書を更新する
8. 変更範囲に応じて `mvn clean test` または `mvn verify` を実行する

Skill は実装コードを固定テンプレートとして大量複製させず、既存の3ジョブから最も近いパターンを選ばせる。詳細な設計情報は重複記載せず、リポジトリ内文書と既存コードを参照させる。

### Skill が守る境界

- 参照ジョブは `components/koiki_ref_batch_app` に置く
- 業務固有すぎる内容や実顧客情報を含めない
- 再利用可能な横断機能が必要になっても、同時に `libkoiki-batch` へ昇格させず、境界を評価する
- transaction、retry/skip、exit code を汎用 utility に隠さない
- 標準ジョブパラメータは `job.name`、`job.bizDate`、`job.requestId` を基本とする
- テストデータは架空かつ決定的な値を使う
- `target/` を成果物としてコミットしない

## A3: Skill の検証と複数 AI ツールへの位置づけ

Skill 作成後は、少なくとも以下を確認する。

- frontmatter の `name` と `description` が存在する
- Skill 名とディレクトリ名が一致する
- 本文から参照するファイルが存在する
- Skill の手順が既存の3参照ジョブと矛盾しない
- 「新しい tasklet 参照ジョブを追加する」という想定依頼に対して、配置・テスト・文書更新・検証まで判断できる
- `mvn verify` を完了条件として適切に選択できる

可能であれば `skill-creator` の `quick_validate.py` を使って形式検証する。リポジトリ外のツール依存で実行できない場合は、frontmatter と参照リンクを手動確認し、その旨を記録する。

`docs/agent/skills` はツール中立な共有正本とする。ただし、Codex、Claude Code、Kiro、GitHub Copilot では Skill の自動発見方式が異なるため、各ツール固有の配置・導線は本プランでは実装しない。後続作業では、共通 Skill を複製せず参照する薄い adapter/instruction を検討する。

## 対応順序

1. `docs/agent/README.md` を現在の実装状態に合わせる。
2. 参照ジョブ追加作業の共通パターンを既存3ジョブから抽出する。
3. `add-reference-batch-job/SKILL.md` を作成する。
4. Skill の形式、参照先、境界、検証判断を確認する。
5. Agent 文書から新しい Skill への導線を追加する。
6. 文書のみの変更として参照リンクと Git 差分を確認する。

## スコープ外

- 新しい Java バッチジョブの実装
- `libkoiki-batch` の機能追加
- 既存の `koiki-batch-overview` / `koiki-batch-ops-jp1` Skill の全面改修
- Claude Code、Codex、Kiro、GitHub Copilot 固有設定の追加
- CI による Skill 検証の自動化
- JP1 launcher の実装

## 完了条件

- `docs/agent/README.md` が Phase 0-5 と3参照ジョブの実装状態を正しく説明している
- skeleton、未実装という誤った説明が解消されている
- `add-reference-batch-job/SKILL.md` に有効な frontmatter と反復可能な作業手順がある
- Skill が参照アプリ、共通基盤、顧客アプリの境界を誤らない
- Skill が必要なテスト・文書更新・検証コマンドを判断できる
- Agent 文書から Skill の存在と用途を確認できる
- 参照リンクと Markdown の整合性が確認されている
