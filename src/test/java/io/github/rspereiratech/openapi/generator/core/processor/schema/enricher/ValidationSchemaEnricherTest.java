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
package io.github.rspereiratech.openapi.generator.core.processor.schema.enricher;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ValidationSchemaEnricher}.
 *
 * <p>Verifies that Jakarta Bean Validation annotations declared on DTO fields are
 * correctly propagated to the corresponding OpenAPI schema properties, including
 * {@code @JsonProperty} name aliasing and superclass field traversal.
 */
class ValidationSchemaEnricherTest {

    private final ValidationSchemaEnricher applier = new ValidationSchemaEnricher();

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

    record DecimalMinMaxExclusiveDto(
            @DecimalMin(value = "1.5", inclusive = false) double price,
            @DecimalMax(value = "9.9", inclusive = false) double discount
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

    record NotEmptyListDto(
            @NotEmpty List<String> tags
    ) {}

    record PatternDto(
            @Pattern(regexp = "\\d{4}") String year
    ) {}

    record EmailDto(
            @Email String address
    ) {}

    record PositiveDto(
            @Positive       int positiveVal,
            @PositiveOrZero int positiveOrZeroVal
    ) {}

    record NegativeDto(
            @Negative       int negativeVal,
            @NegativeOrZero int negativeOrZeroVal
    ) {}

    record SizeListDto(
            @Size(min = 1, max = 10) List<String> tags
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

    static class BaseDto {
        @Min(10) int baseField;
    }

    static class ChildDto extends BaseDto {
        @Max(99) int childField;
    }

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
        applier.apply(MinMaxDto.class, schemas);
        assertEquals(BigDecimal.valueOf(5), prop(schemas, MinMaxDto.class, "count").getMinimum());
    }

    @Test
    void max_setsMaximumOnProperty() {
        var schemas = schemasFor(MinMaxDto.class);
        applier.apply(MinMaxDto.class, schemas);
        assertEquals(BigDecimal.valueOf(100), prop(schemas, MinMaxDto.class, "limit").getMaximum());
    }

    @Test
    void min_doesNotAffectOtherProperties() {
        var schemas = schemasFor(MinMaxDto.class);
        applier.apply(MinMaxDto.class, schemas);
        assertNull(prop(schemas, MinMaxDto.class, "count").getMaximum());
    }

    // ==========================================================================
    // @DecimalMin / @DecimalMax
    // ==========================================================================

    @Test
    void decimalMin_setsMinimum() {
        var schemas = schemasFor(DecimalMinMaxDto.class);
        applier.apply(DecimalMinMaxDto.class, schemas);
        assertEquals(new BigDecimal("1.5"), prop(schemas, DecimalMinMaxDto.class, "price").getMinimum());
    }

    @Test
    void decimalMax_setsMaximum() {
        var schemas = schemasFor(DecimalMinMaxDto.class);
        applier.apply(DecimalMinMaxDto.class, schemas);
        assertEquals(new BigDecimal("9.9"), prop(schemas, DecimalMinMaxDto.class, "discount").getMaximum());
    }

    @Test
    void decimalMin_inclusive_doesNotSetExclusiveMinimum() {
        var schemas = schemasFor(DecimalMinMaxDto.class);
        applier.apply(DecimalMinMaxDto.class, schemas);
        assertNull(prop(schemas, DecimalMinMaxDto.class, "price").getExclusiveMinimum(),
                "inclusive=true (default) must not set exclusiveMinimum");
    }

    @Test
    void decimalMin_notInclusive_setsExclusiveMinimum() {
        var schemas = schemasFor(DecimalMinMaxExclusiveDto.class);
        applier.apply(DecimalMinMaxExclusiveDto.class, schemas);
        assertTrue(prop(schemas, DecimalMinMaxExclusiveDto.class, "price").getExclusiveMinimum(),
                "inclusive=false must set exclusiveMinimum=true");
    }

    @Test
    void decimalMax_notInclusive_setsExclusiveMaximum() {
        var schemas = schemasFor(DecimalMinMaxExclusiveDto.class);
        applier.apply(DecimalMinMaxExclusiveDto.class, schemas);
        assertTrue(prop(schemas, DecimalMinMaxExclusiveDto.class, "discount").getExclusiveMaximum(),
                "inclusive=false must set exclusiveMaximum=true");
    }

    // ==========================================================================
    // @Size
    // ==========================================================================

    @Test
    void size_setsMinLengthAndMaxLength() {
        var schemas = schemasFor(SizeDto.class);
        applier.apply(SizeDto.class, schemas);
        assertAll(
                () -> assertEquals(2,  prop(schemas, SizeDto.class, "name").getMinLength()),
                () -> assertEquals(50, prop(schemas, SizeDto.class, "name").getMaxLength())
        );
    }

    @Test
    void size_minZero_doesNotSetMinLength() {
        var schemas = schemasFor(SizeMaxOnlyDto.class);
        applier.apply(SizeMaxOnlyDto.class, schemas);
        assertNull(prop(schemas, SizeMaxOnlyDto.class, "description").getMinLength(),
                "min=0 (default) must not produce minLength");
    }

    @Test
    void size_maxIntegerMax_doesNotSetMaxLength() {
        var schemas = schemasFor(SizeMinOnlyDto.class);
        applier.apply(SizeMinOnlyDto.class, schemas);
        assertNull(prop(schemas, SizeMinOnlyDto.class, "tag").getMaxLength(),
                "max=Integer.MAX_VALUE (default) must not produce maxLength");
    }

    // ==========================================================================
    // @NotNull / @NotBlank / @NotEmpty
    // ==========================================================================

    @Test
    void notNull_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "required").getNullable());
    }

    @Test
    void notBlank_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "nonBlank").getNullable());
    }

    @Test
    void notEmpty_setsNullableFalse() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertFalse(prop(schemas, NotNullDto.class, "nonEmpty").getNullable());
    }

    @Test
    void notBlank_setsMinLengthOne() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertEquals(1, prop(schemas, NotNullDto.class, "nonBlank").getMinLength(),
                "@NotBlank on a String must set minLength=1");
    }

    @Test
    void notEmpty_onString_setsMinLengthOne() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertEquals(1, prop(schemas, NotNullDto.class, "nonEmpty").getMinLength(),
                "@NotEmpty on a String must set minLength=1");
    }

    @Test
    void notEmpty_onList_setsMinItemsOne() {
        var schemas = schemasFor(NotEmptyListDto.class);
        applier.apply(NotEmptyListDto.class, schemas);
        assertEquals(1, prop(schemas, NotEmptyListDto.class, "tags").getMinItems(),
                "@NotEmpty on a List must set minItems=1");
    }

    @Test
    void notNull_doesNotSetMinLength() {
        var schemas = schemasFor(NotNullDto.class);
        applier.apply(NotNullDto.class, schemas);
        assertNull(prop(schemas, NotNullDto.class, "required").getMinLength(),
                "@NotNull must not set minLength");
    }

    // ==========================================================================
    // @Pattern
    // ==========================================================================

    @Test
    void pattern_setsPatternOnProperty() {
        var schemas = schemasFor(PatternDto.class);
        applier.apply(PatternDto.class, schemas);
        assertEquals("\\d{4}", prop(schemas, PatternDto.class, "year").getPattern());
    }

    // ==========================================================================
    // @Email
    // ==========================================================================

    @Test
    void email_setsFormatEmail() {
        var schemas = schemasFor(EmailDto.class);
        applier.apply(EmailDto.class, schemas);
        assertEquals("email", prop(schemas, EmailDto.class, "address").getFormat());
    }

    // ==========================================================================
    // @Positive / @PositiveOrZero
    // ==========================================================================

    @Test
    void positive_setsMinimumZeroAndExclusiveMinimum() {
        var schemas = schemasFor(PositiveDto.class);
        applier.apply(PositiveDto.class, schemas);
        Schema<?> prop = prop(schemas, PositiveDto.class, "positiveVal");
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, prop.getMinimum()),
                () -> assertTrue(prop.getExclusiveMinimum(), "exclusiveMinimum must be true for @Positive")
        );
    }

    @Test
    void positiveOrZero_setsMinimumZeroWithoutExclusiveMinimum() {
        var schemas = schemasFor(PositiveDto.class);
        applier.apply(PositiveDto.class, schemas);
        Schema<?> prop = prop(schemas, PositiveDto.class, "positiveOrZeroVal");
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, prop.getMinimum()),
                () -> assertNull(prop.getExclusiveMinimum(), "exclusiveMinimum must not be set for @PositiveOrZero")
        );
    }

    // ==========================================================================
    // @Negative / @NegativeOrZero
    // ==========================================================================

    @Test
    void negative_setsMaximumZeroAndExclusiveMaximum() {
        var schemas = schemasFor(NegativeDto.class);
        applier.apply(NegativeDto.class, schemas);
        Schema<?> prop = prop(schemas, NegativeDto.class, "negativeVal");
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, prop.getMaximum()),
                () -> assertTrue(prop.getExclusiveMaximum(), "exclusiveMaximum must be true for @Negative")
        );
    }

    @Test
    void negativeOrZero_setsMaximumZeroWithoutExclusiveMaximum() {
        var schemas = schemasFor(NegativeDto.class);
        applier.apply(NegativeDto.class, schemas);
        Schema<?> prop = prop(schemas, NegativeDto.class, "negativeOrZeroVal");
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, prop.getMaximum()),
                () -> assertNull(prop.getExclusiveMaximum(), "exclusiveMaximum must not be set for @NegativeOrZero")
        );
    }

    // ==========================================================================
    // @Size on collections → minItems / maxItems
    // ==========================================================================

    @Test
    void size_onList_setsMinItemsAndMaxItems() {
        var schemas = schemasFor(SizeListDto.class);
        applier.apply(SizeListDto.class, schemas);
        Schema<?> prop = prop(schemas, SizeListDto.class, "tags");
        assertAll(
                () -> assertEquals(1,  prop.getMinItems(), "minItems must be set for @Size on List"),
                () -> assertEquals(10, prop.getMaxItems(), "maxItems must be set for @Size on List")
        );
    }

    @Test
    void size_onList_doesNotSetStringLengthFields() {
        var schemas = schemasFor(SizeListDto.class);
        applier.apply(SizeListDto.class, schemas);
        Schema<?> prop = prop(schemas, SizeListDto.class, "tags");
        assertAll(
                () -> assertNull(prop.getMinLength(), "minLength must not be set for @Size on List"),
                () -> assertNull(prop.getMaxLength(), "maxLength must not be set for @Size on List")
        );
    }

    // ==========================================================================
    // Superclass field inheritance
    // ==========================================================================

    @Test
    @SuppressWarnings("rawtypes")
    void inheritedFields_constraintsApplied() {
        Schema<?> childSchema = new Schema<>();
        Map<String, Schema> props = new LinkedHashMap<>();
        props.put("baseField",  new Schema());
        props.put("childField", new Schema());
        childSchema.setProperties(props);
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("ChildDto", childSchema);

        applier.apply(ChildDto.class, schemas);

        assertAll(
                () -> assertEquals(BigDecimal.valueOf(10),
                        ((Schema<?>) schemas.get("ChildDto").getProperties().get("baseField")).getMinimum(),
                        "@Min on inherited BaseDto.baseField must be applied"),
                () -> assertEquals(BigDecimal.valueOf(99),
                        ((Schema<?>) schemas.get("ChildDto").getProperties().get("childField")).getMaximum(),
                        "@Max on ChildDto.childField must be applied")
        );
    }

    // ==========================================================================
    // @JsonProperty name resolution
    // ==========================================================================

    @Test
    void jsonProperty_usesSerializedNameAsKey() {
        var schemas = schemasFor(JsonPropertyDto.class);
        applier.apply(JsonPropertyDto.class, schemas);
        Schema<?> prop = (Schema<?>) schemas.get("JsonPropertyDto").getProperties().get("search_offset");
        assertEquals(BigDecimal.valueOf(0), prop.getMinimum(),
                "@Min must be applied to the property keyed by @JsonProperty value");
    }

    // ==========================================================================
    // Null / empty safety
    // ==========================================================================

    @Test
    void apply_nullSchemas_doesNotThrow() {
        applier.apply(MinMaxDto.class, null);
    }

    @Test
    void apply_emptySchemas_doesNotThrow() {
        applier.apply(MinMaxDto.class, new LinkedHashMap<String, Schema<?>>());
    }

    @Test
    void apply_schemaWithNullProperties_doesNotThrow() {
        Schema<?> schema = new Schema<>(); // properties is null
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        schemas.put("MinMaxDto", schema);
        applier.apply(MinMaxDto.class, schemas);
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

        applier.apply(NestedDto.class, schemas);

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
