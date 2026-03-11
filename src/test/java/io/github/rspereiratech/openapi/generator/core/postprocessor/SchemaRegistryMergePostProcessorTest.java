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

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchemaRegistryMergePostProcessor}.
 *
 * <p>Verifies that schemas accumulated in the shared registry during processing are
 * merged into {@code components/schemas}, that pre-existing components are preserved,
 * and that an empty registry leaves the model unchanged.
 */
@ExtendWith(MockitoExtension.class)
class SchemaRegistryMergePostProcessorTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private SchemaRegistryMergePostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SchemaRegistryMergePostProcessor(schemaProcessor);
    }

    // ==========================================================================
    // Preconditions
    // ==========================================================================

    @Test
    void process_nullOpenAPI_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    // ==========================================================================
    // Empty registry — early exit
    // ==========================================================================

    @Test
    void process_emptyRegistry_doesNotCreateComponents() {
        when(schemaProcessor.getSchemaRegistry()).thenReturn(Map.of());
        OpenAPI api = new OpenAPI();

        processor.process(api);

        assertNull(api.getComponents(), "Components must not be created when the registry is empty");
    }

    // ==========================================================================
    // No existing components
    // ==========================================================================

    @Test
    void process_noExistingComponents_createsComponentsAndAddsSchemas() {
        Map<String, Schema<?>> registry = new LinkedHashMap<>();
        registry.put("TenantDTO", new Schema<>());
        when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);

        OpenAPI api = new OpenAPI();
        processor.process(api);

        assertAll(
                () -> assertNotNull(api.getComponents(), "Components must be created"),
                () -> assertNotNull(api.getComponents().getSchemas()),
                () -> assertTrue(api.getComponents().getSchemas().containsKey("TenantDTO"))
        );
    }

    // ==========================================================================
    // Existing components
    // ==========================================================================

    @Test
    void process_existingComponents_mergesIntoExisting() {
        Map<String, Schema<?>> registry = new LinkedHashMap<>();
        registry.put("NewSchema", new Schema<>());
        when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);

        OpenAPI api = new OpenAPI();
        Components existing = new Components();
        existing.addSchemas("OldSchema", new Schema<>());
        api.setComponents(existing);

        processor.process(api);

        assertAll(
                () -> assertTrue(api.getComponents().getSchemas().containsKey("OldSchema"),
                        "Pre-existing schema must be preserved"),
                () -> assertTrue(api.getComponents().getSchemas().containsKey("NewSchema"),
                        "Registry schema must be merged in")
        );
    }

    // ==========================================================================
    // Multiple schemas
    // ==========================================================================

    @Test
    void process_multipleSchemas_allMerged() {
        Map<String, Schema<?>> registry = new LinkedHashMap<>();
        registry.put("SchemaA", new Schema<>());
        registry.put("SchemaB", new Schema<>());
        registry.put("SchemaC", new Schema<>());
        when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);

        OpenAPI api = new OpenAPI();
        processor.process(api);

        assertAll(
                () -> assertTrue(api.getComponents().getSchemas().containsKey("SchemaA")),
                () -> assertTrue(api.getComponents().getSchemas().containsKey("SchemaB")),
                () -> assertTrue(api.getComponents().getSchemas().containsKey("SchemaC"))
        );
    }
}
