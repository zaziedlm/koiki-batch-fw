package org.koikifw.libkoiki.batch.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryFileArchivePolicyTest {

    @TempDir
    Path tmp;

    private Path inputFile(String name) throws IOException {
        Path file = tmp.resolve(name);
        Files.writeString(file, "data");
        return file;
    }

    @Test
    void archiveMovesFileToArchiveDir() throws IOException {
        Path archive = tmp.resolve("archive");
        Path file = inputFile("in.dat");
        DirectoryFileArchivePolicy policy = new DirectoryFileArchivePolicy(archive.toString(), "");

        policy.archive(file);

        assertThat(file).doesNotExist();
        try (var entries = Files.list(archive)) {
            assertThat(entries.filter(p -> p.getFileName().toString().endsWith("in.dat"))).hasSize(1);
        }
    }

    @Test
    void moveToErrorMovesFileToErrorDir() throws IOException {
        Path error = tmp.resolve("error");
        Path file = inputFile("bad.dat");
        DirectoryFileArchivePolicy policy = new DirectoryFileArchivePolicy("", error.toString());

        policy.moveToError(file);

        assertThat(file).doesNotExist();
        try (var entries = Files.list(error)) {
            assertThat(entries.filter(p -> p.getFileName().toString().endsWith("bad.dat"))).hasSize(1);
        }
    }

    @Test
    void blankDirectoryIsNoOp() throws IOException {
        Path file = inputFile("keep.dat");
        DirectoryFileArchivePolicy policy = new DirectoryFileArchivePolicy("", "");

        policy.archive(file);

        assertThat(file).exists();
    }

    @Test
    void moveFailureDoesNotThrow() {
        Path missing = tmp.resolve("missing.dat");
        DirectoryFileArchivePolicy policy = new DirectoryFileArchivePolicy(tmp.resolve("archive").toString(), "");

        assertThatCode(() -> policy.archive(missing)).doesNotThrowAnyException();
    }
}
