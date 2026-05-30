# AGENTS.md

This file defines repository-wide guidance for AI coding agents and human contributors working on KOIKI Batch Framework.

The guidance is intentionally tool-neutral. Codex, Claude Code, Kiro, GitHub Copilot, and other agents should follow the same project boundaries unless a more specific local instruction file exists.

## Project Purpose

KOIKI Batch Framework is a Java/Spring Batch project for enterprise batch applications.

Current development line: `v0.1.0`

Maven artifacts currently use `0.1.0-SNAPSHOT`.

The repository follows a three-layer project model:

- Shared framework: `components/libkoiki-batch`
- Reference application: `components/koiki_ref_batch_app`
- Customer applications: `apps/*`

The current project is still in an early framework stage. Prefer clear boundaries, small implementations, and documented decisions over heavy framework code that has not yet been validated by real jobs.

## Package Standard

Use `org.koikifw.*` as the official Java package root.

Framework code belongs under:

```text
org.koikifw.libkoiki.batch.*
```

Reference application code belongs under:

```text
org.koikifw.refapp.batch.*
```

Repository-owned customer sample apps should use:

```text
org.koikifw.customer.<customer-id>.batch.*
```

Real downstream customer apps outside this repository may use customer-owned roots when that decision is explicit, for example:

```text
jp.co.customer.example.batch.*
com.customer.example.batch.*
```

`libkoiki-batch` must not depend on customer application package roots. A customer app works outside `org.koikifw.*` as long as it depends on `libkoiki-batch` and its own Spring Boot component scan includes its job configuration.

Do not introduce a new package root without updating the relevant documentation and explaining the reason.

## Module Boundaries

### `components/libkoiki-batch`

Use this module for reusable framework behavior only.

Good candidates:

- Spring Boot auto-configuration
- Job execution control
- Concurrency guardrails
- Fault classification
- Exit code mapping
- Structured logging support
- Audit event contracts
- Transaction policy helpers
- I/O adapter contracts
- Validation contracts
- Security and masking hooks

Avoid placing business-specific jobs, customer-specific schemas, customer-specific repositories, or one-off operational workarounds here.

### `components/koiki_ref_batch_app`

Use this module for reference implementations and sample business flows.

This module should demonstrate how the framework is intended to be used. It can contain realistic sample jobs, services, repositories, and models, but should not become a customer-specific application.

### `apps/*`

Use these modules for customer-specific jobs and integrations.

Customer-specific input formats, SQL, business services, external interfaces, and operational variations should stay in the relevant customer app unless they are proven reusable.

## Batch Design Rules

Keep Spring Batch responsibilities explicit:

- Job configuration defines flow, transitions, restart behavior, and job parameters.
- Step configuration defines chunk/tasklet style, transaction boundary, commit interval, and retry/skip behavior.
- Reader, Processor, and Writer components should have focused I/O and transformation roles.
- Domain services own business rules.
- Repository and adapter classes own DB, file, and external API interaction.

Do not hide transaction behavior, retry behavior, or exit code behavior inside unrelated utility classes.

## Enterprise Concerns

Logging, audit, transaction handling, fault handling, and security are separate concerns.

- Logs are for operational diagnosis.
- Audit records are for explaining business-relevant changes and control events.
- Transaction policies define consistency and rerun behavior.
- Fault policies classify failures and map them to stable exit codes.
- Security support prevents accidental exposure of secrets and sensitive data.

When adding a feature in one of these areas, update or reference `docs/batch/platform-capabilities.md`.

## Verification

The default verification command is:

```powershell
.\mvnw.cmd clean test
```

On macOS / Linux, use `./mvnw clean test`.

`mvn clean test` runs Surefire unit tests. Integration tests named `*IT` run under Failsafe during `mvn verify`.

Use `mvn verify` before completing changes that affect reference-app integration, Failsafe, launch behavior, exit codes, DB-backed jobs, I/O lifecycle, or package/dependency boundaries across modules.

When using the VS Code Java Extension Pack managed JDK on Windows, `JAVA_HOME` may still need to be set as documented in `README.md`.

For documentation-only changes, Maven execution is optional unless the change affects build instructions, modules, package names, or generated references.

## Change Discipline

Keep changes small and aligned with existing project structure.

Do not:

- Add heavy framework abstractions before a concrete reference job needs them.
- Move customer-specific behavior into `libkoiki-batch` prematurely.
- Change the Spring Boot or Spring Batch baseline without running a clean build.
- Change package roots without updating docs and tests.
- Commit generated `target/` content.

Do:

- Update `docs/batch/decision-log.md` for durable design decisions.
- Update `docs/agent/*` when agent-facing rules change.
- Prefer clear package placement over broad utility classes.
- Keep future work documented when a capability is intentionally deferred.

## Key Documents

- `README.md`: project overview and current status
- `docs/batch/architecture-batch.md`: batch architecture
- `docs/batch/platform-capabilities.md`: framework capability map
- `docs/batch/decision-log.md`: design decisions
- `docs/agent/README.md`: agent documentation index
- `docs/agent/boundaries.md`: placement and ownership boundaries
- `docs/agent/testing.md`: verification guidance
