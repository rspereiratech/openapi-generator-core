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
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * {@link TypeSchemaHandler} for Project Reactor {@code Flux<T>} types.
 *
 * <p>Maps {@code Flux<T>} to an OpenAPI array schema whose item schema is derived
 * from {@code T} by recursively delegating back to the owning {@link SchemaProcessor}.
 *
 * @author ruispereira
 */
@SuppressWarnings("java:S1452")
public class FluxTypeSchemaHandler implements TypeSchemaHandler {

    /**
     * Returns {@code true} if {@code type} is a parameterized {@code reactor.core.publisher.Flux}.
     *
     * @param type the type to test; must not be {@code null}
     * @return {@code true} when {@code type} is {@code Flux<?>}; {@code false} otherwise
     */
    @Override
    @SuppressWarnings("java:S1872")
    public boolean supports(Type type) {
        if (!(type instanceof ParameterizedType pt)) return false;
        Type raw = pt.getRawType();
        return raw instanceof Class<?> c && "reactor.core.publisher.Flux".equals(c.getName());
    }

    /**
     * Converts a {@code Flux<T>} type to an OpenAPI array schema whose {@code items}
     * schema is derived from {@code T} by delegating to {@code schemaProcessor}.
     *
     * @param type            the {@code Flux<T>} type; must be a parameterized type
     * @param schemaProcessor the processor used to resolve the item type {@code T}
     * @return an {@link ArraySchema} wrapping the schema for {@code T}
     */
    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        Type itemType = TypeUtils.firstTypeArgument(type);
        ArraySchema array = new ArraySchema();
        array.setItems(schemaProcessor.toSchema(itemType));
        return array;
    }
}
