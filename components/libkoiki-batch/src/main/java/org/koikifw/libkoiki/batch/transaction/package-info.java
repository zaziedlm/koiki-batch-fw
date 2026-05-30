/**
 * Transaction policy support for batch jobs.
 *
 * <p>This package owns shared guidance and the default commit boundary for
 * transaction managers, commit boundaries, rollback behavior, and
 * multi-resource concerns. The full DB-backed model — metadata vs business
 * persistence, transaction topology, and schema ownership — is defined in
 * {@code docs/batch/db-management-architecture.md}.</p>
 *
 * <p>Design boundaries:</p>
 * <ul>
 *   <li>The framework does <b>not</b> create a {@code PlatformTransactionManager}
 *       bean. For DB-backed jobs Spring Boot auto-configures one from the
 *       application's {@code DataSource}; DB-less jobs declare a
 *       {@code ResourcelessTransactionManager} explicitly. A misconfigured app
 *       fails fast rather than silently running non-transactional steps.</li>
 *   <li>The default commit boundary is exposed as
 *       {@code koiki.batch.transaction.defaultCommitInterval}
 *       ({@code org.koikifw.libkoiki.batch.core.KoikiBatchProperties.Transaction}).
 *       Jobs reference it explicitly when building chunk steps — transaction
 *       behavior is never hidden inside utility classes.</li>
 *   <li>With a single shared {@code DataSource}, each chunk commit writes
 *       business rows and step-execution metadata atomically, giving
 *       restart-consistent processing.</li>
 * </ul>
 */
package org.koikifw.libkoiki.batch.transaction;
