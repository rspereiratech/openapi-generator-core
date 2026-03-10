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
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortSpecPostProcessorTest {

    private final SortSpecPostProcessor enabled  = new SortSpecPostProcessor(true);
    private final SortSpecPostProcessor disabled = new SortSpecPostProcessor(false);

    // ==========================================================================
    // Path sorting — enabled
    // ==========================================================================

    @Test
    void process_unsortedPaths_sortedAlphabetically() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        enabled.process(openAPI);

        assertEquals(List.of("/a/first", "/m/middle", "/z/last"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_alreadySortedPaths_remainsSorted() {
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
    // Response sorting — enabled
    // ==========================================================================

    @Test
    void process_unsortedResponses_sortedByStatusCode() {
        Operation op = operationWithResponses("500", "200", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        enabled.process(openAPI);

        assertEquals(List.of("200", "404", "500"),
                List.copyOf(op.getResponses().keySet()));
    }

    @Test
    void process_alreadySortedResponses_remainsSorted() {
        Operation op = operationWithResponses("200", "201", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        enabled.process(openAPI);

        assertEquals(List.of("200", "201", "404"),
                List.copyOf(op.getResponses().keySet()));
    }

    @Test
    void process_multipleOperations_allResponsesSorted() {
        Operation get  = operationWithResponses("404", "200");
        Operation post = operationWithResponses("500", "201", "400");
        PathItem item = new PathItem().get(get).post(post);
        OpenAPI openAPI = new OpenAPI();
        Paths paths = new Paths();
        paths.addPathItem("/test", item);
        openAPI.setPaths(paths);

        enabled.process(openAPI);

        assertEquals(List.of("200", "404"), List.copyOf(get.getResponses().keySet()));
        assertEquals(List.of("201", "400", "500"), List.copyOf(post.getResponses().keySet()));
    }

    @Test
    void process_operationWithNullResponses_doesNotThrow() {
        Operation op = new Operation();  // no responses set
        OpenAPI openAPI = openApiWithOperation("/test", op);
        enabled.process(openAPI);
    }

    // ==========================================================================
    // Sorting disabled
    // ==========================================================================

    @Test
    void process_sortOutputFalse_pathOrderUnchanged() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        disabled.process(openAPI);

        assertEquals(List.of("/z/last", "/a/first", "/m/middle"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_sortOutputFalse_responseOrderUnchanged() {
        Operation op = operationWithResponses("500", "200", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        disabled.process(openAPI);

        assertEquals(List.of("500", "200", "404"),
                List.copyOf(op.getResponses().keySet()));
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

    private static OpenAPI openApiWithOperation(String path, Operation operation) {
        OpenAPI openAPI = new OpenAPI();
        Paths paths = new Paths();
        paths.addPathItem(path, new PathItem().get(operation));
        openAPI.setPaths(paths);
        return openAPI;
    }

    private static Operation operationWithResponses(String... codes) {
        ApiResponses responses = new ApiResponses();
        for (String code : codes) {
            responses.addApiResponse(code, new ApiResponse().description(code));
        }
        return new Operation().responses(responses);
    }
}
