# Batch Platform Capabilities

This document records the target capability map for KOIKI Batch Framework. It is intentionally lightweight: the goal is to keep responsibility boundaries clear before heavy implementation begins.

## Current Baseline

- Development line: `v0.1.0`
- Maven version: `0.1.0-SNAPSHOT`
- Java 21
- Spring Boot 4.0.x
- Spring Batch 6.0.x
- Multi-module Maven structure
- Shared framework module: `components/libkoiki-batch`
- Reference application module: `components/koiki_ref_batch_app`
- Customer application modules: `apps/*`
- JP1-oriented operation assets under `ops/jp1`

## Capability Map

| Area | Package | Purpose | Initial Scope |
| --- | --- | --- | --- |
| Core configuration | `org.koikifw.libkoiki.batch.core` | Framework auto-configuration and defaults | Auto-configuration entry point |
| Execution control | `org.koikifw.libkoiki.batch.execution` | Job execution policy, concurrency, rerun/restart guardrails | Concurrency guard service |
| Fault handling | `org.koikifw.libkoiki.batch.fault` | Exception classification, retry/skip, exit code mapping | Policy definitions and common classifiers |
| I/O support | `org.koikifw.libkoiki.batch.io` | Common readers/writers and adapter contracts | DB/file/external interface extension points |
| Observability | `org.koikifw.libkoiki.batch.observability` | Structured logging, lifecycle logging, metrics | Job listener and log context |
| Audit | `org.koikifw.libkoiki.batch.audit` | Audit trail events and publication boundary | Audit event model and publisher interface |
| Security | `org.koikifw.libkoiki.batch.security` | Secret handling, masking, credential boundary | Masking and policy hooks |
| Transaction | `org.koikifw.libkoiki.batch.transaction` | Transaction boundaries and commit policy | Transaction policy guidance and helpers |
| Validation | `org.koikifw.libkoiki.batch.validation` | Job parameter/input/business validation | Validator contracts |
| Support | `org.koikifw.libkoiki.batch.support` | Small framework utilities | Non-business utility helpers |

## Enterprise Concerns

Logging should be structured and correlated by job name, job execution id, step name, business date, tenant/customer key where applicable, and scheduler request id where available. Application logs should avoid raw personal data and secrets.

Transaction handling should be explicit at step boundaries. Chunk steps should define commit interval, retry/skip policy, and rollback behavior. Tasklet steps should document whether they are idempotent and how rerun is handled.

Audit is separate from ordinary logging. Logs help operations diagnose execution; audit records explain business-relevant changes and control events. Audit records should be durable enough for later inspection, but the first implementation can be an interface plus reference implementation.

Fault handling should map technical and business failures to stable exit codes. JP1 scripts and runbooks should depend on those stable codes, not on exception class names.

Security should focus first on masking, secrets boundaries, and avoiding accidental leakage in logs. Authorization models can remain application-specific until there is a concrete shared requirement.

## Placement Rules

- Put reusable framework behavior in `components/libkoiki-batch`.
- Put example business jobs and reference usage in `components/koiki_ref_batch_app`.
- Put customer-specific job configuration and overrides in `apps/*`.
- Put operational scripts, scheduler contracts, and runbooks under `ops/`.
- Put cross-module integration and end-to-end test assets under `tests/` when they are introduced.

## Deferred Decisions

- Persistence model for audit records
- Standard metrics registry and naming convention
- Standard transaction manager selection for multi-database jobs
- Standard file ingestion archive/error directory model
- Standard masking rules for personal data classes
- Exit code catalog beyond the initial `0 / 10 / 20 / 30` convention
