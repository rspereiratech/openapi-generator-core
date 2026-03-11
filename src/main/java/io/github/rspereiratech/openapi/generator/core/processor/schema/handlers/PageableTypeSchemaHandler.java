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

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

/**
 * {@link TypeSchemaHandler} for Spring Data {@code Pageable} and {@code PageRequest} types.
 *
 * <p>Registers the {@code Pageable} component schema (with {@code page}, {@code size},
 * and {@code sort} properties) and returns a {@code $ref} to it — matching the single
 * query-parameter representation that SpringDoc generates.
 *
 * @author ruispereira
 */
@SuppressWarnings("java:S1452")
public class PageableTypeSchemaHandler implements TypeSchemaHandler {

    /**
     * Returns {@code true} if {@code type} is {@code org.springframework.data.domain.Pageable}
     * or {@code org.springframework.data.domain.PageRequest}.
     *
     * @param type the type to test; must not be {@code null}
     * @return {@code true} for {@code Pageable} or {@code PageRequest}; {@code false} otherwise
     */
    @Override
    public boolean supports(Type type) {
        if (!(type instanceof Class<?> c)) return false;
        String name = c.getName();
        return "org.springframework.data.domain.Pageable".equals(name)
                || "org.springframework.data.domain.PageRequest".equals(name);
    }

    /**
     * Ensures a {@code Pageable} component schema is registered and returns a
     * {@code $ref} pointing to it.
     *
     * <p>The component schema is an object with three properties:
     * <ul>
     *   <li>{@code page} — integer ≥ 0</li>
     *   <li>{@code size} — integer ≥ 1</li>
     *   <li>{@code sort} — array of strings</li>
     * </ul>
     *
     * @param type            the {@code Pageable} or {@code PageRequest} type (unused beyond gate check)
     * @param schemaProcessor the shared processor whose registry is populated with the component schema
     * @return a {@code $ref} schema pointing to {@code #/components/schemas/Pageable}
     */
    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        Map<String, Schema<?>> registry = schemaProcessor.getSchemaRegistry();
        if (!registry.containsKey("Pageable")) {
            Schema<?> pageable = new Schema<>()
                    .type("object")
                    .addProperty("page", new Schema<>().type("integer").format("int32").minimum(BigDecimal.ZERO))
                    .addProperty("size", new Schema<>().type("integer").format("int32").minimum(BigDecimal.ONE))
                    .addProperty("sort", new ArraySchema().items(new Schema<>().type("string")));
            registry.put("Pageable", pageable);
        }
        return new Schema<>().$ref("#/components/schemas/Pageable");
    }
}
