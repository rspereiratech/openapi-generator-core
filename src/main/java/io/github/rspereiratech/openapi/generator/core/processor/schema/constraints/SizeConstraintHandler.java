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

import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * {@link ConstraintHandler} for {@link Size}.
 *
 * <p>Maps {@code @Size} differently depending on the field type:
 * <ul>
 *   <li>{@link java.util.Collection}, {@link java.util.Map}, or array →
 *       {@code minItems} / {@code maxItems}</li>
 *   <li>All other types (typically {@link String}) →
 *       {@code minLength} / {@code maxLength}</li>
 * </ul>
 *
 * <p>Boundary values that carry no information are suppressed:
 * {@code min = 0} (the default) is not written, and {@code max = Integer.MAX_VALUE}
 * (the default) is not written.
 *
 * @author ruispereira
 */
public class SizeConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Size;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        Size size = (Size) annotation;
        boolean multiValued = TypeUtils.isCollection(fieldType)
                || TypeUtils.isMap(fieldType)
                || TypeUtils.isArray(fieldType);

        Consumer<Integer> setMin = multiValued ? property::setMinItems : property::setMinLength;
        Consumer<Integer> setMax = multiValued ? property::setMaxItems : property::setMaxLength;

        if (size.min() > 0)                setMin.accept(size.min());
        if (size.max() < Integer.MAX_VALUE) setMax.accept(size.max());
    }
}
