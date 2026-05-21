# JP1 Operation Assets

This directory contains the initial JP1-oriented operation assets for KOIKI Batch Framework.

Current development line: `v0.1.0`

The contents are intentionally lightweight. They define the project-side operation contract that batch jobs should eventually satisfy, but they are not yet a complete JP1 production design.

## Scope

- `scripts/run-job.sh`: scheduler launcher placeholder
- `jobs/return-code-mapping.md`: initial return code convention
- `runbook/rerun-procedure.md`: initial rerun guidance

## Current Assumptions

- JP1 or another scheduler launches batch jobs through a project-provided script.
- The launcher returns stable process exit codes.
- Operators and scheduler definitions should depend on return codes and runbooks, not Java exception class names.
- Standard job parameters are expected to include job name, business date, and request id, but the final CLI contract is not fixed yet.

## Deferred JP1 Details

- Final command-line arguments for launching Spring Batch jobs
- Environment file layout
- Log file path and rotation policy
- JP1 jobnet naming convention
- Parameter passing convention from JP1 to the launcher
- Timeout and forced-stop behavior
- Rerun versus restart rules per job
- Mapping between KOIKI return codes and JP1 judgment conditions

These details should be refined when the first real reference job and launcher implementation are introduced.
