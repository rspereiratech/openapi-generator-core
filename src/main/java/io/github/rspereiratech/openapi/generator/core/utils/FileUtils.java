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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * General-purpose file system utilities.
 *
 * @author ruispereira
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {
    /**
     * Creates the parent directories of {@code path} if they do not already exist.
     *
     * <p>If {@code path} has no parent (e.g. a bare filename with no directory component)
     * this method is a no-op.
     *
     * @param path the target file path; must not be {@code null}
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if the directories cannot be created
     */
    public static void createParentDirectories(Path path) throws IOException {
        Preconditions.checkNotNull(path, "'path' must not be null");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
