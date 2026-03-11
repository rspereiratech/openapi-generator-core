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
public class DecimalMaxConstraintHandler extends AbstractConstraintHandler<DecimalMax> {

    /** Creates a handler for {@link DecimalMax}. */
    public DecimalMaxConstraintHandler() { super(DecimalMax.class); }

    @Override
    protected void applyTyped(DecimalMax ann, Type fieldType, Schema<?> property) {
        property.setMaximum(new BigDecimal(ann.value()));
        if (!ann.inclusive()) property.setExclusiveMaximum(true);
    }
}
