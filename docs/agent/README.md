# Agent Documentation

This directory contains tool-neutral guidance for AI coding agents and human contributors working on KOIKI Batch Framework.

The documents are intended to be useful for Codex, Claude Code, Kiro, GitHub Copilot, and similar tools. They avoid tool-specific command syntax unless necessary and focus on project boundaries, verification, and stable development rules.

## Reading Order

Start with:

1. `AGENTS.md` at the repository root
2. `README.md` at the repository root
3. `docs/batch/architecture-batch.md`
4. `docs/batch/platform-capabilities.md`
5. `docs/agent/boundaries.md`
6. `docs/agent/testing.md`

Use `docs/batch/decision-log.md` when a change involves a durable architecture or policy decision.

## Agent Goals

AI agents should help maintain a clean enterprise batch framework by:

- Preserving module boundaries
- Keeping package names under the approved roots
- Making small, verifiable changes
- Avoiding premature framework abstractions
- Recording important decisions in documentation
- Running the smallest useful verification for the change

## Current Project Stage

Current development line: `v0.1.0`

Maven artifacts currently use `0.1.0-SNAPSHOT`.

The project currently has:

- A Maven multi-module structure
- Java 21 baseline
- Spring Boot 4.0.x baseline
- Spring Batch 6.0.x baseline
- Implemented Phase 0-5 initial framework scope covering core, execution, fault handling, observability, audit, security, transaction, validation, and I/O support
- Three executable reference jobs:
  - `customer-daily-sync`: tasklet reference job
  - `customer-import`: DB-backed chunk reference job
  - `billing-file`: file-to-file chunk reference job
- Unit tests for framework contracts
- Failsafe integration tests for framework wiring, launch and exit-code behavior, DB-backed processing, logging, audit, masking, and file I/O lifecycle
- A repository-owned customer application skeleton under `apps/customer_a_batch_app`
- JP1-oriented operation documentation

The `v0.1.0` initial framework scope is implemented and demonstrated through the reference application. It is not yet a complete production platform. Distributed locking, durable audit storage, production database dialect verification, standard PII-class masking rules, and the production JP1 launcher remain deferred.

Customer applications should depend directly on `libkoiki-batch`. They should not inherit production job implementations from `koiki-ref-batch-app`.

## Verification Summary

Use the smallest verification that covers the change:

- `mvn clean test`: Surefire unit tests for a quick local regression check
- `mvn verify`: full verification including Failsafe `*IT` integration tests

Use `mvn verify` before completing changes that affect reference jobs, application launch behavior, exit codes, DB/Flyway integration, file I/O lifecycle, or cross-module package and dependency boundaries.

See `docs/agent/testing.md` for environment-specific commands and the detailed verification policy.

## Tool-Neutral Rules

Prefer Markdown documents in this repository as the source of truth.

Tool-specific instruction files can be added later, but they should point back to these shared documents instead of duplicating rules. This prevents Codex, Claude Code, Kiro, and GitHub Copilot from drifting into different project conventions.

When a tool suggests a change that conflicts with this repository guidance, follow the repository guidance and document any exception.

## Skill Readiness

The project now has enough working implementation patterns to introduce small, task-focused agent skills.

Good skill candidates are workflows that:

- Reuse the existing tasklet, DB chunk, or file chunk reference patterns
- Preserve framework/reference/customer module boundaries
- Apply established logging, audit, transaction, validation, fault-handling, and I/O conventions
- Have clear unit or integration verification requirements

Keep skills lightweight and point them back to repository documents and working code. Do not duplicate the complete architecture or create tool-specific rules that can drift from `AGENTS.md`.

## Shared Skills

`docs/agent/skills` is the tool-neutral source of truth for repository-specific agent workflows.

| Skill | Status | Purpose |
| --- | --- | --- |
| [`add-reference-batch-job`](skills/add-reference-batch-job/SKILL.md) | Usable | Add or extend a reference job under `components/koiki_ref_batch_app`, using a lightweight requirement gate before selecting or combining the existing tasklet, DB chunk, or file chunk patterns. |
| [`koiki-batch-overview`](skills/koiki-batch-overview/SKILL.md) | Seed | Future project overview and module-planning workflow. The current one-line file is not yet an operational skill. |
| [`koiki-batch-ops-jp1`](skills/koiki-batch-ops-jp1/SKILL.md) | Seed | Future JP1 operation workflow. The current one-line file is not yet an operational skill. |

Use `add-reference-batch-job` only for reference application jobs. Customer-specific jobs belong under `apps/*`, common framework capability design belongs under `components/libkoiki-batch`, and JP1 launcher work belongs under `ops/jp1`.

The requirement gate distinguishes `実装開始`, `暫定試行`, and `設計確認`. It should prevent assumptions about data integrity, restart, security, audit, and operations from silently becoming specifications, without forcing a full requirements questionnaire for every small change. Existing patterns may be combined, but an unverified combination such as file-to-DB must not be described as an implemented reference pattern.

These shared files are repository documentation, not automatic installation or discovery configuration for every AI tool. Codex, Claude Code, Kiro, and GitHub Copilot use different discovery mechanisms. Tool-specific adapters may point to these files later, but should not duplicate their rules.

## Documentation Maintenance

Update these files when project rules change:

- Root `AGENTS.md`: repository-wide rules
- `docs/agent/boundaries.md`: ownership and placement boundaries
- `docs/agent/testing.md`: verification rules
- `docs/agent/skills/*`: repeatable repository-specific workflows
- `docs/batch/decision-log.md`: durable decisions and rationale
- `docs/batch/platform-capabilities.md`: batch framework capability map

Do not bury important conventions only inside chat history or generated code comments.
