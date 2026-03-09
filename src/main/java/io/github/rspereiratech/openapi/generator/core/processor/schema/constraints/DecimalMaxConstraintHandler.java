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
import jakarta.validation.constraints.DecimalMax;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link DecimalMax}.
 *
 * <p>Maps {@code @DecimalMax(value)} → {@code maximum: value}.
 * When {@code inclusive = false}, also sets {@code exclusiveMaximum: true}.
 *
 * @author ruispereira
 */
public class DecimalMaxConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof DecimalMax;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        DecimalMax dMax = (DecimalMax) annotation;
        property.setMaximum(new BigDecimal(dMax.value()));
        if (!dMax.inclusive()) property.setExclusiveMaximum(true);
    }
}
