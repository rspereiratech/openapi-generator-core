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
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.schema.enricher.SchemaAnnotationEnricher;
import io.github.rspereiratech.openapi.generator.core.processor.schema.enricher.SchemaEnricher;
import io.github.rspereiratech.openapi.generator.core.processor.schema.enricher.ValidationSchemaEnricher;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Catch-all {@link TypeSchemaHandler} that delegates to Swagger's
 * {@link ModelConverters} engine — the same engine used by SpringDoc at runtime.
 *
 * <p>This handler always returns {@code true} from {@link #supports} and must be
 * placed last in the chain so that more specific handlers are tried first.
 *
 * <p>Any referenced component schemas discovered during resolution are registered
 * in the schema registry via {@link SchemaProcessor#getSchemaRegistry()}.
 * A configurable chain of {@link SchemaEnricher}s is then applied to those schemas
 * in order. The default chain consists of:
 * <ol>
 *   <li>{@link ValidationSchemaEnricher} — propagates Jakarta Bean Validation constraints</li>
 *   <li>{@link SchemaAnnotationEnricher} — propagates {@code @Schema} metadata
 *       (description, title, example, deprecated)</li>
 * </ol>
 *
 * <p>A custom enricher list can be supplied via
 * {@link #ModelConvertersTypeSchemaHandler(List)} for full control over enrichment
 * behaviour (e.g. adding Hibernate Validator support).
 *
 * @author ruispereira
 * @see SchemaEnricher
 * @see ValidationSchemaEnricher
 * @see SchemaAnnotationEnricher
 */
@Slf4j
@SuppressWarnings("java:S1452")
public class ModelConvertersTypeSchemaHandler implements TypeSchemaHandler {

    /** Ordered chain of enrichers applied to resolved schemas. */
    private final List<SchemaEnricher> enrichers;

    /**
     * Creates an instance with the default enricher chain:
     * {@link ValidationSchemaEnricher} followed by {@link SchemaAnnotationEnricher}.
     */
    public ModelConvertersTypeSchemaHandler() {
        this(List.of(new ValidationSchemaEnricher(), new SchemaAnnotationEnricher()));
    }

    /**
     * Creates an instance with a custom enricher chain.
     *
     * <p>Enrichers are applied in list order; each enricher sees the schemas as
     * already mutated by the previous ones.
     *
     * @param enrichers ordered list of schema enrichers; must not be {@code null} or empty
     * @throws NullPointerException     if {@code enrichers} is {@code null}
     * @throws IllegalArgumentException if {@code enrichers} is empty
     */
    public ModelConvertersTypeSchemaHandler(List<SchemaEnricher> enrichers) {
        Preconditions.checkNotNull(enrichers, "'enrichers' must not be null");
        Preconditions.checkArgument(!enrichers.isEmpty(), "'enrichers' must not be empty");
        this.enrichers = List.copyOf(enrichers);
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
                @SuppressWarnings({"unchecked", "rawtypes"})
                Map<String, Schema<?>> referencedSchemas = (Map<String, Schema<?>>) (Map<String, ?>) resolved.referencedSchemas;
                enrichers.forEach(e -> e.apply(type, referencedSchemas));
                referencedSchemas.forEach((name, schema) ->
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
