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
import jakarta.validation.constraints.Pattern;

import java.lang.reflect.Type;

/**
 * {@link ConstraintHandler} for {@link Pattern}.
 *
 * <p>Maps {@code @Pattern(regexp)} → {@code pattern: regexp}.
 *
 * @author ruispereira
 */
public class PatternConstraintHandler extends AbstractConstraintHandler<Pattern> {

    /** Creates a handler for {@link Pattern}. */
    public PatternConstraintHandler() { super(Pattern.class); }

    @Override
    protected void applyTyped(Pattern ann, Type fieldType, Schema<?> property) {
        property.setPattern(ann.regexp());
    }
}
