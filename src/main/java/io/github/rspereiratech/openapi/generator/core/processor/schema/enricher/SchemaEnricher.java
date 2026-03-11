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
package io.github.rspereiratech.openapi.generator.core.processor.schema.enricher;

import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Strategy for enriching OpenAPI component schemas after initial resolution by
 * {@link io.swagger.v3.core.converter.ModelConverters}.
 *
 * <p>Enrichers are called with the root type being resolved and the full map of
 * component schemas discovered during that resolution. Each enricher is free to
 * mutate the schemas in-place (e.g. set {@code description}, apply constraint
 * keywords, mark fields as {@code nullable}).
 *
 * <p>Multiple enrichers can be composed by passing them in order to
 * {@link io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.ModelConvertersTypeSchemaHandler}.
 * Each enricher is applied in sequence; later enrichers see the schemas as already
 * mutated by earlier ones.
 *
 * @author ruispereira
 * @see ValidationSchemaEnricher
 * @see SchemaAnnotationEnricher
 */
@SuppressWarnings("java:S1452")
public interface SchemaEnricher {

    /**
     * Enriches the component schemas discovered while resolving {@code type}.
     *
     * @param type    the root type that triggered schema resolution; never {@code null}
     * @param schemas mutable map of schema-name → schema; enrichers may mutate values in-place
     */
    void apply(Type type, Map<String, Schema<?>> schemas);
}
