# Agent Testing Guidance

This document defines the default verification approach for KOIKI Batch Framework.

The project is still early, so the goal is to keep tests focused and useful while leaving room for stronger integration and e2e testing as real batch jobs are added.

## Default Verification

The standard quick repository-level verification command is:

```powershell
.\mvnw.cmd clean test
```

On macOS / Linux, use `./mvnw clean test`.

Use this for local regression checks when a change affects:

- Maven POM files
- Java source code
- Package names
- Spring Boot or Spring Batch dependencies
- Auto-configuration
- Shared framework package structure
- Test dependencies

`clean` matters because stale `target/` classes can hide package or dependency problems.

`mvn clean test` runs unit tests only (Surefire). Integration tests follow the `*IT` naming convention and run under the Maven Failsafe plugin during `mvn verify`.

Use `mvn verify` before completing changes that affect:

- Reference-app integration
- Failsafe or test naming
- Application launch behavior
- Exit code behavior
- DB-backed jobs or Flyway schema
- File input/output lifecycle
- Cross-module package or dependency boundaries

Current reference integration tests include `BatchCoreWiringIT`, `CustomerDailySyncJobIT`, `ExitCodeE2EIT`, `CustomerImportJobIT`, and `BillingFileJobIT`.

## JDK Pin

The repository pins development builds to **Adoptium Temurin 21 (LTS)** as bundled by the **`pleiades.java-extension-pack-jdk`** VS Code extension. This matches the JDK exposed on the interactive PowerShell `PATH` and avoids the older JRE that ships inside `redhat.java`.

```powershell
$env:JAVA_HOME="$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\21"
.\mvnw.cmd clean verify
```

Maven Wrapper is checked in, so no local Maven installation is required.

When this pin is changed (e.g. moving to a standalone Adoptium install or adopting Java 25 LTS), update this section *and* the `decision-log` so downstream environments and agents pick up the new target consistently.

If a command fails because Maven cannot access Maven Central, rerun with the appropriate network permission in the current tool environment.

## Smaller Scope Commands

Use smaller commands when they are enough for the change.

Examples:

```bash
./mvnw -pl components/libkoiki-batch test
./mvnw -pl components/koiki_ref_batch_app test
./mvnw -pl apps/customer_a_batch_app test
```

For changes that touch a dependency used by downstream modules, prefer the full reactor command:

```bash
./mvnw clean test
```

## Documentation-Only Changes

For documentation-only changes, Maven execution is optional unless the change affects:

- Build instructions
- Module names
- Package names
- Dependency versions
- Test commands
- Paths referenced by build or source code

At minimum, check that referenced files exist and that the document does not contradict `README.md`, `AGENTS.md`, or `docs/batch/*`.

## Current Test State

Current tests include unit tests for framework contracts and integration tests for reference jobs.

Current expectations:

- Unit tests compile.
- Maven reactor builds.
- `mvn verify` runs `*IT` integration tests through Failsafe.
- Java release is 21 (Pleiades-bundled Adoptium Temurin; see JDK Pin above).
- Spring Boot and Spring Batch dependencies resolve.
- Package names are valid for Spring Batch 6 APIs.

## Future Test Layers

As batch implementation matures, introduce the following layers.

### Unit Tests

Use for small framework and business logic:

- Fault classification
- Exit code mapping
- Validation contracts
- Masking rules
- Transaction policy decisions that can be tested without infrastructure

### Slice / Configuration Tests

Use for Spring Boot auto-configuration and framework wiring:

- Auto-configuration imports
- Conditional beans
- Listener registration
- Framework properties binding

### Job Integration Tests

Use for real Spring Batch job execution:

- Job launch with parameters
- Step transition behavior
- Restart/rerun behavior
- Retry/skip behavior
- Transaction rollback behavior
- JobRepository interactions

These tests should live close to the module they verify until a dedicated `tests/integration` module is introduced.

### End-to-End Tests

Use for scheduler-like execution and cross-module workflows:

- JP1 launcher contract
- Exit code behavior
- Input file to output file flow
- DB migration plus job execution
- Operational rerun scenario

When introduced, place cross-module e2e assets under:

```text
tests/e2e
```

## Test Data Rules

Do not use real customer data.

Use synthetic data that is:

- Small
- Deterministic
- Safe to commit
- Clearly tied to the scenario being tested

Sensitive-looking sample values should be obviously fake.

## Agent Checklist

Before finishing a code change, report:

- What verification command was run
- Whether it passed
- Any tests that were not run
- Any residual risk from missing integration coverage
