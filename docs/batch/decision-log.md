# Batch Decision Log

## 2026-05-21: Development Version Baseline

Decision: use `v0.1.0` as the current KOIKI Batch Framework development line.

Reason: the project has reached an initial framework baseline with Maven modules, dependency baseline, package boundaries, and agent-facing guidance.

Impact: Maven artifacts remain on `0.1.0-SNAPSHOT` while this line is under active development. A non-SNAPSHOT release can be introduced when the first usable batch core and reference job are ready.

## 2026-05-21: Package Root

Decision: use `org.koikifw.*` as the official Java package root.

Reason: `koikifw.org` is the owned domain, and Java package naming should follow the owned reverse domain.

Impact: framework and reference packages under `org.koikifw.*` are considered canonical.

## 2026-05-21: Framework Capability Boundaries

Decision: define shared batch framework responsibilities under `org.koikifw.libkoiki.batch.*`, with dedicated packages for execution, fault handling, I/O, observability, audit, security, transaction, validation, and support.

Reason: enterprise batch applications need clear separation between operations, audit, transaction control, and business logic. Creating the package map early prevents common framework behavior from leaking into customer applications.

Impact: initial implementations may be small, but new shared features should be placed according to the package responsibility map in `platform-capabilities.md`.

## 2026-05-21: Dependency Baseline

Decision: use Java 21, Spring Boot 4.0.x, and Spring Batch 6.0.x as the current development baseline.

Reason: the project is at an early stage and can adopt the current stable major line before application code accumulates.

Impact: code must follow Spring Batch 6 package names and APIs.

## 2026-05-27: Phase 0 E2E Inspection Decisions

These decisions arose from wiring the Phase 0 framework into the reference app end-to-end on Spring Batch 6.0.3 / Spring Boot 4.0.6.

Decision: framework does not provide a fallback `PlatformTransactionManager`.

Reason: Spring Batch 6 uses a resourceless transaction manager internally but does not expose a `PlatformTransactionManager` bean. A silent framework fallback would let an application that mis-wires its datasource-backed manager run non-transactional steps without noticing — a data-integrity risk.

Impact: DB-less batch apps must declare a `ResourcelessTransactionManager` explicitly (the reference app does so in `RefBatchInfrastructureConfig`). DB-backed apps use Boot's datasource transaction manager.

Decision: map job parameter validation failures (and other startup exceptions) onto the `0/10/20/30` return codes via a Spring Boot `ExitCodeExceptionMapper`.

Reason: validation throws before a `JobExecution` exists, so no `JobExecutionEvent` is published and the exit-code generator never sees it; Spring Boot would otherwise report its default exit code `1`, outside the JP1 contract. Parameter errors are input errors, so they map to business error `20`; unclassified startup failures default to system error `30`.

Impact: `org.koikifw.libkoiki.batch.fault.KoikiExitCodeExceptionMapper` is auto-configured alongside the exit-code generator.

Decision: the concurrency guard is enforced through an opt-in `JobExecutionListener`, and the guard contract checks for *other* running executions (excluding the current one).

Reason: a guard invoked in `beforeJob` runs after the current `JobExecution` is already counted as running, so it must exclude itself. Enforcement is opt-in (a job registers the listener) to match how the parameter validator is applied. A concurrent launch is refused by failing the job with a `SystemException`, which maps to exit code `30`.

Impact: `ConcurrencyGuardService` exposes `canRun(JobExecution)`; `ConcurrencyGuardJobListener` enforces it. Jobs opt in via `JobBuilder.listener(...)`.

Decision: integration tests (`*IT`) run under the Maven Failsafe plugin via `mvn verify`; `mvn clean test` remains unit-only (Surefire).

Reason: Surefire does not execute `*IT` classes, so the reference-app integration tests (including the pre-existing placeholder) never ran. Binding Failsafe makes integration tests first-class without changing the unit-test command.

Impact: the default quick check stays `mvn clean test`; full verification including job/exit-code integration tests is `mvn verify`.

## 2026-05-30: Phase 3 Masking Boundary

Decision: provide masking through two cooperating but separate hooks — value-level masking for audit attributes (a `Masker` SPI applied by `LoggingAuditEventPublisher` to attribute values whose key is in a configured sensitive-key set) and text-level masking for application logs (a Logback `MaskingPatternConverter` that redacts regex matches).

Reason: the two outputs have different shapes. Audit attributes are structured `key=value` pairs, so a key-based decision is precise and DI-friendly. Free-form log messages have no keys, so masking there must match text patterns, and Logback converters are created by Logback (not Spring), so they cannot receive an injected `Masker`. Forcing both through one mechanism would require a brittle Spring-to-Logback bridge. Standard personal-data masking rules are deferred, so the framework ships the mechanism and applications supply the sensitive keys / regex patterns.

Impact: `org.koikifw.libkoiki.batch.security` exposes `Masker` + `RedactingMasker` (full redaction to `***`) and `MaskingPatternConverter`. Audit value masking is logging-backend neutral (SLF4J only); only `MaskingPatternConverter` needs Logback, which is therefore an `optional` dependency of `libkoiki-batch` so log4j2 applications are not forced onto Logback. Configuration is under `koiki.batch.security.masking.{enabled,mask,sensitive-keys}`; applications register the converter via their own `logback-spring.xml` `<conversionRule converterClass=...>` (properties-only converter registration is not relied upon).

## 2026-05-30: DB-backed Batch Management Posture

Context: Phase 4 review clarified that the earlier "no fallback transaction manager" decision only declines to force a *metadata* persistence choice; business RDBMS access, connections, chunk management, and transaction management are core capabilities that need an explicit design. The full rationale and diagrams live in [db-management-architecture.md](db-management-architecture.md).

Decision: keep `ResourcelessJobRepository` as the framework default (no DB required) and treat the JDBC `JobRepository` as an **explicit application opt-in**. Verified against Boot 4.0.6 / Spring Batch 6.0.3: a `DataSource` alone does **not** switch to JDBC persistence (Boot's batch config extends `DefaultBatchConfiguration`, whose default `jobRepository()` is resourceless). An app that wants restart-safe metadata adds `@EnableBatchProcessing @EnableJdbcJobRepository(dataSourceRef="dataSource", transactionManagerRef="transactionManager")`, reusing Boot's auto-configured beans. For DB-backed jobs the standard topology is a **single shared `DataSource` with one transaction manager**, so each chunk commit writes business rows and step-execution metadata atomically. Schema is owned by **Flyway** (`spring.batch.jdbc.initialize-schema=never`), including the `BATCH_*` metadata tables.

Reason: the metadata transaction and the chunk transaction are separate by default; sharing one `DataSource`/manager makes them atomic and gives the strongest restart guarantee. The Boot-3 assumption that a `DataSource` auto-enables JDBC persistence is false on Boot 4 / Spring Batch 6, so the opt-in is explicit. `@EnableBatchProcessing` makes Boot's default batch config back off, but its job launcher and exit-code generator (`BatchJobLauncherAutoConfiguration`) stay active, so the framework's `0/10/20/30` mapping is preserved (confirmed by the reference exit-code IT). Flyway as the single schema source avoids Flyway-vs-Boot initializer ordering conflicts and matches enterprise practice (DBAs manage migrations; auto-init disabled in production).

Impact: store selection stays an application concern — the framework module never declares `@EnableBatchProcessing` / `@EnableJdbcJobRepository` or a `JobRepository`. The `transaction` package contributes a `koiki.batch.transaction.defaultCommitInterval` property and commit-boundary/rollback guidance but **no transaction-manager bean** (the no-fallback-TM decision continues). Reader/writer adapter contracts (`io`) remain Phase 5; DB-backed jobs use stock Spring Batch readers/writers until then. Separate-`DataSource` (`@BatchDataSource`) topology and production dialects (Oracle/PostgreSQL) are deferred; tests use H2.
