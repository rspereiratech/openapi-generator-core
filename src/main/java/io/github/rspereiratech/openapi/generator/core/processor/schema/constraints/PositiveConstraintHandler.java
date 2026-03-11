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
import jakarta.validation.constraints.Positive;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link Positive}.
 *
 * <p>Maps {@code @Positive} → {@code minimum: 0, exclusiveMinimum: true}.
 *
 * @author ruispereira
 */
public class PositiveConstraintHandler extends AbstractConstraintHandler<Positive> {

    /** Creates a handler for {@link Positive}. */
    public PositiveConstraintHandler() { super(Positive.class); }

    @Override
    protected void applyTyped(Positive ann, Type fieldType, Schema<?> property) {
        property.setMinimum(BigDecimal.ZERO);
        property.setExclusiveMinimum(true);
    }
}
