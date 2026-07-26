# Agent Documentation Plan

This plan records the current documentation task for making KOIKI Batch Framework easier to work on with multiple AI coding agents.

## Purpose

Prepare lightweight, tool-neutral guidance that can be read by Codex, Claude Code, Kiro, GitHub Copilot, and human developers.

At the time of this initial documentation plan, the project did not depend on specialized agent skills. The first priority was to establish stable boundaries, verification commands, and placement rules without prescribing implementation details that had not yet been proven by code.

The Phase 0-5 initial scope and three reference-job patterns are now implemented. Follow-up work is tracked in `docs/plans/95-agent-readiness-reference-job-skill.md`, beginning with the lightweight `add-reference-batch-job` shared skill.

## Scope

Create and update:

- Root `AGENTS.md`
- `docs/agent/README.md`
- `docs/agent/boundaries.md`
- `docs/agent/testing.md`

## Tasks

| Task | Target | Status |
| --- | --- | --- |
| Record the documentation plan | `docs/agent/documentation-plan.md` | Done |
| Create repository-wide agent rules | `AGENTS.md` | Done |
| Expand agent documentation index | `docs/agent/README.md` | Done |
| Define module and responsibility boundaries | `docs/agent/boundaries.md` | Done |
| Define testing and verification guidance | `docs/agent/testing.md` | Done |
| Verify documentation references and formatting | affected docs | Done |

## Completion Criteria

- The root agent guidance explains the project purpose, package standard, module boundaries, and basic verification command.
- Agent docs remain tool-neutral and avoid assuming one specific AI assistant.
- Boundaries clarify what belongs in `libkoiki-batch`, `koiki_ref_batch_app`, and `apps/*`.
- Testing guidance documents the current standard command and the future direction for integration/e2e tests.
- The documents avoid premature heavy implementation rules for areas that are still design-stage.

## Follow-up Status

Repository-specific shared skills may now be added under `docs/agent/skills` when backed by working code and tests. Tool-specific discovery adapters remain deferred:

- Claude Code `CLAUDE.md`
- Kiro steering/spec documents
- GitHub Copilot `.github/copilot-instructions.md`
- Codex installation or discovery adapter

These adapters should reference the shared repository guidance instead of copying complete rules into each tool-specific file.
