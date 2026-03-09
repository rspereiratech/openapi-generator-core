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
import jakarta.validation.constraints.DecimalMin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link DecimalMin}.
 *
 * <p>Maps {@code @DecimalMin(value)} → {@code minimum: value}.
 * When {@code inclusive = false}, also sets {@code exclusiveMinimum: true}.
 *
 * @author ruispereira
 */
public class DecimalMinConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof DecimalMin;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        DecimalMin dMin = (DecimalMin) annotation;
        property.setMinimum(new BigDecimal(dMin.value()));
        if (!dMin.inclusive()) property.setExclusiveMinimum(true);
    }
}
