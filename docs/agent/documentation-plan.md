# Agent Documentation Plan

This plan records the current documentation task for making KOIKI Batch Framework easier to work on with multiple AI coding agents.

## Purpose

Prepare lightweight, tool-neutral guidance that can be read by Codex, Claude Code, Kiro, GitHub Copilot, and human developers.

At this stage, the project should not depend on heavily specialized agent skills. The batch core is still early, so the documentation should define stable boundaries, verification commands, and placement rules without over-prescribing implementation details that are not proven by code yet.

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

## Deferred

Dedicated tool-specific files can be added later when the implementation patterns are proven:

- Codex skill
- Claude Code `CLAUDE.md`
- Kiro steering/spec documents
- GitHub Copilot `.github/copilot-instructions.md`
