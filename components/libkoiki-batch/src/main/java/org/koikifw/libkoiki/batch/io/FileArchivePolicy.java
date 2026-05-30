package org.koikifw.libkoiki.batch.io;

import java.nio.file.Path;

/**
 * Post-processing policy for an ingestion input file: where it goes after the job
 * succeeds or fails.
 *
 * <p>This is the framework's reusable hook for input-file lifecycle handling,
 * separate from parsing (which Spring Batch's {@code FlatFileItemReader}
 * performs). Implementations decide the concrete destination; the framework ships
 * a directory-based reference implementation and applications may replace it. The
 * standard archive/error directory naming and retention model is deferred.</p>
 *
 * <p>Implementations must not throw for ordinary move failures (a failed archive
 * must not turn a successful job into a failure); they report and continue.</p>
 */
public interface FileArchivePolicy {

    /** Handles a successfully processed input file (e.g. move to an archive directory). */
    void archive(Path file);

    /** Handles an input file whose job failed (e.g. move to an error directory). */
    void moveToError(Path file);
}
