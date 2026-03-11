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
import jakarta.validation.constraints.NotBlank;

import java.lang.reflect.Type;

/**
 * {@link ConstraintHandler} for {@link NotBlank}.
 *
 * <p>Maps {@code @NotBlank} → {@code nullable: false, minLength: 1}.
 *
 * @author ruispereira
 */
public class NotBlankConstraintHandler extends AbstractConstraintHandler<NotBlank> {

    /** Creates a handler for {@link NotBlank}. */
    public NotBlankConstraintHandler() { super(NotBlank.class); }

    @Override
    protected void applyTyped(NotBlank ann, Type fieldType, Schema<?> property) {
        property.setNullable(false);
        property.setMinLength(1);
    }
}
