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
import jakarta.validation.constraints.Min;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link Min}.
 *
 * <p>Maps {@code @Min(n)} → {@code minimum: n}.
 *
 * @author ruispereira
 */
public class MinConstraintHandler extends AbstractConstraintHandler<Min> {

    /** Creates a handler for {@link Min}. */
    public MinConstraintHandler() { super(Min.class); }

    @Override
    protected void applyTyped(Min ann, Type fieldType, Schema<?> property) {
        property.setMinimum(BigDecimal.valueOf(ann.value()));
    }
}
