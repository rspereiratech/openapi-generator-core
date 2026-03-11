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
import jakarta.validation.constraints.Email;

import java.lang.reflect.Type;

/**
 * {@link ConstraintHandler} for {@link Email}.
 *
 * <p>Maps {@code @Email} → {@code format: email}.
 *
 * @author ruispereira
 */
public class EmailConstraintHandler extends AbstractConstraintHandler<Email> {

    /** Creates a handler for {@link Email}. */
    public EmailConstraintHandler() { super(Email.class); }

    @Override
    protected void applyTyped(Email ann, Type fieldType, Schema<?> property) {
        property.setFormat("email");
    }
}
