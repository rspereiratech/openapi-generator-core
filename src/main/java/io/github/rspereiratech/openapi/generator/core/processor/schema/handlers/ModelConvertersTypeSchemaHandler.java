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
package io.github.rspereiratech.openapi.generator.core.processor.schema.handlers;

import com.google.common.base.Preconditions;
import io.github.rspereiratech.openapi.generator.core.processor.schema.ValidationSchemaEnricher;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

/**
 * Catch-all {@link TypeSchemaHandler} that delegates to Swagger's
 * {@link ModelConverters} engine — the same engine used by SpringDoc at runtime.
 *
 * <p>This handler always returns {@code true} from {@link #supports} and must be
 * placed last in the chain so that more specific handlers are tried first.
 *
 * <p>Any referenced component schemas discovered during resolution are registered
 * in the schema registry via {@link SchemaProcessor#getSchemaRegistry()}.
 * Bean Validation constraints are then propagated to those schemas via the
 * supplied {@link ValidationSchemaEnricher}.
 *
 * @author ruispereira
 */
@Slf4j
@SuppressWarnings("java:S1452")
public class ModelConvertersTypeSchemaHandler implements TypeSchemaHandler {

    private final ValidationSchemaEnricher constraintApplier;

    /**
     * Creates an instance with the default {@link ValidationSchemaEnricher}.
     */
    public ModelConvertersTypeSchemaHandler() {
        this(new ValidationSchemaEnricher());
    }

    /**
     * Creates an instance with a custom {@link ValidationSchemaEnricher}.
     *
     * @param enricher the enricher to use for Bean Validation propagation;
     *                 must not be {@code null}
     * @throws NullPointerException if {@code enricher} is {@code null}
     */
    public ModelConvertersTypeSchemaHandler(ValidationSchemaEnricher enricher) {
        Preconditions.checkNotNull(enricher, "'enricher' must not be null");
        this.constraintApplier = enricher;
    }

    @Override
    public boolean supports(Type type) {
        return true; // catch-all fallback — must be last in the chain
    }

    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        try {
            AnnotatedType annotatedType = new AnnotatedType(type).resolveAsRef(true);
            ResolvedSchema resolved = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(annotatedType);

            if (resolved == null) {
                log.warn("ModelConverters could not resolve type: {}", type);
                return new Schema<>();
            }

            if (resolved.referencedSchemas != null) {
                constraintApplier.apply(type, resolved.referencedSchemas);
                resolved.referencedSchemas.forEach((name, schema) ->
                        schemaProcessor.getSchemaRegistry().merge(
                                name, schema, (existing, incoming) -> existing));
            }

            return resolved.schema;

        } catch (Exception e) {
            log.debug("Schema resolution failed for type {}: {}", type, e.getMessage());
            return new Schema<>();
        }
    }
}
