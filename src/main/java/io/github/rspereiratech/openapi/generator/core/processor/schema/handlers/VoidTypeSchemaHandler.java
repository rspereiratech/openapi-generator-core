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

    @Override
    public boolean supports(Type type) {
        return TypeUtils.isVoid(type);
    }

    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        return null;
    }
}
