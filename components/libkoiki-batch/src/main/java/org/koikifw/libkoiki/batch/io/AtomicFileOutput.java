package org.koikifw.libkoiki.batch.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for atomic file output: a job writes to an in-progress temp file and the
 * framework promotes it to the final path only when the job succeeds, so
 * downstream systems (e.g. JP1) never pick up a partial file.
 *
 * <p>The temp path is derived from the final path by appending a configurable
 * suffix (default {@code .inprogress}), so the writer and the
 * {@link AtomicOutputListener} agree on the same location without extra
 * coordination.</p>
 */
public final class AtomicFileOutput {

    private static final Logger log = LoggerFactory.getLogger(AtomicFileOutput.class);

    private AtomicFileOutput() {
    }

    /** Returns the in-progress temp path for {@code finalPath} (final + suffix). */
    public static Path tempPath(Path finalPath, String suffix) {
        String safeSuffix = (suffix == null || suffix.isBlank()) ? ".inprogress" : suffix;
        return finalPath.resolveSibling(finalPath.getFileName() + safeSuffix);
    }

    /** Moves the temp file to the final path (replacing any existing final file). */
    public static void promote(Path temp, Path finalPath) {
        if (temp == null || finalPath == null || !Files.exists(temp)) {
            return;
        }
        try {
            Path parent = finalPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try {
                Files.move(temp, finalPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to promote output file {} to {}", temp, finalPath, ex);
        }
    }

    /** Deletes the temp file (best-effort), used when the job did not succeed. */
    public static void discard(Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to discard in-progress output file {}", temp, ex);
        }
    }
}
