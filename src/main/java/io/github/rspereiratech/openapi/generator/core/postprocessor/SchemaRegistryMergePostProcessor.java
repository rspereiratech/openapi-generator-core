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

import com.google.common.base.Preconditions;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Merges all component schemas collected by the {@link SchemaProcessor} during
 * controller processing into the {@link OpenAPI} model's {@code components/schemas} section.
 *
 * @author ruispereira
 */
@Slf4j
public class SchemaRegistryMergePostProcessor implements PostProcessor {

    private final SchemaProcessor schemaProcessor;

    /**
     * Creates a new post-processor backed by the given schema processor.
     *
     * @param schemaProcessor the processor whose registry will be merged into the OpenAPI model;
     *                        must not be {@code null}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    public SchemaRegistryMergePostProcessor(SchemaProcessor schemaProcessor) {
        this.schemaProcessor = schemaProcessor;
    }

    /**
     * Copies every schema from the {@link SchemaProcessor} registry into
     * {@code openAPI.components.schemas}, creating the {@code Components} section
     * if it does not yet exist.
     *
     * <p>This method is a no-op when the registry is empty.</p>
     *
     * @param openAPI the OpenAPI model to enrich in-place; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        Map<String, Schema<?>> schemas = schemaProcessor.getSchemaRegistry();
        if (schemas.isEmpty()) return;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        schemas.forEach(openAPI.getComponents()::addSchemas);
        log.info("{} component schema(s) registered", schemas.size());
    }
}
