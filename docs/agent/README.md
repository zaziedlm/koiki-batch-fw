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
- Shared framework package map
- Reference application skeleton
- Customer application skeleton
- JP1-oriented operation documentation

The project does not yet have a complete production batch core implementation. Agents should treat logging, audit, transaction, fault handling, and validation as defined capability areas, not as fully implemented features.

## Tool-Neutral Rules

Prefer Markdown documents in this repository as the source of truth.

Tool-specific instruction files can be added later, but they should point back to these shared documents instead of duplicating rules. This prevents Codex, Claude Code, Kiro, and GitHub Copilot from drifting into different project conventions.

When a tool suggests a change that conflicts with this repository guidance, follow the repository guidance and document any exception.

## When to Create Tool-Specific Skills

Dedicated agent skills or tool-specific instructions are useful after implementation patterns are proven.

Good triggers:

- A reference job has a real Job/Step/Reader/Processor/Writer implementation.
- The same new-batch creation workflow has been repeated more than once.
- Logging, audit, transaction, and fault-handling patterns have working minimal implementations.
- Integration or e2e testing conventions exist.

Until then, keep guidance lightweight and shared.

## Documentation Maintenance

Update these files when project rules change:

- Root `AGENTS.md`: repository-wide rules
- `docs/agent/boundaries.md`: ownership and placement boundaries
- `docs/agent/testing.md`: verification rules
- `docs/batch/decision-log.md`: durable decisions and rationale
- `docs/batch/platform-capabilities.md`: batch framework capability map

Do not bury important conventions only inside chat history or generated code comments.
