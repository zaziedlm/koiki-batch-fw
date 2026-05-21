# Rerun Procedure

Current development line: `v0.1.0`

This is the initial rerun guidance for JP1-oriented execution. Detailed job-specific recovery procedures should be added when real jobs are implemented.

## Basic Flow

1. Identify the return code.
2. Check the job log and operation log for the failed execution.
3. Determine whether the failure is a business error or system error.
4. Fix the cause or confirm that rerun is allowed.
5. Re-run with the same business date and a new request id.
6. Record the rerun result in the operation log.

## Current Assumptions

- Business date is treated as a stable rerun key.
- Request id is unique per launch attempt.
- Each job should eventually document whether it supports restart, rerun, or both.
- Operators should not manually change business data unless the job-specific runbook allows it.

## Deferred

- Job-specific restartability rules
- Data correction procedure
- Duplicate prevention checks
- JP1 jobnet operation steps
- Required approval flow for rerun
