# Agent Boundaries

This document defines where code and documentation should be placed when working on KOIKI Batch Framework.

Use this as the primary boundary guide for Codex, Claude Code, Kiro, GitHub Copilot, and human contributors.

## Package Roots

Official framework and reference packages use `org.koikifw.*`.

- Shared framework: `org.koikifw.libkoiki.batch.*`
- Reference app: `org.koikifw.refapp.batch.*`
- Customer apps: customer-owned package roots, for example `com.customer.a.batch.*`

Do not introduce a new root package without recording the decision.

## Module Ownership

| Area | Path | Ownership |
| --- | --- | --- |
| Shared framework | `components/libkoiki-batch` | Reusable batch platform code |
| Reference application | `components/koiki_ref_batch_app` | Standard usage examples and reference business flows |
| Customer applications | `apps/*` | Customer-specific jobs, rules, and integrations |
| Batch docs | `docs/batch` | Architecture, capabilities, operations, and decisions |
| Agent docs | `docs/agent` | Agent-facing development guidance |
| Operations | `ops` | Scheduler scripts, runbooks, monitoring, and operational contracts |

## `libkoiki-batch` Boundary

Place code here only when it is reusable across multiple batch applications or defines a framework extension point.

Good examples:

- Auto-configuration
- Framework properties
- Execution guardrails
- Common job/step listeners
- Structured logging support
- Audit event interfaces
- Fault classification contracts
- Exit code mapping support
- Transaction policy helpers
- Validation interfaces
- Common I/O adapter contracts
- Security and masking hooks

Avoid:

- Customer-specific business rules
- Customer-specific SQL
- Customer-specific file layouts
- One-off job implementations
- Application-specific model classes
- Hard-coded scheduler job names
- Operational shortcuts that apply to only one customer

When in doubt, start in the reference app or customer app. Promote to `libkoiki-batch` only after reuse is clear.

## Framework Package Boundaries

Use these package responsibilities inside `org.koikifw.libkoiki.batch`.

| Package | Put Here | Do Not Put Here |
| --- | --- | --- |
| `core` | Auto-configuration and defaults | Business job logic |
| `execution` | Concurrency, restart, rerun, launch policy | Business validation rules |
| `fault` | Exception classification and exit code mapping | Logging format decisions unrelated to faults |
| `io` | Reader/writer support and adapter contracts | Customer-specific schema parsing |
| `observability` | Logs, metrics, lifecycle listeners, correlation context | Audit persistence models |
| `audit` | Audit event models and publisher boundaries | Ordinary diagnostic logs |
| `security` | Masking, secret boundaries, sensitive-data hooks | Customer authorization policy unless reusable |
| `transaction` | Commit boundary helpers and transaction policy | Business data update rules |
| `validation` | Reusable validators and validation contracts | One-off input checks tied to one job |
| `support` | Small neutral utilities | Cross-cutting features that deserve a package |

## Reference Application Boundary

Use `components/koiki_ref_batch_app` to show how the framework should be used.

Good examples:

- A representative customer sync job
- A representative billing job
- Sample job configuration
- Sample services and repositories
- Sample input/output models
- Sample use of logging, transaction, audit, and fault handling

Avoid:

- Customer-specific naming
- Real customer credentials or endpoint assumptions
- Framework code that belongs in `libkoiki-batch`
- Test-only shortcuts presented as production patterns

The reference app should be realistic enough to guide implementation, but generic enough to be reusable as a template.

## Customer Application Boundary

Use `apps/*` for customer-specific implementation.

Good examples:

- Customer-specific Job configuration
- Customer-specific file format
- Customer-specific external API adapters
- Customer-specific DB schema access
- Customer-specific business services
- Customer-specific operation variations

Avoid:

- Editing `libkoiki-batch` to satisfy one customer without a reusable abstraction
- Copying framework internals into a customer app
- Hiding customer-specific behavior in the reference app

## Documentation Boundary

Use `docs/batch` for project architecture and batch platform decisions.

Use `docs/agent` for instructions that help agents and contributors modify the repository correctly.

Use `docs/batch/decision-log.md` when a decision has long-term impact, such as:

- Package root changes
- Dependency baseline changes
- Module structure changes
- Standard exit code changes
- Logging, audit, or transaction policy changes

## Change Review Questions

Before making or accepting a change, ask:

- Is this framework code, reference code, or customer code?
- Is the package root correct?
- Does this introduce a new cross-cutting policy?
- Should a decision be recorded?
- Is the implementation heavier than the current project stage needs?
- Can the change be verified with `mvn clean test` or a smaller command?
