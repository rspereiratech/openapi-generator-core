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

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Type-safe base for {@link ConstraintHandler} implementations.
 *
 * <p>Eliminates the boilerplate that would otherwise appear in every concrete handler:
 * <ul>
 *   <li>the {@link #supports} check ({@code annotation instanceof A}) is implemented once
 *       via {@link Class#isInstance};</li>
 *   <li>the unsafe cast of the raw {@link Annotation} to {@code A} is encapsulated in
 *       {@link #apply}, which delegates to the type-safe {@link #applyTyped}.</li>
 * </ul>
 *
 * <p>Concrete handlers only need to:
 * <ol>
 *   <li>Declare their annotation type {@code A}.</li>
 *   <li>Pass {@code A.class} to the super constructor.</li>
 *   <li>Implement {@link #applyTyped(Annotation, Type, Schema)} with the correct
 *       annotation type — no cast required.</li>
 * </ol>
 *
 * @param <A> the Jakarta Bean Validation annotation type this handler processes
 * @author ruispereira
 * @see ConstraintHandler
 */
@SuppressWarnings("java:S1452") // Schema<?> wildcard is intentional
public abstract class AbstractConstraintHandler<A extends Annotation> implements ConstraintHandler {

    /** The annotation type this handler is responsible for. */
    private final Class<A> annotationType;

    /**
     * Creates a handler bound to the given annotation type.
     *
     * @param annotationType the annotation class this handler processes; must not be {@code null}
     * @throws NullPointerException if {@code annotationType} is {@code null}
     */
    protected AbstractConstraintHandler(Class<A> annotationType) {
        this.annotationType = Preconditions.checkNotNull(annotationType, "annotationType must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} when {@code annotation} is an instance of the annotation type
     * passed to the constructor.
     */
    @Override
    public final boolean supports(Annotation annotation) {
        return annotationType.isInstance(annotation);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Casts {@code annotation} to {@code A} and delegates to
     * {@link #applyTyped(Annotation, Type, Schema)}.
     * Only called when {@link #supports} has already returned {@code true}.
     */
    @Override
    public final void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        applyTyped(annotationType.cast(annotation), fieldType, property);
    }

    /**
     * Applies the constraint represented by {@code annotation} to {@code property}.
     *
     * <p>The annotation is already cast to {@code A} — no further casting is needed.
     * Called only after {@link #supports} has returned {@code true}.
     *
     * @param annotation the constraint annotation, already cast to {@code A}; never {@code null}
     * @param fieldType  the declared type of the annotated field
     * @param property   the OpenAPI schema property to mutate; never {@code null}
     */
    protected abstract void applyTyped(A annotation, Type fieldType, Schema<?> property);
}
