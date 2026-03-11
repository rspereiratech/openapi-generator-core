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
import jakarta.validation.constraints.NotNull;

import java.lang.reflect.Type;

/**
 * {@link ConstraintHandler} for {@link NotNull}.
 *
 * <p>Maps {@code @NotNull} → {@code nullable: false}.
 *
 * @author ruispereira
 */
public class NotNullConstraintHandler extends AbstractConstraintHandler<NotNull> {

    /** Creates a handler for {@link NotNull}. */
    public NotNullConstraintHandler() { super(NotNull.class); }

    @Override
    protected void applyTyped(NotNull ann, Type fieldType, Schema<?> property) {
        property.setNullable(false);
    }
}
