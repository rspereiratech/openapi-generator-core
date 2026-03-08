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

import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/**
 * Contract for schema processing: converts Java {@link Type}s to OpenAPI {@link Schema}s
 * and provides access to the accumulated component schema registry.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *     <li>Resolving simple and complex Java types to their OpenAPI schema representation</li>
 *     <li>Registering reusable component schemas internally and returning {@code $ref} schemas when appropriate</li>
 *     <li>Providing access to the accumulated component schema registry</li>
 * </ul>
 * </p>
 *
 * <p>Generic type variables can be resolved using a type-variable map before schema generation.</p>
 *
 * @author ruispereira
 * @see SchemaProcessorImpl
 * @see io.swagger.v3.oas.models.media.Schema
 */
@SuppressWarnings("java:S1452") // Schema<?> is intentional: the schema value type is unknown at this abstraction level
public interface SchemaProcessor {

    /**
     * Resolves a Java {@link Type} to its OpenAPI {@link Schema}.
     *
     * <p>Complex object types may be registered as component schemas internally
     * and returned as {@code $ref} schemas.</p>
     *
     * @param type the Java type to convert; may be a generic type
     * @return the OpenAPI schema, or {@code null} if the type is {@code void}/{@code Void}
     */
    Schema<?> toSchema(Type type);

    /**
     * Resolves a Java {@link Type} to its OpenAPI {@link Schema}, substituting
     * any {@link TypeVariable}s found in {@code type} using {@code typeVarMap}
     * before delegating to {@link #toSchema(Type)}.
     *
     * @param type       the Java type to convert
     * @param typeVarMap mapping of type variables to concrete types; may be empty
     * @return the OpenAPI schema, or {@code null} if the type is {@code void}/{@code Void}
     */
    default Schema<?> toSchema(Type type, Map<TypeVariable<?>, Type> typeVarMap) {
        return toSchema(TypeUtils.resolveType(type, typeVarMap));
    }

    /**
     * Returns all component schemas accumulated so far.
     *
     * <p>The returned map should be merged into {@code OpenAPI.components.schemas}
     * after generation is complete.</p>
     *
     * <p>The default implementation returns an empty map; override to provide
     * an accumulating registry, as {@link SchemaProcessorImpl} does.</p>
     *
     * @return a map of schema name to {@link Schema}; never {@code null}
     */
    default Map<String, Schema<?>> getSchemaRegistry() {
        return Map.of();
    }
}
