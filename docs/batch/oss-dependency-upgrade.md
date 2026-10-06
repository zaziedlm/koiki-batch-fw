# OSS 依存関係の更新と検証（2026-10-06）

Maven Central の各 artifact の `maven-metadata.xml` を参照し、公開済みの最新安定版へ更新した。Milestone、RC、SNAPSHOT は採用しない。

## 更新方針

- Spring Boot parent を 4.0.6 から 4.1.1 へ更新し、Spring Framework / Spring Batch / Micrometer などの整合は Boot BOM で管理する。
- BOM の管理版より新しい安定版がある使用中のライブラリは、親 POM の Boot バージョンプロパティで上書きする。同じライブラリ群の core / bridge / agent / test / engine を個別に固定しない。
- Boot BOM が管理しない推移的テスト依存の Objenesis と ASM は、親 POM の `dependencyManagement` で揃える。
- Java は既存の 21 を継続し、全3モジュールで同じ依存管理を継承する。
- Maven Wrapper のスクリプト版 3.3.4 は最新のため継続し、Maven 配布物を 3.9.16 から 3.10.0 へ更新する。

## 主な解決バージョン

| OSS | 更新前 | 更新後 |
| --- | --- | --- |
| Spring Boot | 4.0.6 | 4.1.1 |
| Spring Framework | 7.0.7 | 7.0.9 |
| Spring Batch | 6.0.3 | 6.0.5 |
| Spring Retry | 2.0.12 | 2.0.13 |
| Micrometer | 1.16.5 | 1.17.1 |
| Flyway | 11.14.1 | 13.9.0 |
| H2 | 2.4.240 | 2.5.252 |
| HikariCP | 7.0.2 | 7.1.0 |
| Logback | 1.5.32 | 1.6.5 |
| SLF4J | 2.0.17 | 2.0.20 |
| Log4j API / SLF4J bridge | 2.25.4 | 2.26.1 |
| SnakeYAML | 2.5 | 2.7 |
| Jackson annotations | 2.21 | 2.22（Jackson 2 BOM 2.22.3） |
| Commons Logging | 1.3.6 | 1.4.0 |
| JSONPath | 2.10.0 | 3.0.0 |
| JUnit Jupiter / Platform | 6.0.3 | 6.1.3 |
| Mockito | 5.20.0 | 5.24.0 |
| Byte Buddy / agent | 1.17.8 | 1.18.14 |
| Objenesis | 3.3 | 3.6 |
| ASM | 9.7.1 | 9.10.1 |
| XMLUnit | 2.10.4 | 2.14.0 |
| Jakarta XML Bind API | 4.0.4 | 4.0.5 |
| JSpecify | 1.0.0 | 1.0.1 |
| Maven | 3.9.16 | 3.10.0 |
| Maven Compiler Plugin | 3.14.1 | 3.16.0 |
| Maven Surefire / Failsafe Plugin | 3.5.5 | 3.6.0 |
| Maven Dependency Plugin | 3.9.0 | 3.11.0 |
| Maven Resources Plugin | 3.3.1 | 3.5.0 |
| Maven Jar Plugin | 3.4.2 | 3.5.1 |
| Maven Install / Deploy Plugin | 3.1.4 | 3.2.0 |

更新後の依存ツリーに含まれる外部 artifact は70件。各 artifact の安定版メタデータとの比較で更新漏れはなく、同じ artifact のモジュール間バージョン差もない。Flyway の更新で Jackson 2 core / databind は依存ツリーから外れ、現在は annotations のみ解決される。

## 検証結果

専用 worktree の `chore/upgrade-oss-20261006` ブランチで、更新前と最終更新後に clean build を実施した。Java ソースと既存テストの変更は不要だった。

| 検証 | 結果 |
| --- | --- |
| `libkoiki-batch` Surefire | 117件成功 |
| `koiki-ref-batch-app` Failsafe | 20件成功 |
| `customer-a-batch-app` Failsafe | 1件成功 |
| 全体 | 138件、失敗0、エラー0、スキップ0 |
| Maven Enforcer `dependencyConvergence` | 親 POM と全3モジュールで成功 |
| 本番クラスの bytecode | 全て Java 21（major version 65） |

既存の統合テストは、Boot の自動構成、起動と終了コード、監査・MDC・マスキング、Flyway migration、JDBC JobRepository と chunk rollback、ファイルの文字コード・archive/error 移動・atomic output を含む。

通常の開発環境では、フル JDK 21 で以下を実行する。

```bash
./mvnw -B -ntp clean verify
./mvnw -B -ntp org.apache.maven.plugins:maven-enforcer-plugin:3.6.3:enforce \
  -Denforcer.rules=dependencyConvergence
```

今回のクラウド環境の Debian Java 21.0.12.1 には `jdk.compiler` がある一方、フル JDK の `lib/ct.sym` がなく、通常の `--release 21` は更新前から `release version 21 not supported` で失敗した。この環境では検証コマンドに限り以下を追加し、Java 21 のコンパイラとランタイムで検証した。POM の `java.version=21` は維持している。

```bash
./mvnw -B -ntp -Dmaven.compiler.release= \
  -Dmaven.compiler.source=21 -Dmaven.compiler.target=21 clean verify
```

実行時には、環境専用の `JAVA_HOME`、`MAVEN_USER_HOME`、`-s` で Maven Central 向け proxy とローカルキャッシュも指定した。それらの設定はリポジトリには含めない。フル JDK による通常の `--release 21` のビルドはこの環境では未検証であり、空の release プロパティにより Maven Compiler の package-info 補完警告も出るが、コンパイルとテストは成功した。

Flyway は H2 2.5.252 を自身の検証済み版より新しいとして警告する。H2 上の migration / 正常 commit / rollback の既存テストは成功している。本番 DB 方言の検証は既存どおり対象外。
