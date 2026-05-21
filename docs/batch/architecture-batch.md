# Batch Architecture

## Module Topology

- `components/libkoiki-batch`: reusable framework layer
- `components/koiki_ref_batch_app`: reference implementation using the framework
- `apps/*`: customer-specific batch applications

## Framework Package Topology

The shared batch framework uses `org.koikifw.libkoiki.batch.*` as its public package root.

- `core`: Spring Boot auto-configuration and framework defaults
- `execution`: job execution policy, concurrency guard, restart/rerun support
- `fault`: exception classification, retry/skip policy, abnormal termination mapping
- `io`: reusable reader/writer support for files, database access, and external interfaces
- `observability`: structured logging, metrics, job/step lifecycle listeners
- `audit`: audit event model and audit trail publication
- `security`: secret handling, credential boundaries, data masking hooks
- `transaction`: transaction policy, transaction manager helpers, commit boundary guidance
- `validation`: input, parameter, and business rule validation support
- `support`: small shared utilities that do not own business rules

## Processing Topology

Each job should be implemented with explicit Spring Batch responsibilities:

- Job configuration (flow, transitions, restart policy)
- Step configuration (chunk/tasklet, commit interval, transaction boundary)
- Reader / Processor / Writer composition
- Domain service for business rules
- Infrastructure adapters (DB, file, external API)

Customer and reference applications should keep business-specific classes outside
`libkoiki-batch`. Common framework behavior belongs in `libkoiki-batch`; sample usage
and reference business flow belong in `koiki_ref_batch_app`; customer-specific
customization belongs in `apps/*`.

## Operations

- Scheduler integration: JP1 via `ops/jp1/scripts/run-job.sh`
- Exit code convention: 0 / 10 / 20 / 30
- Rerun and recovery: `ops/jp1/runbook/rerun-procedure.md`
