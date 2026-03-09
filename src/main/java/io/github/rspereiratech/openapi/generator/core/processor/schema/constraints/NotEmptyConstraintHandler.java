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
package io.github.rspereiratech.openapi.generator.core.processor.schema.constraints;

import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * {@link ConstraintHandler} for {@link NotEmpty}.
 *
 * <p>Maps {@code @NotEmpty} → {@code nullable: false}, and additionally:
 * <ul>
 *   <li>{@link java.util.Collection}, {@link java.util.Map}, or array →
 *       {@code minItems: 1}</li>
 *   <li>All other types (typically {@link String}) → {@code minLength: 1}</li>
 * </ul>
 *
 * @author ruispereira
 */
public class NotEmptyConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof NotEmpty;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        property.setNullable(false);
        boolean multiValued = TypeUtils.isCollection(fieldType)
                || TypeUtils.isMap(fieldType)
                || TypeUtils.isArray(fieldType);
        if (multiValued) property.setMinItems(1);
        else             property.setMinLength(1);
    }
}
