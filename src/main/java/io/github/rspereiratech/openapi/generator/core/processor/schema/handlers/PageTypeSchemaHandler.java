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
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * {@link TypeSchemaHandler} for Spring Data {@code Page<T>} types.
 *
 * <p>Generates a {@code PageXxxDTO} object schema whose property order matches
 * SpringDoc's output, and ensures that {@code SortObject} and {@code PageableObject}
 * component schemas are registered in the schema registry as well.
 *
 * @author ruispereira
 */
@SuppressWarnings("java:S1452")
public class PageTypeSchemaHandler implements TypeSchemaHandler {

    /** OpenAPI {@code $ref} prefix for component schemas. */
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    /** Component schema name for the {@code SortObject} type. */
    private static final String SORT_OBJECT       = "SortObject";
    /** Component schema name for the {@code PageableObject} type. */
    private static final String PAGEABLE_OBJECT   = "PageableObject";
    /** OpenAPI {@code type} value for object schemas. */
    private static final String TYPE_OBJECT       = "object";
    /** OpenAPI {@code type} value for integer schemas. */
    private static final String TYPE_INTEGER      = "integer";
    /** OpenAPI {@code type} value for boolean schemas. */
    private static final String TYPE_BOOLEAN      = "boolean";
    /** OpenAPI {@code type} value for string schemas. */
    private static final String TYPE_STRING       = "string";
    /** OpenAPI {@code format} value for 32-bit integers. */
    private static final String FORMAT_INT32      = "int32";
    /** OpenAPI {@code format} value for 64-bit integers. */
    private static final String FORMAT_INT64      = "int64";

    @Override
    @SuppressWarnings("java:S1872")
    public boolean supports(Type type) {
        if (!(type instanceof ParameterizedType pt)) return false;
        Type raw = pt.getRawType();
        return raw instanceof Class<?> c && "org.springframework.data.domain.Page".equals(c.getName());
    }

    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        Type itemType = TypeUtils.firstTypeArgument(type);
        return buildPageSchema(itemType, schemaProcessor);
    }

    // ------------------------------------------------------------------
    // Page schema construction
    // ------------------------------------------------------------------

    /**
     * Builds a {@code PageXxxDTO} component schema for the given item type and registers it
     * in the schema registry, along with the {@code SortObject} and {@code PageableObject}
     * helper schemas that it references.
     *
     * <p>The property order matches SpringDoc's output:
     * {@code totalElements}, {@code totalPages}, {@code size}, {@code content},
     * {@code number}, {@code first}, {@code last}, {@code sort},
     * {@code numberOfElements}, {@code pageable}, {@code empty}.</p>
     *
     * @param itemType        the element type of the page (e.g. {@code TenantDTO})
     * @param schemaProcessor used to resolve {@code itemType} and access the schema registry
     * @return a {@code $ref} schema pointing to the newly registered {@code PageXxxDTO} component
     */
    private Schema<?> buildPageSchema(Type itemType, SchemaProcessor schemaProcessor) {
        Schema<?> itemSchema = schemaProcessor.toSchema(itemType);
        String pageSchemaName = "Page" + schemaNameFor(itemType);

        Map<String, Schema<?>> registry = schemaProcessor.getSchemaRegistry();
        ensureSortObjectSchema(registry);
        ensurePageableObjectSchema(registry);

        // Property order matches SpringDoc output
        Schema<?> pageSchema = new Schema<>()
                .type(TYPE_OBJECT)
                .addProperty("totalElements",    new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT64))
                .addProperty("totalPages",       new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("size",             new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("content",          new ArraySchema().items(itemSchema))
                .addProperty("number",           new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("first",            new Schema<>().type(TYPE_BOOLEAN))
                .addProperty("last",             new Schema<>().type(TYPE_BOOLEAN))
                .addProperty("sort",             new ArraySchema().items(new Schema<>().$ref(SCHEMA_REF_PREFIX + SORT_OBJECT)))
                .addProperty("numberOfElements", new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("pageable",         new Schema<>().$ref(SCHEMA_REF_PREFIX + PAGEABLE_OBJECT))
                .addProperty("empty",            new Schema<>().type(TYPE_BOOLEAN));

        registry.put(pageSchemaName, pageSchema);
        return new Schema<>().$ref(SCHEMA_REF_PREFIX + pageSchemaName);
    }

    /**
     * Registers the {@code SortObject} component schema in the registry if not already present.
     *
     * <p>Properties: {@code direction}, {@code nullHandling}, {@code ascending},
     * {@code property}, {@code ignoreCase}.</p>
     *
     * @param registry the schema registry to populate
     */
    private void ensureSortObjectSchema(Map<String, Schema<?>> registry) {
        if (registry.containsKey(SORT_OBJECT)) return;
        Schema<?> sort = new Schema<>()
                .type(TYPE_OBJECT)
                .addProperty("direction",    new Schema<>().type(TYPE_STRING))
                .addProperty("nullHandling", new Schema<>().type(TYPE_STRING))
                .addProperty("ascending",    new Schema<>().type(TYPE_BOOLEAN))
                .addProperty("property",     new Schema<>().type(TYPE_STRING))
                .addProperty("ignoreCase",   new Schema<>().type(TYPE_BOOLEAN));
        registry.put(SORT_OBJECT, sort);
    }

    /**
     * Registers the {@code PageableObject} component schema in the registry if not already present.
     *
     * <p>Also ensures {@code SortObject} is registered first, since {@code PageableObject}
     * references it via {@code sort}.</p>
     *
     * <p>Properties: {@code offset}, {@code sort} (array of {@code $ref: SortObject}),
     * {@code paged}, {@code pageNumber}, {@code pageSize}, {@code unpaged}.</p>
     *
     * @param registry the schema registry to populate
     */
    private void ensurePageableObjectSchema(Map<String, Schema<?>> registry) {
        if (registry.containsKey(PAGEABLE_OBJECT)) return;
        ensureSortObjectSchema(registry);
        Schema<?> pageable = new Schema<>()
                .type(TYPE_OBJECT)
                .addProperty("offset",     new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT64))
                .addProperty("sort",       new ArraySchema().items(new Schema<>().$ref(SCHEMA_REF_PREFIX + SORT_OBJECT)))
                .addProperty("paged",      new Schema<>().type(TYPE_BOOLEAN))
                .addProperty("pageNumber", new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("pageSize",   new Schema<>().type(TYPE_INTEGER).format(FORMAT_INT32))
                .addProperty("unpaged",    new Schema<>().type(TYPE_BOOLEAN));
        registry.put(PAGEABLE_OBJECT, pageable);
    }

    /**
     * Derives a simple name from {@code type} to use as the suffix of the {@code PageXxxDTO}
     * schema name (e.g. {@code TenantDTO} → {@code "PageTenantDTO"}).
     *
     * <ul>
     *   <li>{@link Class} — returns {@link Class#getSimpleName()}</li>
     *   <li>{@link ParameterizedType} — returns the simple name of the raw class</li>
     *   <li>fallback — strips the package prefix from {@link Type#getTypeName()}</li>
     * </ul>
     *
     * @param type the item type of the page
     * @return a non-empty simple name suitable for use in a schema key
     */
    private String schemaNameFor(Type type) {
        if (type instanceof Class<?> c) {
            return c.getSimpleName();
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c.getSimpleName();
        }
        String name = type.getTypeName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }
}
