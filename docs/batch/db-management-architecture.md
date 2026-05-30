# DB Management Architecture (Spring Batch 6 / Spring Boot 4)

This document defines the **target ("should-be") architecture for database management**
in KOIKI Batch Framework: how batch metadata, business data, transactions, and schema
are owned and wired. It is the reference for Phase 4 (transaction / validation) and for
any DB-backed job in the framework, reference app, and customer apps.

Status: agreed baseline for `v0.1.0`. Grounded in Spring Batch 6.0.x and Spring Boot 4.0.x.

> Japanese companion for business/operations teams: [db-management-architecture.ja.md](db-management-architecture.ja.md). Keep the two in sync.

## Background

The earlier decision "the framework does not provide a fallback `PlatformTransactionManager`"
([decision-log](decision-log.md), 2026-05-27) only means the framework does not force a
**metadata** persistence choice on applications. It does **not** mean DB access is out of
scope. Business RDBMS access, connections, chunk management, and transaction management are
**core batch capabilities** and must be designed with clear responsibility separation.

The project is currently DB-less end to end (no `DataSource`, no JDBC driver, no Flyway;
the reference app declares a `ResourcelessTransactionManager`). This document describes the
DB-backed model the framework moves toward.

## Two independent concerns

Spring Batch separates two persistence concerns. Keeping them distinct is the core of the
design.

| # | Concern | What it stores | Owner |
| --- | --- | --- | --- |
| 1 | **Batch metadata** (`JobRepository`) | Job / step execution state — the basis for **restart / rerun** | Framework auto-config + app `DataSource` wiring |
| 2 | **Business data processing** | Domain input/output via chunk read / process / write | Application (DAO/SQL); reader/writer **contracts** are framework `io` (Phase 5) |

A third cross-cutting concern, **transaction management**, binds them together (see below),
and a fourth, **schema management**, provisions both sets of tables (see Schema management).

## Metadata persistence: two modes

| Mode | Class | DB | Restartable | Concurrency | Use |
| --- | --- | --- | --- | --- | --- |
| Resourceless (default) | `ResourcelessJobRepository` | none | **No** | not thread-safe | dev, smoke, DB-less tasklets |
| JDBC (opt-in) | JDBC `JobRepository` (`BATCH_*` tables) | required | **Yes** | safe | real DB-backed jobs |

Decision for `v0.1.0`:

- **Resourceless stays the framework default** (non-breaking; matches the current code).
- **JDBC is an explicit opt-in.** Verified against Boot 4.0.6 / Spring Batch 6.0.3:
  **a `DataSource` alone does NOT switch to JDBC persistence.** Boot's
  `BatchAutoConfiguration` extends Spring Batch's `DefaultBatchConfiguration`, whose
  default `jobRepository()` is a `ResourcelessJobRepository`. To persist metadata the
  application explicitly enables the JDBC store. The reference app does so to demonstrate
  restart-safe metadata.

### Spring Boot idiom (verified)

To enable the JDBC `JobRepository` in a Spring Boot 4 app, add a small configuration that
opts in explicitly, reusing Boot's auto-configured `dataSource` and `transactionManager`:

```java
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(dataSourceRef = "dataSource", transactionManagerRef = "transactionManager")
class BatchJobRepositoryConfig {}
```

Notes grounded in the 4.0.6 / 6.0.3 jars and an end-to-end run:

- `@EnableBatchProcessing` makes Boot's `BatchAutoConfiguration.SpringBootBatchDefaultConfiguration`
  (which is `@ConditionalOnMissingBean(DefaultBatchConfiguration.class)`) back off, so the
  JDBC repository configured here is used instead of the resourceless default.
- Boot's **job launcher and exit-code generator remain active**
  (`BatchJobLauncherAutoConfiguration` is separate), so the framework's `0/10/20/30`
  exit-code mapping still works — confirmed by the reference app's exit-code integration test.
- `@EnableJdbcJobRepository` carries the store attributes (`tablePrefix`,
  `isolationLevelForCreate=SERIALIZABLE`, etc.); defaults reference beans named
  `dataSource` / `transactionManager`, which Boot provides.
- The framework's own `@AutoConfiguration(after = BatchAutoConfiguration.class)` stays
  additive (listeners/properties only) and never defines a `JobRepository` / `JobOperator`.

> Earlier drafts assumed Boot auto-configures the JDBC repository from a `DataSource` (true
> on Boot 3). That is **not** the case on Boot 4 / Spring Batch 6; the explicit opt-in above
> is required.

## Choosing the repository mode (operational guidance)

The repository mode is an **operational decision per executable** and a defining trait of
running on Spring Batch 6, where resourceless is the default. **Crucially, a resourceless
repository does not prevent business RDBMS processing**: a job can still open a business
`DataSource`, run chunk read/process/write, and commit/roll back business data through a
`DataSourceTransactionManager`. What "resourceless" drops is the *persistence of batch
metadata*, not business-data integrity.

| Capability | Resourceless (default) | JDBC (opt-in) |
| --- | --- | --- |
| Business RDBMS read/write + transactions | ✅ (business `DataSource` + TM) | ✅ |
| Chunk commit boundary, skip/retry | ✅ | ✅ |
| Exit codes `0/10/20/30` | ✅ (classifier-driven, repo-independent) | ✅ |
| Observability / audit / masking | ✅ | ✅ |
| **Restart from the point of failure** | ❌ rerun starts over | ✅ |
| **Cross-process duplicate-run / concurrency guard** | ⚠️ in-memory only, per process | ✅ |
| Execution history (`BATCH_*`) for ops/audit | ❌ | ✅ |
| Parallel / partitioned / multi-threaded steps | ❌ not thread-safe | ✅ |
| Atomic business + metadata commit | n/a (metadata not persisted) | ✅ (single shared `DataSource`) |

**Use resourceless when** the job is idempotent / safe to rerun from the start (full reloads,
stateless transforms), runs single-process and single-threaded, and the scheduler (JP1)
recovers by relaunching from the beginning. It avoids operating a metadata database.

**Use JDBC when** you need restart-from-failure, cross-process duplicate-run prevention (a
usable concurrency guard), execution history for operations/audit, or parallel/partitioned
steps.

Mixing is per process: one Spring context has exactly one `JobRepository`. To offer both,
separate the executables (or, as the reference app does, select the mode with a Spring
profile). To run a **resourceless business batch**, simply do *not* add the
`@EnableJdbcJobRepository` opt-in; still declare the business `DataSource` and pass its
transaction manager to the chunk step so business writes stay transactional.

The reference app demonstrates both: the default profile runs resourceless (with real H2
business tables), and the `jdbc-repository` profile enables the JDBC `JobRepository` for
restart-safe metadata.

## Transaction model

Two transaction scopes exist:

- **Repository transaction** — transactional advice around `JobRepository` methods persists
  execution metadata so state survives a crash (restart basis).
- **Chunk (business) transaction** — the `PlatformTransactionManager` passed to a
  chunk-oriented step commits item read/process/write at the **commit-interval** (chunk size).

These are *separate by default*. The relationship between them is decided by topology:

### Single shared `DataSource` (v0.1.0 standard)

Metadata and business data live in **one `DataSource` with one
`DataSourceTransactionManager`**. Each chunk commit then writes **business rows and the step
execution metadata in the same transaction** → atomic, restart-consistent (effectively
exactly-once per chunk).

```
              ┌─────────────── one DataSourceTransactionManager ───────────────┐
 read ──▶ process ──▶ write (business rows)  +  step execution update (BATCH_*) │
              └───────────────── single commit at commit-interval ─────────────┘
```

This is the recommended default: simplest correct behavior, strongest restart guarantee.

### Separate `DataSource` per concern (deferred guidance)

For multi-DB realism, annotate the metadata `DataSource` / manager with `@BatchDataSource`
/ `@BatchTransactionManager` (use `defaultCandidate = false` on the non-primary bean). The
metadata stays robust, but business writes and metadata updates are **no longer atomic**
(a crash between the two commits leaves a reconciliation window). Standard manager selection
for multi-database jobs remains **deferred** ([platform-capabilities](platform-capabilities.md)).

### Rollback / commit boundary policy

- Commit boundary is the **commit-interval** and must be **explicit** at the step. The
  framework exposes a default via `koiki.batch.transaction.defaultCommitInterval`; jobs
  reference it when building chunk steps. Transaction behavior is never hidden inside
  unrelated utilities ([AGENTS.md](../../AGENTS.md)).
- Spring Batch rolls back the chunk transaction on any unchecked exception by default.
  Business-controlled failures throw `BusinessException` (exit code 20); skip/retry policy
  is configured per step. Tasklet steps must document idempotency and rerun behavior.

## Schema management

Both business tables and `BATCH_*` metadata tables need provisioning.

Decision for `v0.1.0`: **Flyway is the single source of truth for schema**, and
`spring.batch.jdbc.initialize-schema=never`. The reference app ships the Spring Batch schema
(e.g. `schema-h2.sql` shipped by `spring-batch-core`) as a Flyway migration alongside its
business-table migrations. This avoids dual-mechanism ordering conflicts (Flyway vs Boot's
batch initializer) and matches enterprise practice where DBAs manage schema via migrations
and auto-initialization is disabled in production.

For purely embedded throwaway tests, Boot's `initialize-schema=embedded` could create
`BATCH_*` automatically, but the framework standardizes on Flyway for consistency between
test and production wiring.

## Production database

The reference app and tests use **H2 in in-memory mode** (`jdbc:h2:mem:...`,
`DB_CLOSE_DELAY=-1`): data lives only in the JVM heap and is **discarded when the process
exits**. This is for tests and demos only.

For production, deploy a **durable, production-grade RDBMS** (PostgreSQL, Oracle, SQL Server,
etc.). H2 in-memory loses all data on restart — fatal for business data, and for a JDBC
`JobRepository` it would defeat restart/history entirely. (H2 file mode is durable but is not
recommended for enterprise batch.) Business data needs a durable RDBMS even for a
**resourceless** job, which keeps no metadata but still reads/writes business tables.

Switching to production is an **application concern** (the framework module is unchanged):

1. Add the production JDBC driver (e.g. `org.postgresql:postgresql`, `com.oracle.database.jdbc:ojdbc11`).
2. Externalize the `DataSource` URL / credentials (environment variables / secrets — never
   hard-code them in `application.yml`).
3. **Replace the dialect-specific `BATCH_*` migration.** The reference migration
   `V002__create_batch_metadata.sql` is **H2-specific** (a verbatim copy of `schema-h2.sql`);
   production must use the matching `schema-postgresql.sql` / `schema-oracle10g.sql` (etc.)
   from `spring-batch-core`, and business-table DDL must be checked for dialect differences.
4. Keep `spring.batch.jdbc.initialize-schema=never` (Flyway remains the single schema source).

Production database dialects and their locking/behavioral differences are otherwise
**deferred** ([decision-log](decision-log.md)).

## Responsibility map (where each concern lives)

| Concern | Package / location |
| --- | --- |
| Batch infra auto-config integration | `org.koikifw.libkoiki.batch.core` (adds to Boot, never redefines) |
| Commit boundary / transaction policy | `org.koikifw.libkoiki.batch.transaction` (properties + guidance; **no TM bean**) |
| Restart / rerun / concurrency guard | `org.koikifw.libkoiki.batch.execution` |
| Reader / writer contracts | `org.koikifw.libkoiki.batch.io` (**Phase 5**; jobs use stock Spring Batch readers/writers until then) |
| Fault → exit code mapping | `org.koikifw.libkoiki.batch.fault` |
| `DataSource`, transaction manager bean, business DAO/SQL, Flyway migrations | **Application** (reference app / `apps/*`) |

## Anti-patterns

- Do not hide transaction, retry, or exit-code behavior inside generic utility classes.
- Do not provide a fallback `PlatformTransactionManager` from the framework; a misconfigured
  app must fail fast rather than silently run non-transactional steps (decision continued).
- Do not put `@EnableBatchProcessing` / `@EnableJdbcJobRepository` in the framework module —
  store selection is an application concern. Apps that need JDBC persistence add the opt-in
  configuration shown above; the framework stays additive.

## Deferred

- Standard transaction manager selection and patterns for multi-database jobs
  (`@BatchDataSource` topology).
- Production database dialects (Oracle / PostgreSQL) and locking differences; tests use H2,
  dialect differences are app-level guidance.
- Framework reader/writer (`io`) adapter contracts — Phase 5.
