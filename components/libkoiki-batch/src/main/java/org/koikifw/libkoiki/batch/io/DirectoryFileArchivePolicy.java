package org.koikifw.libkoiki.batch.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link FileArchivePolicy} that moves the input file into a configured
 * archive directory on success and an error directory on failure, prefixing the
 * file name with a timestamp to avoid collisions.
 *
 * <p>When the corresponding directory is not configured (blank), the operation is
 * a no-op. Move failures are logged as warnings and swallowed, so post-processing
 * never turns a successful job into a failure.</p>
 */
public class DirectoryFileArchivePolicy implements FileArchivePolicy {

    private static final Logger log = LoggerFactory.getLogger(DirectoryFileArchivePolicy.class);

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final String archiveDir;

    private final String errorDir;

    public DirectoryFileArchivePolicy(String archiveDir, String errorDir) {
        this.archiveDir = archiveDir;
        this.errorDir = errorDir;
    }

    @Override
    public void archive(Path file) {
        move(file, archiveDir, "archive");
    }

    @Override
    public void moveToError(Path file) {
        move(file, errorDir, "error");
    }

    private void move(Path file, String targetDir, String kind) {
        if (file == null || targetDir == null || targetDir.isBlank()) {
            return;
        }
        try {
            Path dir = Path.of(targetDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(LocalDateTime.now().format(STAMP) + "_" + file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to {} ingestion file {} to {}", kind, file, targetDir, ex);
        }
    }
}
