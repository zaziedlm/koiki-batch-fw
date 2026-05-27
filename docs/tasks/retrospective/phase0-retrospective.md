# Phase 0 実装・改修 振り返り

| 項目 | 値 |
| --- | --- |
| 対象バージョン | `v0.1.0`（`0.1.0-SNAPSHOT`） |
| 対象 | libkoiki-batch Phase 0（core / execution / fault）＋参照アプリ E2E 結線 |
| 基盤 | Java 21 / Spring Boot 4.0.6 / Spring Batch 6.0.3 |
| ブランチ | `topic/koiki-batch-impl` |
| 関連 | [ロードマップ](../../plans/00-libkoiki-batch-roadmap.md) / [decision-log](../../batch/decision-log.md) / [testing](../../agent/testing.md) |

この文書は Phase 0 の実装〜改修の流れを点検・記録し、今後の作業設計の参考にするためのもの。Git 履歴（`9ac9e96..topic/koiki-batch-impl` の 5 コミット）を事実の軸にしている。

## 1. タイムライン

| # | commit | 内容 | 規模 |
| --- | --- | --- | --- |
| 1 | `3bc7b1d` docs: plans/tasks | 計画・タスク文書 7 本 | +470 |
| 2 | `737beed` feat: Core 実装 | core/execution/fault 実装＋ユニット | 21 files / +715 |
| 3 | `3dd3906` feat(ref-app): E2E 結線 | 参照アプリ結線＋IT | 7 files / +267 |
| 4 | `c650b98` feat: 点検フォローアップ | 3 つの穴埋め＋テスト | 11 files / +243 |
| 5 | `6f79e3e` docs: 点検結果記録 | decision-log 等 | 6 files / +39 |

特徴は、コミット 2→3→4 と進むにつれ **テスト・実行・結線から得た気づきが次のコミットを生んでいる**こと。

## 2.「気づき → 対応」の連鎖

### フェーズ0: 計画
- 気づき（人によるレビュー）: 初版計画は Spring Batch 6 仕様の裏取りが甘く、一度差し戻し。
- 対応: 公式移行ガイド＋実 JAR で API を確定してから計画確定。「推測で書かない」を以降の土台に。

### フェーズ1: Core 実装（`737beed`）— 実バイナリと IDE 診断からの気づき
- SB6 パッケージ再編を実 JAR で確定: listener=`...core.listener` / params=`...core.job.parameters` / explore=`...core.repository.explore`。
- **Boot 4 で `BatchAutoConfiguration` が別モジュール** `org.springframework.boot.batch.autoconfigure` へ移動（旧 `...autoconfigure.batch`）。推測では外していた箇所。
- **IDE 警告で `JobExplorer` 非推奨を検知** → `JobOperator` で実装 → さらに `JobOperator.getRunningExecutions` も非推奨と判明 → **`JobRepository.findRunningJobExecutions` に着地**。
- 検証例外は SB6 で `InvalidJobParametersException`（旧名から改名）と確定。
- ユニット 23 件で確定。

### フェーズ2: E2E 結線（`3dd3906`）— 「アプリとして動かす」視点で噴出
1. 参照アプリに **`main` が無く起動不能** → 終了コード伝播付き `main` を追加。
2. DB レス構成で **`PlatformTransactionManager` bean が不在** → 参照アプリに `ResourcelessTransactionManager` を明示（フレームワークは暗黙提供しない判断）。
3. **IDE 警告で `JobLauncherTestUtils` 非推奨**を検知 → IT を `JobOperator.start` で記述。
4. **`*IT` が surefire で実行されない**ことを「テストが 0.387 秒で“成功”」した観察から発見。
5. 終了コード generator が **Boot の `JobExecutionExitCodeGenerator` と競合しない**ことを javap で確認（自動登録されない）。

### フェーズ3: 点検フォローアップ（`c650b98`）— テスト実行が暴いた設計の穴
1. **`*IT` 未実行** → 親 pom に maven-failsafe を追加し `mvn verify` で実行。
2. **パラメータ検証失敗が `0/10/20/30` を経由しない**（JobExecution 生成前 throw で `JobExecutionEvent` が出ず Boot 既定 exit 1）→ `KoikiExitCodeExceptionMapper` を追加。
3. **ガードを `beforeJob` で呼ぶと自実行を多重起動と誤検知** → API を `acquire(String)` から **`canRun(JobExecution)`（自分除外）**へ精緻化し、`ConcurrencyGuardJobListener` で実効化。
- ユニット 31 件＋IT 6 件で確定。

### フェーズ4: 記録（`6f79e3e`）
- 恒久的設計判断を [decision-log](../../batch/decision-log.md) に、運用知識（`mvn test` vs `verify`）を [testing](../../agent/testing.md) に集約。

## 3. 気づきの「源泉」分類（今後への最重要示唆）

| 源泉 | 見つかったもの | 教訓 |
| --- | --- | --- |
| 計画レビュー（人） | SB6 裏取り不足 | 計画段階で実仕様確認を要件化 |
| 実 JAR / javap | パッケージ移動・改名・正しい API | 推測せず実バイナリで確定（最も多く誤りを未然に防いだ） |
| IDE 診断（deprecation） | `JobExplorer` / `JobOperator.getRunningExecutions` / `JobLauncherTestUtils` | 警告を逐次拾い非推奨を即回避 |
| コンパイラ | import / シグネチャ齟齬 | 小さく作って都度コンパイル |
| テスト“実行”の観察 | `*IT` 未実行（実行時間で発覚）、検証の例外型 | 「速すぎる成功」は未実行を疑う |
| E2E 結線（アプリ視点） | `main` 不在 / TM 不在 / exit code の穴 / ガード自己誤検知 | ユニットでは出ない穴が E2E で出る |

## 4. プロセス上の教訓（今後の作業設計へ）

1. **「提供しただけ」と「効いている」は別物**。bean 登録だけでは動かず、実行時の結線（opt-in listener / `.validator()`）が要る。新機能は「誰が・いつ呼ぶか」をセットで設計する。
2. **フレームワークの暗黙デフォルトは安全側で慎重に**。TM フォールバックは整合性リスクのため、あえて提供しない（fail-fast 優先）。
3. **起動前例外（検証）は通常のジョブ失敗経路と別系統**。終了コード機構を 2 系統（generator / exception mapper）持つ必要があった。
4. **テスト命名規約（`*IT`）と実行フェーズの結びつき**を最初に固める（今回は後から failsafe を追加）。
5. **裏取り → 小さく実装 → 即コンパイル/テスト**の短いループが、変化の大きい SB6 / Boot4 で特に有効。
6. **記録の置き場の使い分け**: 恒久判断=decision-log、運用知識=agent docs、設計=plans、実行可能単位=tasks。

## 5. 現時点で開いている事項（次への引き継ぎ）

- **FAULT-05 スケジューラ経路**（`CommandLineJobOperator` ＋ `ExitCodeMapper`）は設計のみ・実装 deferred。
- **ガード強制は opt-in**（全ジョブ自動適用ではない）。全ジョブ強制が要るなら別途検討。
- **終了コードの細部**: 検証失敗→20 の妥当性、警告(10) の生成経路は未定義。
- **リモート未 push**。
- **Phase 1 以降**（observability / audit / security / transaction / validation / io / support）は未着手。
