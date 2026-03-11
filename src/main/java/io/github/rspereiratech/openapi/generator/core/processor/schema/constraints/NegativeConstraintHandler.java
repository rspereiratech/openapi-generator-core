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
import jakarta.validation.constraints.Negative;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link Negative}.
 *
 * <p>Maps {@code @Negative} → {@code maximum: 0, exclusiveMaximum: true}.
 *
 * @author ruispereira
 */
public class NegativeConstraintHandler extends AbstractConstraintHandler<Negative> {

    /** Creates a handler for {@link Negative}. */
    public NegativeConstraintHandler() { super(Negative.class); }

    @Override
    protected void applyTyped(Negative ann, Type fieldType, Schema<?> property) {
        property.setMaximum(BigDecimal.ZERO);
        property.setExclusiveMaximum(true);
    }
}
