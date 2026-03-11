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
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;

/**
 * {@link TypeSchemaHandler} for {@code void} / {@code Void} return types.
 *
 * <p>Returns {@code null}, which signals to callers (e.g. response builders) that
 * no response body schema should be emitted.
 *
 * @author ruispereira
 */
@SuppressWarnings("java:S1452")
public class VoidTypeSchemaHandler implements TypeSchemaHandler {

    /**
     * Returns {@code true} if {@code type} represents a void or {@code Void} type.
     *
     * @param type the type to test; must not be {@code null}
     * @return {@code true} for {@code void} / {@code Void}; {@code false} otherwise
     */
    @Override
    public boolean supports(Type type) {
        return TypeUtils.isVoid(type);
    }

    /**
     * Returns {@code null} to signal that no response body schema should be emitted
     * for {@code void} / {@code Void} return types.
     *
     * @param type            the void type (not used in the implementation)
     * @param schemaProcessor the schema processor (not used in the implementation)
     * @return {@code null} always
     */
    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        return null;
    }
}
