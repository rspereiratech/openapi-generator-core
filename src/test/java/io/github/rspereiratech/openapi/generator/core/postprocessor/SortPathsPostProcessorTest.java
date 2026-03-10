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
package io.github.rspereiratech.openapi.generator.core.postprocessor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortPathsPostProcessorTest {

    private final SortPathsPostProcessor enabled  = new SortPathsPostProcessor(true);
    private final SortPathsPostProcessor disabled = new SortPathsPostProcessor(false);

    // ==========================================================================
    // Sorting enabled
    // ==========================================================================

    @Test
    void process_unsortedPaths_sortedAlphabetically() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        enabled.process(openAPI);

        assertEquals(List.of("/a/first", "/m/middle", "/z/last"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_alreadySorted_remainsSorted() {
        OpenAPI openAPI = openApiWith("/a/first", "/b/second", "/c/third");

        enabled.process(openAPI);

        assertEquals(List.of("/a/first", "/b/second", "/c/third"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_singlePath_noChange() {
        OpenAPI openAPI = openApiWith("/only");

        enabled.process(openAPI);

        assertEquals(List.of("/only"), List.copyOf(openAPI.getPaths().keySet()));
    }

    // ==========================================================================
    // Sorting disabled
    // ==========================================================================

    @Test
    void process_sortOutputFalse_pathOrderUnchanged() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        disabled.process(openAPI);

        // Original insertion order must be preserved — no sorting applied.
        assertEquals(List.of("/z/last", "/a/first", "/m/middle"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    // ==========================================================================
    // Null / empty safety
    // ==========================================================================

    @Test
    void process_nullOpenAPI_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> enabled.process(null));
    }

    @Test
    void process_nullPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        enabled.process(openAPI);
        assertNull(openAPI.getPaths());
    }

    @Test
    void process_emptyPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        enabled.process(openAPI);
        assertEquals(0, openAPI.getPaths().size());
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private static OpenAPI openApiWith(String... paths) {
        OpenAPI openAPI = new OpenAPI();
        Paths p = new Paths();
        for (String path : paths) {
            p.addPathItem(path, new PathItem());
        }
        openAPI.setPaths(p);
        return openAPI;
    }
}
