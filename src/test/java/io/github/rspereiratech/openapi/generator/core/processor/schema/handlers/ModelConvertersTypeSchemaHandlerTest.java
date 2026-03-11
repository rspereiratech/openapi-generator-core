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
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link ModelConvertersTypeSchemaHandler}.
 *
 * <p>Verifies catch-all schema resolution via Jackson {@code ModelConverters},
 * {@link io.github.rspereiratech.openapi.generator.core.processor.schema.enricher.ValidationSchemaEnricher}
 * integration for propagating Bean Validation constraints,
 * {@link io.github.rspereiratech.openapi.generator.core.processor.schema.enricher.SchemaAnnotationEnricher}
 * integration for propagating {@code @Schema} metadata, and schema registration
 * in the component registry.
 */
@ExtendWith(MockitoExtension.class)
class ModelConvertersTypeSchemaHandlerTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private final Map<String, Schema<?>> registry = new LinkedHashMap<>();
    private ModelConvertersTypeSchemaHandler handler;

    record SimpleDto(String name, int value) {}
    record NestedDto(String label, SimpleDto inner) {}

    record ValidatedDto(
            @Min(0)  int age,
            @Max(100) int score,
            @Size(min = 1, max = 50) String name,
            @Pattern(regexp = "\\d+") String code,
            @NotNull String required
    ) {}

    @io.swagger.v3.oas.annotations.media.Schema(description = "A product record")
    record AnnotatedDto(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Product name", example = "Widget") String productName,
            int quantity
    ) {}

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);
        handler = new ModelConvertersTypeSchemaHandler();
    }

    // ==========================================================================
    // supports() — catch-all
    // ==========================================================================

    @Test
    void supports_string_returnsTrue() {
        assertTrue(handler.supports(String.class));
    }

    @Test
    void supports_integer_returnsTrue() {
        assertTrue(handler.supports(Integer.class));
    }

    @Test
    void supports_void_returnsTrue() {
        assertTrue(handler.supports(void.class));
    }

    @Test
    void supports_complexType_returnsTrue() {
        assertTrue(handler.supports(SimpleDto.class));
    }

    // ==========================================================================
    // resolve() — scalar types
    // ==========================================================================

    @Test
    void resolve_string_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(String.class, schemaProcessor);
        assertNotNull(result);
    }

    @Test
    void resolve_integer_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(Integer.class, schemaProcessor);
        assertNotNull(result);
    }

    // ==========================================================================
    // resolve() — complex types register referenced schemas
    // ==========================================================================

    @Test
    void resolve_complexType_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(SimpleDto.class, schemaProcessor);
        assertNotNull(result);
    }

    @Test
    void resolve_complexType_registersSchemaInRegistry() {
        handler.resolve(SimpleDto.class, schemaProcessor);
        assertTrue(registry.containsKey("SimpleDto"),
                "ModelConverters must register SimpleDto in the schema registry");
    }

    @Test
    void resolve_nestedComplexType_registersRootSchemaInRegistry() {
        handler.resolve(NestedDto.class, schemaProcessor);
        assertTrue(registry.containsKey("NestedDto"), "NestedDto must be registered");
    }

    @Test
    void resolve_nestedComplexType_registersNestedSchemaInRegistry() {
        handler.resolve(NestedDto.class, schemaProcessor);
        assertTrue(registry.containsKey("SimpleDto"),
                "Nested SimpleDto must also be registered as a referenced schema");
    }

    @Test
    void resolve_sameTypeTwice_doesNotDuplicateRegistration() {
        handler.resolve(SimpleDto.class, schemaProcessor);
        int sizeAfterFirst = registry.size();
        handler.resolve(SimpleDto.class, schemaProcessor);
        assertTrue(registry.size() <= sizeAfterFirst + 1,
                "Re-resolving the same type must not grow the registry unboundedly");
    }

    // ==========================================================================
    // Bean Validation constraint propagation
    // ==========================================================================

    @Test
    void resolve_validatedDto_minConstraintApplied() {
        handler.resolve(ValidatedDto.class, schemaProcessor);
        Schema<?> schema = registry.get("ValidatedDto");
        assertNotNull(schema, "ValidatedDto must be in the registry");
        @SuppressWarnings("unchecked")
        Schema<?> ageProperty = (Schema<?>) schema.getProperties().get("age");
        assertNotNull(ageProperty, "age property must be present");
        assertEquals(BigDecimal.valueOf(0), ageProperty.getMinimum(),
                "@Min(0) must set minimum = 0 on the age property");
    }

    @Test
    void resolve_validatedDto_maxConstraintApplied() {
        handler.resolve(ValidatedDto.class, schemaProcessor);
        @SuppressWarnings("unchecked")
        Schema<?> scoreProperty = (Schema<?>) registry.get("ValidatedDto").getProperties().get("score");
        assertNotNull(scoreProperty, "score property must be present");
        assertEquals(BigDecimal.valueOf(100), scoreProperty.getMaximum(),
                "@Max(100) must set maximum = 100 on the score property");
    }

    @Test
    void resolve_validatedDto_sizeConstraintApplied() {
        handler.resolve(ValidatedDto.class, schemaProcessor);
        @SuppressWarnings("unchecked")
        Schema<?> nameProperty = (Schema<?>) registry.get("ValidatedDto").getProperties().get("name");
        assertNotNull(nameProperty, "name property must be present");
        assertEquals(1, nameProperty.getMinLength(),
                "@Size(min=1) must set minLength = 1 on the name property");
        assertEquals(50, nameProperty.getMaxLength(),
                "@Size(max=50) must set maxLength = 50 on the name property");
    }

    @Test
    void resolve_validatedDto_patternConstraintApplied() {
        handler.resolve(ValidatedDto.class, schemaProcessor);
        @SuppressWarnings("unchecked")
        Schema<?> codeProperty = (Schema<?>) registry.get("ValidatedDto").getProperties().get("code");
        assertNotNull(codeProperty, "code property must be present");
        assertEquals("\\d+", codeProperty.getPattern(),
                "@Pattern(regexp=\"\\\\d+\") must set pattern on the code property");
    }

    @Test
    void resolve_validatedDto_notNullConstraintApplied() {
        handler.resolve(ValidatedDto.class, schemaProcessor);
        @SuppressWarnings("unchecked")
        Schema<?> requiredProperty = (Schema<?>) registry.get("ValidatedDto").getProperties().get("required");
        assertNotNull(requiredProperty, "required property must be present");
        assertEquals(Boolean.FALSE, requiredProperty.getNullable(),
                "@NotNull must set nullable = false on the required property");
    }

    // ==========================================================================
    // @Schema annotation propagation
    // ==========================================================================

    @Test
    void resolve_annotatedDto_classLevelDescriptionApplied() {
        handler.resolve(AnnotatedDto.class, schemaProcessor);
        Schema<?> schema = registry.get("AnnotatedDto");
        assertNotNull(schema, "AnnotatedDto must be in the registry");
        assertEquals("A product record", schema.getDescription(),
                "@Schema(description) at class level must be propagated to the schema");
    }

    @Test
    void resolve_annotatedDto_fieldLevelDescriptionApplied() {
        handler.resolve(AnnotatedDto.class, schemaProcessor);
        @SuppressWarnings("unchecked")
        Schema<?> prop = (Schema<?>) registry.get("AnnotatedDto").getProperties().get("productName");
        assertNotNull(prop, "productName property must be present");
        assertEquals("Product name", prop.getDescription(),
                "@Schema(description) at field level must be propagated to the property schema");
    }

    // ==========================================================================
    // Null guards
    // ==========================================================================

    @Test
    void supports_nullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> handler.supports(null));
    }

    @Test
    void resolve_nullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> handler.resolve(null, schemaProcessor));
    }

    @Test
    void resolve_nullSchemaProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> handler.resolve(String.class, null));
    }
}
