/**
 * Reusable input/output support for batch processing.
 *
 * <p>This package is for framework-level readers, writers, resource handling,
 * and adapter contracts. Business-specific file layouts and schemas belong in
 * application modules.</p>
 *
 * <p>Parsing is performed by Spring Batch's {@code FlatFileItemReader} /
 * {@code FlatFileItemWriter}; the framework adds three independent, opt-in IO
 * value-adds that compose without interfering (a job uses only what applies):</p>
 * <ul>
 *   <li><b>Charset policy</b> — the file charset is configurable via
 *       {@code koiki.batch.io.file.charset} (default {@code MS932}); resolve it
 *       with {@link org.koikifw.libkoiki.batch.io.BatchCharsets}.</li>
 *   <li><b>Input-file lifecycle</b> —
 *       {@link org.koikifw.libkoiki.batch.io.FileIngestionLifecycleListener}
 *       checks the input file exists and is non-empty, then archives it on
 *       success / moves it to the error directory on failure via
 *       {@link org.koikifw.libkoiki.batch.io.FileArchivePolicy}
 *       (default {@link org.koikifw.libkoiki.batch.io.DirectoryFileArchivePolicy}).
 *       Keyed by the {@code koiki.io.inputFile} job parameter; no-op when absent.</li>
 *   <li><b>Atomic output</b> —
 *       {@link org.koikifw.libkoiki.batch.io.AtomicOutputListener} promotes an
 *       in-progress temp file (see {@link org.koikifw.libkoiki.batch.io.AtomicFileOutput})
 *       to the final path only on success, so downstream never sees a partial
 *       file. Keyed by the {@code koiki.io.outputFile} job parameter; no-op when
 *       absent.</li>
 * </ul>
 *
 * <p>Because the input and output concerns are independent and no-op when their
 * parameter is absent, future {@code file→DB} (charset + input lifecycle) and
 * {@code DB→file} (charset + atomic output) jobs reuse these parts as-is.
 * Standard archive/error directory naming, reject-record files, path-resolution
 * conventions, and external-interface adapters are deferred.</p>
 */
package org.koikifw.libkoiki.batch.io;
