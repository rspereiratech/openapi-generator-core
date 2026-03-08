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
package io.github.rspereiratech.openapi.generator.core.processor.schema.handlers;

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessorImpl;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;

/**
 * Strategy element in the Chain of Responsibility used by
 * {@link io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessorImpl}
 * to convert Java types to OpenAPI {@link Schema}s.
 *
 * <p>Handlers are tried in order; the first one whose {@link #supports} method
 * returns {@code true} is responsible for resolving the type via {@link #resolve}.
 * A catch-all handler (e.g. {@link ModelConvertersTypeSchemaHandler}) should
 * always be placed last in the chain.</p>
 *
 * <p>This design allows the schema processor to support multiple type categories
 * (primitives, collections, enums, complex objects, generics) in a flexible
 * and extensible way.</p>
 *
 * @author ruispereira
 * @see SchemaProcessor
 * @see SchemaProcessorImpl
 */
@SuppressWarnings("java:S1452") // Schema<?> is intentional: the schema value type is unknown at this abstraction level
public interface TypeSchemaHandler {

    /**
     * Returns {@code true} if this handler can convert the given (already-unwrapped) type.
     *
     * @param type the unwrapped Java type to test; must not be {@code null}
     * @return {@code true} if this handler should be used to resolve {@code type}
     */
    boolean supports(Type type);

    /**
     * Converts the given type to an OpenAPI {@link Schema}.
     *
     * <p>Handlers that produce composite schemas (e.g. {@code PageXxxDTO}) may
     * call back into the supplied {@code schemaProcessor} to recursively resolve
     * inner types and may mutate the schema registry via
     * {@code schemaProcessor.getSchemaRegistry()}.</p>
     *
     * @param type            the unwrapped Java type to convert; must not be {@code null}
     * @param schemaProcessor the owning processor used for recursive resolution and
     *                        schema registry access; must not be {@code null}
     * @return the resolved {@link Schema}, or {@code null} if the type maps to void/no-content
     */
    Schema<?> resolve(Type type, SchemaProcessor schemaProcessor);
}
