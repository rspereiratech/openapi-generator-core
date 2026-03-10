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

class SortPathsPostProcessorTest {

    private final SortPathsPostProcessor processor = new SortPathsPostProcessor();

    // ==========================================================================
    // Sorting
    // ==========================================================================

    @Test
    void process_unsortedPaths_sortedAlphabetically() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        processor.process(openAPI);

        assertEquals(List.of("/a/first", "/m/middle", "/z/last"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_alreadySorted_remainsSorted() {
        OpenAPI openAPI = openApiWith("/a/first", "/b/second", "/c/third");

        processor.process(openAPI);

        assertEquals(List.of("/a/first", "/b/second", "/c/third"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_singlePath_noChange() {
        OpenAPI openAPI = openApiWith("/only");

        processor.process(openAPI);

        assertEquals(List.of("/only"), List.copyOf(openAPI.getPaths().keySet()));
    }

    // ==========================================================================
    // Null / empty safety
    // ==========================================================================

    @Test
    void process_nullPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(openAPI);
        assertNull(openAPI.getPaths());
    }

    @Test
    void process_emptyPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        processor.process(openAPI);
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
