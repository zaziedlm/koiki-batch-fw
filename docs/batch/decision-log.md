# Batch Decision Log

## 2026-05-21: Development Version Baseline

Decision: use `v0.1.0` as the current KOIKI Batch Framework development line.

Reason: the project has reached an initial framework baseline with Maven modules, dependency baseline, package boundaries, and agent-facing guidance.

Impact: Maven artifacts remain on `0.1.0-SNAPSHOT` while this line is under active development. A non-SNAPSHOT release can be introduced when the first usable batch core and reference job are ready.

## 2026-05-21: Package Root

Decision: use `org.koikifw.*` as the official Java package root.

Reason: `koikifw.org` is the owned domain, and Java package naming should follow the owned reverse domain.

Impact: framework and reference packages under `org.koikifw.*` are considered canonical.

## 2026-05-21: Framework Capability Boundaries

Decision: define shared batch framework responsibilities under `org.koikifw.libkoiki.batch.*`, with dedicated packages for execution, fault handling, I/O, observability, audit, security, transaction, validation, and support.

Reason: enterprise batch applications need clear separation between operations, audit, transaction control, and business logic. Creating the package map early prevents common framework behavior from leaking into customer applications.

Impact: initial implementations may be small, but new shared features should be placed according to the package responsibility map in `platform-capabilities.md`.

## 2026-05-21: Dependency Baseline

Decision: use Java 21, Spring Boot 4.0.x, and Spring Batch 6.0.x as the current development baseline.

Reason: the project is at an early stage and can adopt the current stable major line before application code accumulates.

Impact: code must follow Spring Batch 6 package names and APIs.
