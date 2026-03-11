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
public class DecimalMinConstraintHandler extends AbstractConstraintHandler<DecimalMin> {

    /** Creates a handler for {@link DecimalMin}. */
    public DecimalMinConstraintHandler() { super(DecimalMin.class); }

    @Override
    protected void applyTyped(DecimalMin ann, Type fieldType, Schema<?> property) {
        property.setMinimum(new BigDecimal(ann.value()));
        if (!ann.inclusive()) property.setExclusiveMinimum(true);
    }
}
