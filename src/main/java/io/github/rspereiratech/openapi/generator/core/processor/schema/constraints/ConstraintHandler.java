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

import io.github.rspereiratech.openapi.generator.core.processor.schema.ValidationSchemaEnricher;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Strategy element in the Chain of Responsibility used by
 * {@link ValidationSchemaEnricher} to map a single Jakarta Bean Validation
 * annotation to its OpenAPI schema equivalent.
 *
 * <p>Handlers are tried in order; the first one whose {@link #supports} method returns
 * {@code true} is responsible for mutating the property schema via {@link #apply}.
 * Unrecognised annotations are skipped silently when no handler matches.
 *
 * <p>This design allows the constraint mapping to be extended without modifying
 * {@link ValidationSchemaEnricher}: pass a custom handler list to
 * {@link ValidationSchemaEnricher#ValidationSchemaEnricher(java.util.List)}.
 *
 * @author ruispereira
 * @see ValidationSchemaEnricher
 */
@SuppressWarnings("java:S1452") // Schema<?> is intentional
public interface ConstraintHandler {

    /**
     * Returns {@code true} if this handler can process the given annotation.
     *
     * @param annotation the annotation to test; must not be {@code null}
     * @return {@code true} if this handler should be used to apply {@code annotation}
     */
    boolean supports(Annotation annotation);

    /**
     * Applies the constraint represented by {@code annotation} to {@code property}.
     *
     * <p>Called only when {@link #supports} has already returned {@code true} for the
     * same annotation instance. Implementations may safely cast {@code annotation} to
     * their specific annotation type.
     *
     * @param annotation the constraint annotation to apply; must not be {@code null}
     * @param fieldType  the declared type of the annotated field; used by handlers that
     *                   need to distinguish string-like from multi-valued types
     *                   (e.g. {@code @Size}, {@code @NotEmpty})
     * @param property   the OpenAPI schema property to mutate; must not be {@code null}
     */
    void apply(Annotation annotation, Type fieldType, Schema<?> property);
}
