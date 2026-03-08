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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniqueOperationIdPostProcessorTest {

    private UniqueOperationIdPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UniqueOperationIdPostProcessor();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private OpenAPI buildOpenAPI(String... operationIds) {
        OpenAPI api = new OpenAPI();
        Paths paths = new Paths();
        for (int i = 0; i < operationIds.length; i++) {
            Operation op = new Operation();
            if (operationIds[i] != null) {
                op.setOperationId(operationIds[i]);
            }
            PathItem item = new PathItem();
            item.setGet(op);
            paths.addPathItem("/path" + i, item);
        }
        api.setPaths(paths);
        return api;
    }

    private String operationId(OpenAPI api, int pathIndex) {
        PathItem item = api.getPaths().values().stream()
                .skip(pathIndex)
                .findFirst()
                .orElseThrow();
        return item.readOperations().get(0).getOperationId();
    }

    // ==========================================================================
    // Preconditions
    // ==========================================================================

    @Test
    void process_nullOpenAPI_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    // ==========================================================================
    // Early exit
    // ==========================================================================

    @Test
    void process_noPaths_doesNotThrow() {
        OpenAPI api = new OpenAPI();
        processor.process(api);   // must not throw
    }

    // ==========================================================================
    // No collisions
    // ==========================================================================

    @Test
    void process_uniqueOperationIds_remainUnchanged() {
        OpenAPI api = buildOpenAPI("getAll", "getById", "create");
        processor.process(api);
        assertEquals("getAll",   operationId(api, 0));
        assertEquals("getById",  operationId(api, 1));
        assertEquals("create",   operationId(api, 2));
    }

    // ==========================================================================
    // Duplicate — two occurrences
    // ==========================================================================

    @Test
    void process_duplicateOperationId_firstUnchangedSecondSuffixed() {
        OpenAPI api = buildOpenAPI("getById", "getById");
        processor.process(api);
        assertEquals("getById",   operationId(api, 0), "First occurrence must be unchanged");
        assertEquals("getById_1", operationId(api, 1), "Second occurrence must get _1 suffix");
    }

    // ==========================================================================
    // Triplicate — three occurrences
    // ==========================================================================

    @Test
    void process_triplicateOperationId_suffixedInOrder() {
        OpenAPI api = buildOpenAPI("op", "op", "op");
        processor.process(api);
        assertEquals("op",   operationId(api, 0));
        assertEquals("op_1", operationId(api, 1));
        assertEquals("op_2", operationId(api, 2));
    }

    // ==========================================================================
    // Mixed duplicates
    // ==========================================================================

    @Test
    void process_mixedOperationIds_onlyDuplicatesAreSuffixed() {
        OpenAPI api = buildOpenAPI("create", "getById", "delete", "getById");
        processor.process(api);
        assertEquals("create",   operationId(api, 0));
        assertEquals("getById",  operationId(api, 1));
        assertEquals("delete",   operationId(api, 2));
        assertEquals("getById_1", operationId(api, 3));
    }

    // ==========================================================================
    // Null operationId is skipped
    // ==========================================================================

    @Test
    void process_nullOperationId_isSkipped() {
        OpenAPI api = buildOpenAPI((String) null);
        processor.process(api);
        assertNull(operationId(api, 0), "Null operationId must remain null");
    }

    @Test
    void process_nullOperationIdMixed_doesNotAffectOthers() {
        OpenAPI api = buildOpenAPI("getAll", null, "getAll");
        processor.process(api);
        assertEquals("getAll",   operationId(api, 0));
        assertNull(operationId(api, 1));
        assertEquals("getAll_1", operationId(api, 2));
    }
}
