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

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PruneUnreferencedSchemasPostProcessorTest {

    private PruneUnreferencedSchemasPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PruneUnreferencedSchemasPostProcessor();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Builds an OpenAPI with the given schemas in components and one path whose
     *  GET 200 response body $ref's {@code referencedSchemaName}. */
    private OpenAPI buildWithRef(String referencedSchemaName, Map<String, Schema<?>> allSchemas) {
        OpenAPI api = new OpenAPI();

        Components components = new Components();
        allSchemas.forEach(components::addSchemas);
        api.setComponents(components);

        Schema<?> refSchema = new Schema<>().$ref("#/components/schemas/" + referencedSchemaName);
        MediaType mediaType = new MediaType().schema(refSchema);
        Content content = new Content().addMediaType("application/json", mediaType);
        ApiResponse response = new ApiResponse().description("OK").content(content);
        Operation op = new Operation().responses(new ApiResponses().addApiResponse("200", response));
        Paths paths = new Paths();
        paths.addPathItem("/test", new PathItem().get(op));
        api.setPaths(paths);

        return api;
    }

    // ==========================================================================
    // Preconditions
    // ==========================================================================

    @Test
    void process_nullOpenAPI_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    // ==========================================================================
    // Early exit — no components / no schemas
    // ==========================================================================

    @Test
    void process_noComponents_doesNotThrow() {
        OpenAPI api = new OpenAPI();
        processor.process(api);  // must not throw
    }

    @Test
    void process_nullSchemas_doesNotThrow() {
        OpenAPI api = new OpenAPI();
        api.setComponents(new Components());  // components present but schemas null
        processor.process(api);               // must not throw
    }

    // ==========================================================================
    // Referenced schema is kept
    // ==========================================================================

    @Test
    void process_referencedSchema_isRetained() {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("TenantDTO", new Schema<>());
        OpenAPI api = buildWithRef("TenantDTO", schemas);

        processor.process(api);

        assertTrue(api.getComponents().getSchemas().containsKey("TenantDTO"),
                "A referenced schema must not be pruned");
    }

    // ==========================================================================
    // Unreferenced schema is removed
    // ==========================================================================

    @Test
    void process_unreferencedSchema_isRemoved() {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("TenantDTO", new Schema<>());
        schemas.put("Orphan",    new Schema<>());
        OpenAPI api = buildWithRef("TenantDTO", schemas);

        processor.process(api);

        assertFalse(api.getComponents().getSchemas().containsKey("Orphan"),
                "Unreferenced schema must be pruned");
        assertTrue(api.getComponents().getSchemas().containsKey("TenantDTO"),
                "Referenced schema must be retained");
    }

    // ==========================================================================
    // Multiple unreferenced schemas
    // ==========================================================================

    @Test
    void process_multipleUnreferencedSchemas_allRemoved() {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("UsedDTO",   new Schema<>());
        schemas.put("OrphanA",   new Schema<>());
        schemas.put("OrphanB",   new Schema<>());
        OpenAPI api = buildWithRef("UsedDTO", schemas);

        processor.process(api);

        assertFalse(api.getComponents().getSchemas().containsKey("OrphanA"));
        assertFalse(api.getComponents().getSchemas().containsKey("OrphanB"));
        assertTrue(api.getComponents().getSchemas().containsKey("UsedDTO"));
    }

    // ==========================================================================
    // Schema referenced by another schema (transitive $ref)
    // ==========================================================================

    @Test
    void process_schemaReferencedInsideAnotherSchema_isKept() {
        // PageDTO has a property "content" whose items $ref AgentDTO.
        // The path $refs PageDTO. AgentDTO is only $ref'd inside PageDTO's schema definition.
        Schema<?> agentRefSchema = new Schema<>().$ref("#/components/schemas/AgentDTO");
        Schema<?> pageSchema = new Schema<>().addProperty("content", agentRefSchema);
        Schema<?> agentSchema = new Schema<>();

        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("PageDTO",  pageSchema);
        schemas.put("AgentDTO", agentSchema);
        OpenAPI api = buildWithRef("PageDTO", schemas);

        processor.process(api);

        assertTrue(api.getComponents().getSchemas().containsKey("PageDTO"),
                "Top-level referenced schema must be kept");
        assertTrue(api.getComponents().getSchemas().containsKey("AgentDTO"),
                "Schema referenced transitively inside another schema must be kept");
    }

    // ==========================================================================
    // All schemas unreferenced
    // ==========================================================================

    @Test
    void process_noRefsAnywhere_allSchemasRemoved() {
        OpenAPI api = new OpenAPI();
        Components components = new Components();
        components.addSchemas("OrphanA", new Schema<>());
        components.addSchemas("OrphanB", new Schema<>());
        api.setComponents(components);
        // No paths — nothing references these schemas

        processor.process(api);

        assertTrue(api.getComponents().getSchemas().isEmpty(),
                "All schemas must be pruned when nothing references them");
    }
}
