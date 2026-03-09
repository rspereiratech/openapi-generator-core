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

import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.Max;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link Max}.
 *
 * <p>Maps {@code @Max(n)} → {@code maximum: n}.
 *
 * @author ruispereira
 */
public class MaxConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Max;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        property.setMaximum(BigDecimal.valueOf(((Max) annotation).value()));
    }
}
