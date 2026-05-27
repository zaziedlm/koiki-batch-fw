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
