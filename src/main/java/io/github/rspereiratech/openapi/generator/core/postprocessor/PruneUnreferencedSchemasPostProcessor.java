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
package io.github.rspereiratech.openapi.generator.core.postprocessor;

import com.google.common.base.Preconditions;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Removes any entry from {@code components/schemas} that is never referenced via
 * {@code $ref} anywhere in the serialised document (including within other schemas).
 *
 * <p>This cleans up phantom schemas that {@code ModelConverters} registers as side-effects
 * of processing types that use {@code @JsonUnwrapped} composition — e.g.
 * {@code IdentifiableDTO} gets registered when {@code QueueDTO} is resolved, even though
 * the final {@code QueueDTO} schema inlines those fields and never {@code $ref}s the helper type.
 *
 * @author ruispereira
 */
@Slf4j
public class PruneUnreferencedSchemasPostProcessor implements PostProcessor {

    /** Matches {@code $ref} values that point to a component schema, capturing the schema name. */
    private static final Pattern REF_PATTERN = Pattern.compile("#/components/schemas/([\\w]+)");

    /**
     * Removes every entry from {@code components/schemas} that is not referenced by a
     * {@code $ref} anywhere in the serialised document.
     *
     * <p>The document is temporarily serialised to JSON so that {@code $ref} values
     * inside nested schemas are also detected. If serialisation fails the step is
     * skipped silently to avoid blocking the generation pipeline.</p>
     *
     * @param openAPI the OpenAPI model to prune in-place; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }

        try {
            String json = Json.mapper().writeValueAsString(openAPI);
            Set<String> referenced = REF_PATTERN.matcher(json).results()
                    .map(mr -> mr.group(1))
                    .collect(Collectors.toUnmodifiableSet());
            int before = openAPI.getComponents().getSchemas().size();
            openAPI.getComponents().getSchemas().keySet().removeIf(Predicate.not(referenced::contains));
            int removed = before - openAPI.getComponents().getSchemas().size();
            if (removed > 0) {
                log.debug("Pruned {} unreferenced schema(s)", removed);
            }
        } catch (Exception e) {
            log.debug("Schema pruning skipped: {}", e.getMessage());
        }
    }
}
