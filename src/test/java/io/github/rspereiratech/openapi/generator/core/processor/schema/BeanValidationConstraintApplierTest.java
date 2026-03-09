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
package io.github.rspereiratech.openapi.generator.core.processor.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class BeanValidationConstraintApplierTest {

    // ==========================================================================
    // Fixtures
    // ==========================================================================

    record MinMaxDto(
            @Min(5)       int count,
            @Max(100)     int limit
    ) {}

    record DecimalMinMaxDto(
            @DecimalMin("1.5") double price,
            @DecimalMax("9.9") double discount
    ) {}

    record SizeDto(
            @Size(min = 2, max = 50) String name
    ) {}

    record SizeMinOnlyDto(
            @Size(min = 1) String tag
    ) {}

    record SizeMaxOnlyDto(
            @Size(max = 255) String description
    ) {}

    record NotNullDto(
            @NotNull  String required,
            @NotBlank String nonBlank,
            @NotEmpty String nonEmpty
    ) {}

    record PatternDto(
            @Pattern(regexp = "\\d{4}") String year
    ) {}

    record JsonPropertyDto(
            @Min(0)
            @JsonProperty("search_offset") int offset
    ) {}

    record NestedDto(
            String label,
            @Min(1) int id,
            MinMaxDto inner
    ) {}

    // ==========================================================================
    // Helpers
    // ==========================================================================

    /** Builds a schema map with one entry per field name, each with a fresh Schema. */
    @SuppressWarnings("rawtypes")
    private static Map<String, Schema<?>> schemasFor(Class<?> clazz) {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        Schema<?> parent = new Schema<>();
        Map<String, Schema> props = new LinkedHashMap<>();

        for (var field : clazz.getDeclaredFields()) {
            com.fasterxml.jackson.annotation.JsonProperty jp =
                    field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
            String key = (jp != null && !jp.value().isEmpty()) ? jp.value() : field.getName();
            props.put(key, new Schema());
        }

        parent.setProperties(new java.util.LinkedHashMap<>(props));
        schemas.put(clazz.getSimpleName(), parent);
        return schemas;
    }

    private static Schema<?> prop(Map<String, Schema<?>> schemas, Class<?> clazz, String field) {
        //noinspection unchecked
        return (Schema<?>) schemas.get(clazz.getSimpleName()).getProperties().get(field);
    }

    // ==========================================================================
    // @Min / @Max
    // ==========================================================================

    @Test
    void min_setsMinimumOnProperty() {
        var schemas = schemasFor(MinMaxDto.class);
        BeanValidationConstraintApplier.apply(MinMaxDto.class, schemas);
        assertEquals(BigDecimal.valueOf(5), prop(schemas, MinMaxDto.class, "count").getMinimum());
    }

    @Test
    void max_setsMaximumOnProperty() {
        var schemas = schemasFor(MinMaxDto.class);
        BeanValidationConstraintApplier.apply(MinMaxDto.class, schemas);
        assertEquals(BigDecimal.valueOf(100), prop(schemas, MinMaxDto.class, "limit").getMaximum());
    }

    @Test
    void min_doesNotAffectOtherProperties() {
        var schemas = schemasFor(MinMaxDto.class);
        BeanValidationConstraintApplier.apply(MinMaxDto.class, schemas);
        assertNull(prop(schemas, MinMaxDto.class, "count").getMaximum());
    }

    // ==========================================================================
    // @DecimalMin / @DecimalMax
    // ==========================================================================

    @Test
    void decimalMin_setsMinimum() {
        var schemas = schemasFor(DecimalMinMaxDto.class);
        BeanValidationConstraintApplier.apply(DecimalMinMaxDto.class, schemas);
        assertEquals(new BigDecimal("1.5"), prop(schemas, DecimalMinMaxDto.class, "price").getMinimum());
    }

    @Test
    void decimalMax_setsMaximum() {
        var schemas = schemasFor(DecimalMinMaxDto.class);
        BeanValidationConstraintApplier.apply(DecimalMinMaxDto.class, schemas);
        assertEquals(new BigDecimal("9.9"), prop(schemas, DecimalMinMaxDto.class, "discount").getMaximum());
    }

    // ==========================================================================
    // @Size
    // ==========================================================================

    @Test
    void size_setsMinLengthAndMaxLength() {
        var schemas = schemasFor(SizeDto.class);
        BeanValidationConstraintApplier.apply(SizeDto.class, schemas);
        assertAll(
                () -> assertEquals(2,  prop(schemas, SizeDto.class, "name").getMinLength()),
                () -> assertEquals(50, prop(schemas, SizeDto.class, "name").getMaxLength())
        );
    }

    @Test
    void size_minZero_doesNotSetMinLength() {
        var schemas = schemasFor(SizeMaxOnlyDto.class);
        BeanValidationConstraintApplier.apply(SizeMaxOnlyDto.class, schemas);
        assertNull(prop(schemas, SizeMaxOnlyDto.class, "description").getMinLength(),
                "min=0 (default) must not produce minLength");
    }

    @Test
    void size_maxIntegerMax_doesNotSetMaxLength() {
        var schemas = schemasFor(SizeMinOnlyDto.class);
        BeanValidationConstraintApplier.apply(SizeMinOnlyDto.class, schemas);
        assertNull(prop(schemas, SizeMinOnlyDto.class, "tag").getMaxLength(),
                "max=Integer.MAX_VALUE (default) must not produce maxLength");
    }

    // ==========================================================================
    // @NotNull / @NotBlank / @NotEmpty
    // ==========================================================================

    @Test
    void notNull_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        BeanValidationConstraintApplier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "required").getNullable());
    }

    @Test
    void notBlank_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        BeanValidationConstraintApplier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "nonBlank").getNullable());
    }

    @Test
    void notEmpty_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        BeanValidationConstraintApplier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "nonEmpty").getNullable());
    }

    // ==========================================================================
    // @Pattern
    // ==========================================================================

    @Test
    void pattern_setsPatternOnProperty() {
        var schemas = schemasFor(PatternDto.class);
        BeanValidationConstraintApplier.apply(PatternDto.class, schemas);
        assertEquals("\\d{4}", prop(schemas, PatternDto.class, "year").getPattern());
    }

    // ==========================================================================
    // @JsonProperty name resolution
    // ==========================================================================

    @Test
    void jsonProperty_usesSerializedNameAsKey() {
        var schemas = schemasFor(JsonPropertyDto.class);
        BeanValidationConstraintApplier.apply(JsonPropertyDto.class, schemas);
        Schema<?> prop = (Schema<?>) schemas.get("JsonPropertyDto").getProperties().get("search_offset");
        assertEquals(BigDecimal.valueOf(0), prop.getMinimum(),
                "@Min must be applied to the property keyed by @JsonProperty value");
    }

    // ==========================================================================
    // Null / empty safety
    // ==========================================================================

    @Test
    void apply_nullSchemas_doesNotThrow() {
        BeanValidationConstraintApplier.apply(MinMaxDto.class, null);
    }

    @Test
    void apply_emptySchemas_doesNotThrow() {
        BeanValidationConstraintApplier.apply(MinMaxDto.class, new LinkedHashMap<String, Object>());
    }

    @Test
    void apply_schemaWithNullProperties_doesNotThrow() {
        Schema<?> schema = new Schema<>(); // properties is null
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("MinMaxDto", schema);
        BeanValidationConstraintApplier.apply(MinMaxDto.class, schemas);
    }

    // ==========================================================================
    // Nested type traversal
    // ==========================================================================

    @Test
    @SuppressWarnings("rawtypes")
    void nestedType_constraintsAppliedToNestedSchema() {
        // Build schemas for both NestedDto and its inner MinMaxDto
        var schemas = new LinkedHashMap<String, Schema<?>>();

        Schema<?> nestedParent = new Schema<>();
        Map<String, Schema> nestedProps = new LinkedHashMap<>();
        nestedProps.put("label",  new Schema());
        nestedProps.put("id",     new Schema());
        nestedProps.put("inner",  new Schema());
        nestedParent.setProperties(nestedProps);
        schemas.put("NestedDto", nestedParent);

        Schema<?> innerParent = new Schema<>();
        Map<String, Schema> innerProps = new LinkedHashMap<>();
        innerProps.put("count", new Schema());
        innerProps.put("limit", new Schema());
        innerParent.setProperties(innerProps);
        schemas.put("MinMaxDto", innerParent);

        BeanValidationConstraintApplier.apply(NestedDto.class, schemas);

        assertAll(
                () -> assertEquals(BigDecimal.valueOf(1),
                        ((Schema<?>) schemas.get("NestedDto").getProperties().get("id")).getMinimum(),
                        "@Min(1) on NestedDto.id must be applied"),
                () -> assertEquals(BigDecimal.valueOf(5),
                        ((Schema<?>) schemas.get("MinMaxDto").getProperties().get("count")).getMinimum(),
                        "@Min(5) on MinMaxDto.count must be applied via nested traversal")
        );
    }
}
