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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PathUtils}.
 *
 * <p>Covers path normalisation (null handling, leading/trailing slash trimming,
 * duplicate-slash removal) and path concatenation.
 */
class PathUtilsTest {

    // ==========================================================================
    // normalisePath
    // ==========================================================================

    @Test
    void normalisePath_null_returnsEmpty() {
        Assertions.assertEquals("", PathUtils.normalisePath(null));
    }

    @Test
    void normalisePath_blank_returnsEmpty() {
        Assertions.assertAll(
                () -> Assertions.assertEquals("", PathUtils.normalisePath("")),
                () -> Assertions.assertEquals("", PathUtils.normalisePath("   "))
        );
    }

    @Test
    void normalisePath_noLeadingSlash_addsLeadingSlash() {
        Assertions.assertEquals("/api/users", PathUtils.normalisePath("api/users"));
    }

    @Test
    void normalisePath_trailingSlash_removesTrailingSlash() {
        Assertions.assertEquals("/api/users", PathUtils.normalisePath("/api/users/"));
    }

    @Test
    void normalisePath_rootSlash_staysAsRoot() {
        Assertions.assertEquals("/", PathUtils.normalisePath("/"));
    }

    @Test
    void normalisePath_alreadyNormalised_unchanged() {
        Assertions.assertEquals("/api/v1/users", PathUtils.normalisePath("/api/v1/users"));
    }

    @Test
    void normalisePath_noLeadingSlashWithTrailingSlash_bothFixed() {
        Assertions.assertEquals("/api/users", PathUtils.normalisePath("api/users/"));
    }

    // ==========================================================================
    // joinPaths
    // ==========================================================================

    @Test
    void joinPaths_bothEmpty_returnsRoot() {
        Assertions.assertEquals("/", PathUtils.joinPaths("", ""));
    }

    @Test
    void joinPaths_baseEmptySubNonEmpty_returnsSub() {
        Assertions.assertEquals("/sub", PathUtils.joinPaths("", "/sub"));
    }

    @Test
    void joinPaths_baseNonEmptySubEmpty_returnsBase() {
        Assertions.assertEquals("/base", PathUtils.joinPaths("/base", ""));
    }

    @Test
    void joinPaths_bothNonEmpty_concatenatesCorrectly() {
        Assertions.assertEquals("/base/sub", PathUtils.joinPaths("/base", "/sub"));
    }

    @Test
    void joinPaths_noLeadingSlashOnEither_normalisesBoth() {
        Assertions.assertEquals("/base/sub", PathUtils.joinPaths("base", "sub"));
    }

    @Test
    void joinPaths_nullBase_treatedAsEmpty() {
        Assertions.assertEquals("/sub", PathUtils.joinPaths(null, "/sub"));
    }

    @Test
    void joinPaths_nullSub_treatedAsEmpty() {
        Assertions.assertEquals("/base", PathUtils.joinPaths("/base", null));
    }

    @Test
    void joinPaths_bothNull_returnsRoot() {
        Assertions.assertEquals("/", PathUtils.joinPaths(null, null));
    }
}
