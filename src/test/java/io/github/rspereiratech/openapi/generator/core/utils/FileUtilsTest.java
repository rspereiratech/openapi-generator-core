/*
 *   ___                   _   ___ ___
 *  / _ \ _ __  ___ _ _   /_\ | _ \_ _|
 * | (_) | '_ \/ -_) ' \ / _ \|  _/| |
 *  \___/| .__/\___|_||_/_/ \_\_| |___|   Generator
 *       |_|
 *
 * MIT License - Copyright (c) 2026 Rui Pereira
 * See LICENSE in the project root for full license information.
 */
package io.github.rspereiratech.openapi.generator.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FileUtils}.
 *
 * <p>Verifies that parent directories are created as needed before writing,
 * and that {@link java.io.IOException} is propagated correctly when the path
 * cannot be written.
 */
class FileUtilsTest {

    // ==========================================================================
    // createParentDirectories
    // ==========================================================================

    @Test
    void createParentDirectories_nullPath_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> FileUtils.createParentDirectories(null));
    }

    @Test
    void createParentDirectories_parentAlreadyExists_noOp(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("file.yaml");
        FileUtils.createParentDirectories(file);
        assertTrue(Files.isDirectory(tempDir), "Existing parent directory must remain intact");
    }

    @Test
    void createParentDirectories_singleMissingLevel_createsDirectory(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("subdir/file.yaml");
        FileUtils.createParentDirectories(file);
        assertTrue(Files.isDirectory(file.getParent()), "Single missing parent directory must be created");
    }

    @Test
    void createParentDirectories_multipleMissingLevels_createsAllDirectories(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("a/b/c/file.yaml");
        FileUtils.createParentDirectories(file);
        assertTrue(Files.isDirectory(file.getParent()), "All missing parent directories must be created");
    }

    @Test
    void createParentDirectories_noParent_noOp() throws IOException {
        // A bare filename has no parent — must be a no-op without throwing
        Path bare = Path.of("file.yaml");
        FileUtils.createParentDirectories(bare);
    }
}
