# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Unofficial Fork

This repository is **NOT** the official Hazelcast project. It is an unofficial fork that exists
to carry downstream patches on top of an official Hazelcast release — e.g. vulnerability fixes and
dependency version bumps (such as the Jackson upgrade) — rather than to change product behavior.

Because of this, the Maven version is suffixed to avoid colliding with the official
release/Enterprise line, e.g. `5.4.0-patch1` for a patch on top of upstream `5.4.0` (see the
`<version>` in the root `pom.xml`). Bump it across the whole reactor with
`mvn versions:set -DnewVersion=<version> -DprocessAllModules=true`, not by hand-editing every
module's `pom.xml`.

## Repository

This is Hazelcast, an in-memory data grid / real-time data platform (distributed maps, queues,
topics, the Jet stream/batch processing engine, and a SQL engine built on Calcite). This checkout
is a fork used for downstream builds (see e.g. the `pom.xml`/dependency-plugin history in
`git log`) — be mindful that changes here may need to stay compatible with an internal EE
(Enterprise Edition) fork and downstream PR process described below.

Requires **JDK 17** to build.

## Build

Always use the Maven wrapper:

```bash
./mvnw clean package -DskipTests
```

A fast iteration build that skips tests, checkstyle, javadoc, and the `extensions`/`distribution`
modules:

```bash
./mvnw clean package -Dquick
```

## Tests

The full test suite is thousands of tests and takes a long time — scope runs to the
module/class/method you're touching.

```bash
# Run tests in a module, scoped to one test class
./mvnw test -pl hazelcast -Dtest=SomeTest

# Run a single test method
./mvnw test -pl hazelcast -Dtest=SomeTest#someMethod

# Default profile: quick + integration tests, parallel, no network
./mvnw test -P parallelTest

# Slow / non-parallelizable tests
./mvnw test -P nightly-build

# Full suite, serial, with network
./mvnw test -P all-tests
```

Tests are grouped via JUnit `@Category`-style marker annotations in
`hazelcast/src/test/java/com/hazelcast/test/annotation/`: `QuickTest`, `SlowTest`, `NightlyTest`,
`ParallelJVMTest`. Pick the runner to match the test's concurrency needs:
`@RunWith(HazelcastParallelClassRunner.class)` for tests safe to run in parallel within a JVM, or
`@RunWith(HazelcastSerialClassRunner.class)` for tests that must run serially (e.g. tests that
bind real network ports). Both runners live in `hazelcast/src/test/java/com/hazelcast/test/`.

Docker-dependent tests can be skipped with `-Dhazelcast.disable.docker.tests`.

Tests default to no real networking (`-Dhazelcast.test.use.network=false`); `*IT.java` failsafe
integration tests use real networking instead.

## Checkstyle

Before pushing, run `./mvnw clean validate` and fix any Checkstyle violations — the PR builder
enforces this. Config lives in `checkstyle/checkstyle.xml` (plus a `checkstyle_jet.xml` variant
for Jet-related modules and matching `suppressions*.xml` files). License headers are enforced via
`checkstyle/ClassHeaderApache.txt` / `ClassHeaderHazelcastCommunity.txt` — new files need the
matching Apache 2.0 or Hazelcast Community License header depending on where they live.

## Module layout

Top-level Maven modules (see `<modules>` in `pom.xml`):

- `hazelcast` — the core module: cluster membership, partitioning, all distributed data
  structures (`map`, `collection`, `multimap`, `queue`/`ringbuffer`, `topic`, `cache`,
  `replicatedmap`, `cp` for Raft-based CP subsystem), the `jet` stream/batch processing engine,
  persistence/hot-restart, WAN replication, security, and the client/server wire protocol under
  `client`. Internal-only implementation code lives under `*.impl` / `*.internal` packages
  (excluded from generated Javadoc, per `pom.xml`).
- `hazelcast-sql` — the SQL engine (Calcite-based query planner/optimizer and execution).
- `hazelcast-spring` / `hazelcast-spring-tests` — Spring integration and its test suite.
- `hazelcast-tpc-engine` — the low-level thread-per-core networking/eventloop engine.
- `hazelcast-archunit-rules` — ArchUnit rules enforced across the codebase (e.g. no mixing JUnit4
  and JUnit5 annotations in one test class, no mixing Hamcrest with AssertJ matchers, valid
  `serialVersionUID` on `Serializable` classes, `CompletableFuture` async methods must take an
  explicit executor). Check `ArchUnitRules.java` before adding patterns these rules forbid.
- `hazelcast-build-utils` — internal build tooling.
- `extensions/*` — pluggable connectors, each an independent module: `kafka`, `kafka-connect`,
  `s3`, `mongodb`, `hadoop`/`hadoop-dist`, `avro`, `csv`, `parquet`-adjacent, `elasticsearch`,
  `grpc`, `kinesis`, `cdc-debezium`, `cdc-mysql`, `cdc-postgres`, `mapstore`, `python`,
  `protobuf`. These are excluded from `-Dquick` builds.
- `hazelcast-it` / `modulepath-tests` — integration tests, including Java module-path
  (`module-info.java`) compatibility checks.
- `distribution` — packaging for the `hazelcast-<version>.zip`/tarball distribution.

## PR / CI conventions (this repo's process, not GitHub's generic flow)

- PRs land in an internal repo first; the final merged commit is pushed back here with external
  contributors credited as co-authors (see `CONTRIBUTING.md`).
- CI is triggered by magic comment phrases on the PR (not by default on every push), e.g.
  `run-lab-run` (default PR builder), `run-ee-compile` / `run-ee-tests` (build/test against the
  Enterprise fork), `run-sql-only` (for PRs touching only `hazelcast-sql`, label `SQL-only`),
  `run-docs-only` (for `.md`/`.adoc`/`.txt`-only PRs, label `docs-only`), `run-windows`,
  `run-arm64`, `run-with-ibm-jdk-8`, `run-nightly-tests`, `run-cdc-*-tests`, `run-mongodb-tests`,
  `run-s3-tests`, `run-sonar`. See `CONTRIBUTING.md` for the full list — use these when guiding a
  PR through review rather than assuming GitHub Actions run everything automatically.
- When a change touches `hazelcast-sql` and nothing else, label the PR `SQL-only` to shorten the
  build. When it only touches `.md`/`.adoc`/`.txt`, label it `docs-only`.
- Jackson's version (`jackson.version` in `pom.xml`) must be kept in sync with the Enterprise
  (EE) fork — bumping it here requires a matching EE PR.
