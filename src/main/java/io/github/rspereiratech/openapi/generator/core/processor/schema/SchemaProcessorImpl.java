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
package io.github.rspereiratech.openapi.generator.core.processor.schema;

import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.FluxTypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.ModelConvertersTypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.PageTypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.PageableTypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.TypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.handlers.VoidTypeSchemaHandler;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link SchemaProcessor} implementation.
 *
 * <p>Converts Java types to OpenAPI {@link Schema} objects by walking an ordered
 * Chain of Responsibility of {@link TypeSchemaHandler}s. The first handler whose
 * {@link TypeSchemaHandler#supports} method returns {@code true} is used to
 * {@link TypeSchemaHandler#resolve resolve} the type; a catch-all
 * {@link ModelConvertersTypeSchemaHandler} delegates to the Swagger
 * {@code ModelConverters} engine as a final fallback.
 *
 * <p>Transparent wrapper types ({@code ResponseEntity<T>}, {@code Optional<T>},
 * {@code Mono<T>}) are unwrapped by {@link TypeUtils#unwrapType(Type)} before
 * the chain is consulted.
 *
 * <p>Complex object schemas are stored in a registry keyed by schema name so they
 * can be emitted under {@code components/schemas} and referenced via {@code $ref}.
 * A single instance of this class should be shared across all processors during
 * one generation run so that the registry accumulates every component schema
 * discovered while processing all controllers.
 *
 * <p>To extend the set of handled types without modifying this class, pass a custom
 * handler list to {@link #SchemaProcessorImpl(List)}.
 *
 * @author ruispereira
 */
@Slf4j
@SuppressWarnings("java:S1452") // Schema<?> is intentional: the schema value type is unknown at this abstraction level
public class SchemaProcessorImpl implements SchemaProcessor {

    /** Accumulates all component schemas discovered during a single generation run. */
    private final Map<String, Schema<?>> schemaRegistry = new LinkedHashMap<>();

    /** Ordered chain of handlers consulted in sequence; the first match wins. */
    private final List<TypeSchemaHandler> handlers;

    /** Creates an instance with the default handler chain. */
    public SchemaProcessorImpl() {
        this(List.of(
                new VoidTypeSchemaHandler(),
                new FluxTypeSchemaHandler(),
                new PageTypeSchemaHandler(),
                new PageableTypeSchemaHandler(),
                new ModelConvertersTypeSchemaHandler()
        ));
    }

    /**
     * Creates an instance with a custom handler chain.
     *
     * <p>Handlers are tried in list order; the first matching handler wins.
     * The caller is responsible for including a catch-all handler (e.g.
     * {@link ModelConvertersTypeSchemaHandler}) as the last element.
     *
     * @param handlers ordered list of type-schema handlers; must not be null or empty
     * @throws NullPointerException     if {@code handlers} is null
     * @throws IllegalArgumentException if {@code handlers} is empty
     */
    public SchemaProcessorImpl(List<TypeSchemaHandler> handlers) {
        Preconditions.checkNotNull(handlers,"'handlers' must not be null");
        Preconditions.checkArgument(!handlers.isEmpty(),"'handlers' must not be empty");
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public Schema<?> toSchema(Type type) {
        Preconditions.checkNotNull(type,"'type' must not be null");

        Type unwrapped = TypeUtils.unwrapType(type);
        for (TypeSchemaHandler h : handlers) {
            if (h.supports(unwrapped)) {
                return h.resolve(unwrapped, this);
            }
        }
        log.warn("No handler matched type '{}' — returning null schema", type.getTypeName());
        return null;
    }

    @Override
    public Map<String, Schema<?>> getSchemaRegistry() {
        return schemaRegistry;
    }
}
