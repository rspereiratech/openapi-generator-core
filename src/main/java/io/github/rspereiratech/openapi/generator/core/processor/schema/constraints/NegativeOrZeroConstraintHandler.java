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
import jakarta.validation.constraints.NegativeOrZero;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * {@link ConstraintHandler} for {@link NegativeOrZero}.
 *
 * <p>Maps {@code @NegativeOrZero} → {@code maximum: 0}.
 *
 * @author ruispereira
 */
public class NegativeOrZeroConstraintHandler extends AbstractConstraintHandler<NegativeOrZero> {

    /** Creates a handler for {@link NegativeOrZero}. */
    public NegativeOrZeroConstraintHandler() { super(NegativeOrZero.class); }

    @Override
    protected void applyTyped(NegativeOrZero ann, Type fieldType, Schema<?> property) {
        property.setMaximum(BigDecimal.ZERO);
    }
}
