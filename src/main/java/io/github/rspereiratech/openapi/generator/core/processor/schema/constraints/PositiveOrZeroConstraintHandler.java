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
import jakarta.validation.constraints.PositiveOrZero;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link PositiveOrZero}.
 *
 * <p>Maps {@code @PositiveOrZero} → {@code minimum: 0}.
 *
 * @author ruispereira
 */
public class PositiveOrZeroConstraintHandler extends AbstractConstraintHandler<PositiveOrZero> {

    /** Creates a handler for {@link PositiveOrZero}. */
    public PositiveOrZeroConstraintHandler() { super(PositiveOrZero.class); }

    @Override
    protected void applyTyped(PositiveOrZero ann, Type fieldType, Schema<?> property) {
        property.setMinimum(BigDecimal.ZERO);
    }
}
