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
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for individual {@link ConstraintHandler} implementations.
 *
 * <p>Each test verifies {@link ConstraintHandler#supports} and
 * {@link ConstraintHandler#apply} in isolation.
 */
class ConstraintHandlersTest {

    // ==========================================================================
    // Fixtures — annotated fields used to obtain real annotation instances
    // ==========================================================================

    @SuppressWarnings("unused")
    private static class Fixture {
        @Min(5)                                          int minField;
        @Max(100)                                        int maxField;
        @DecimalMin("1.5")                               double decimalMinIncl;
        @DecimalMin(value = "1.5", inclusive = false)    double decimalMinExcl;
        @DecimalMax("9.9")                               double decimalMaxIncl;
        @DecimalMax(value = "9.9", inclusive = false)    double decimalMaxExcl;
        @Positive                                        int positiveField;
        @PositiveOrZero                                  int positiveOrZeroField;
        @Negative                                        int negativeField;
        @NegativeOrZero                                  int negativeOrZeroField;
        @Size(min = 2, max = 50)                         String sizeString;
        @Size(min = 1, max = 10)                         List<String> sizeList;
        @NotNull                                         String notNullField;
        @NotBlank                                        String notBlankField;
        @NotEmpty                                        String notEmptyString;
        @NotEmpty                                        List<String> notEmptyList;
        @Pattern(regexp = "\\d{4}")                      String patternField;
        @Email                                           String emailField;
    }

    private static Annotation ann(String fieldName) throws Exception {
        Field f = Fixture.class.getDeclaredField(fieldName);
        return f.getAnnotations()[0];
    }

    private static java.lang.reflect.Type fieldType(String fieldName) throws Exception {
        return Fixture.class.getDeclaredField(fieldName).getGenericType();
    }

    private static Schema<?> schema() {
        return new Schema<>();
    }

    // ==========================================================================
    // @Min
    // ==========================================================================

    @Test
    void min_supports_onlyMin() throws Exception {
        MinConstraintHandler h = new MinConstraintHandler();
        assertTrue(h.supports(ann("minField")));
        assertFalse(h.supports(ann("maxField")));
    }

    @Test
    void min_apply_setsMinimum() throws Exception {
        Schema<?> s = schema();
        new MinConstraintHandler().apply(ann("minField"), fieldType("minField"), s);
        assertEquals(BigDecimal.valueOf(5), s.getMinimum());
    }

    // ==========================================================================
    // @Max
    // ==========================================================================

    @Test
    void max_supports_onlyMax() throws Exception {
        MaxConstraintHandler h = new MaxConstraintHandler();
        assertTrue(h.supports(ann("maxField")));
        assertFalse(h.supports(ann("minField")));
    }

    @Test
    void max_apply_setsMaximum() throws Exception {
        Schema<?> s = schema();
        new MaxConstraintHandler().apply(ann("maxField"), fieldType("maxField"), s);
        assertEquals(BigDecimal.valueOf(100), s.getMaximum());
    }

    // ==========================================================================
    // @DecimalMin
    // ==========================================================================

    @Test
    void decimalMin_supports_onlyDecimalMin() throws Exception {
        DecimalMinConstraintHandler h = new DecimalMinConstraintHandler();
        assertTrue(h.supports(ann("decimalMinIncl")));
        assertFalse(h.supports(ann("minField")));
    }

    @Test
    void decimalMin_inclusive_doesNotSetExclusiveMinimum() throws Exception {
        Schema<?> s = schema();
        new DecimalMinConstraintHandler().apply(ann("decimalMinIncl"), fieldType("decimalMinIncl"), s);
        assertAll(
                () -> assertEquals(new BigDecimal("1.5"), s.getMinimum()),
                () -> assertNull(s.getExclusiveMinimum())
        );
    }

    @Test
    void decimalMin_exclusive_setsExclusiveMinimum() throws Exception {
        Schema<?> s = schema();
        new DecimalMinConstraintHandler().apply(ann("decimalMinExcl"), fieldType("decimalMinExcl"), s);
        assertTrue(s.getExclusiveMinimum());
    }

    // ==========================================================================
    // @DecimalMax
    // ==========================================================================

    @Test
    void decimalMax_supports_onlyDecimalMax() throws Exception {
        DecimalMaxConstraintHandler h = new DecimalMaxConstraintHandler();
        assertTrue(h.supports(ann("decimalMaxIncl")));
        assertFalse(h.supports(ann("maxField")));
    }

    @Test
    void decimalMax_inclusive_doesNotSetExclusiveMaximum() throws Exception {
        Schema<?> s = schema();
        new DecimalMaxConstraintHandler().apply(ann("decimalMaxIncl"), fieldType("decimalMaxIncl"), s);
        assertAll(
                () -> assertEquals(new BigDecimal("9.9"), s.getMaximum()),
                () -> assertNull(s.getExclusiveMaximum())
        );
    }

    @Test
    void decimalMax_exclusive_setsExclusiveMaximum() throws Exception {
        Schema<?> s = schema();
        new DecimalMaxConstraintHandler().apply(ann("decimalMaxExcl"), fieldType("decimalMaxExcl"), s);
        assertTrue(s.getExclusiveMaximum());
    }

    // ==========================================================================
    // @Positive / @PositiveOrZero
    // ==========================================================================

    @Test
    void positive_apply_setsMinimumZeroAndExclusive() throws Exception {
        Schema<?> s = schema();
        new PositiveConstraintHandler().apply(ann("positiveField"), fieldType("positiveField"), s);
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, s.getMinimum()),
                () -> assertTrue(s.getExclusiveMinimum())
        );
    }

    @Test
    void positiveOrZero_apply_setsMinimumZeroOnly() throws Exception {
        Schema<?> s = schema();
        new PositiveOrZeroConstraintHandler().apply(ann("positiveOrZeroField"), fieldType("positiveOrZeroField"), s);
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, s.getMinimum()),
                () -> assertNull(s.getExclusiveMinimum())
        );
    }

    // ==========================================================================
    // @Negative / @NegativeOrZero
    // ==========================================================================

    @Test
    void negative_apply_setsMaximumZeroAndExclusive() throws Exception {
        Schema<?> s = schema();
        new NegativeConstraintHandler().apply(ann("negativeField"), fieldType("negativeField"), s);
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, s.getMaximum()),
                () -> assertTrue(s.getExclusiveMaximum())
        );
    }

    @Test
    void negativeOrZero_apply_setsMaximumZeroOnly() throws Exception {
        Schema<?> s = schema();
        new NegativeOrZeroConstraintHandler().apply(ann("negativeOrZeroField"), fieldType("negativeOrZeroField"), s);
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, s.getMaximum()),
                () -> assertNull(s.getExclusiveMaximum())
        );
    }

    // ==========================================================================
    // @Size
    // ==========================================================================

    @Test
    void size_onString_setsMinLengthAndMaxLength() throws Exception {
        Schema<?> s = schema();
        new SizeConstraintHandler().apply(ann("sizeString"), fieldType("sizeString"), s);
        assertAll(
                () -> assertEquals(2,  s.getMinLength()),
                () -> assertEquals(50, s.getMaxLength()),
                () -> assertNull(s.getMinItems()),
                () -> assertNull(s.getMaxItems())
        );
    }

    @Test
    void size_onList_setsMinItemsAndMaxItems() throws Exception {
        Schema<?> s = schema();
        new SizeConstraintHandler().apply(ann("sizeList"), fieldType("sizeList"), s);
        assertAll(
                () -> assertEquals(1,  s.getMinItems()),
                () -> assertEquals(10, s.getMaxItems()),
                () -> assertNull(s.getMinLength()),
                () -> assertNull(s.getMaxLength())
        );
    }

    // ==========================================================================
    // @NotNull / @NotBlank / @NotEmpty
    // ==========================================================================

    @Test
    void notNull_apply_setsNullableFalseOnly() throws Exception {
        Schema<?> s = schema();
        new NotNullConstraintHandler().apply(ann("notNullField"), fieldType("notNullField"), s);
        assertAll(
                () -> assertFalse(s.getNullable()),
                () -> assertNull(s.getMinLength())
        );
    }

    @Test
    void notBlank_apply_setsNullableFalseAndMinLengthOne() throws Exception {
        Schema<?> s = schema();
        new NotBlankConstraintHandler().apply(ann("notBlankField"), fieldType("notBlankField"), s);
        assertAll(
                () -> assertFalse(s.getNullable()),
                () -> assertEquals(1, s.getMinLength())
        );
    }

    @Test
    void notEmpty_onString_setsMinLengthOne() throws Exception {
        Schema<?> s = schema();
        new NotEmptyConstraintHandler().apply(ann("notEmptyString"), fieldType("notEmptyString"), s);
        assertAll(
                () -> assertFalse(s.getNullable()),
                () -> assertEquals(1, s.getMinLength()),
                () -> assertNull(s.getMinItems())
        );
    }

    @Test
    void notEmpty_onList_setsMinItemsOne() throws Exception {
        Schema<?> s = schema();
        new NotEmptyConstraintHandler().apply(ann("notEmptyList"), fieldType("notEmptyList"), s);
        assertAll(
                () -> assertFalse(s.getNullable()),
                () -> assertEquals(1, s.getMinItems()),
                () -> assertNull(s.getMinLength())
        );
    }

    // ==========================================================================
    // @Pattern / @Email
    // ==========================================================================

    @Test
    void pattern_apply_setsPattern() throws Exception {
        Schema<?> s = schema();
        new PatternConstraintHandler().apply(ann("patternField"), fieldType("patternField"), s);
        assertEquals("\\d{4}", s.getPattern());
    }

    @Test
    void email_apply_setsFormatEmail() throws Exception {
        Schema<?> s = schema();
        new EmailConstraintHandler().apply(ann("emailField"), fieldType("emailField"), s);
        assertEquals("email", s.getFormat());
    }

    // ==========================================================================
    // supports — cross-annotation false negatives
    // ==========================================================================

    @Test
    void eachHandler_doesNotSupportUnrelatedAnnotation() throws Exception {
        // Use a Spring annotation as an unrelated annotation
        Annotation unrelated = Fixture.class.getDeclaredField("minField").getAnnotations()[0];
        // GetMapping from a dummy method to get a truly unrelated annotation
        var getMapping = ConstraintHandlersTest.class
                .getDeclaredMethod("eachHandler_doesNotSupportUnrelatedAnnotation")
                .getAnnotation(GetMapping.class);
        // Just verify each handler rejects @Min when it's not their annotation
        assertAll(
                () -> assertFalse(new MaxConstraintHandler().supports(unrelated)),
                () -> assertFalse(new DecimalMinConstraintHandler().supports(unrelated)),
                () -> assertFalse(new DecimalMaxConstraintHandler().supports(unrelated)),
                () -> assertFalse(new PositiveConstraintHandler().supports(unrelated)),
                () -> assertFalse(new PositiveOrZeroConstraintHandler().supports(unrelated)),
                () -> assertFalse(new NegativeConstraintHandler().supports(unrelated)),
                () -> assertFalse(new NegativeOrZeroConstraintHandler().supports(unrelated)),
                () -> assertFalse(new SizeConstraintHandler().supports(unrelated)),
                () -> assertFalse(new NotNullConstraintHandler().supports(unrelated)),
                () -> assertFalse(new NotBlankConstraintHandler().supports(unrelated)),
                () -> assertFalse(new NotEmptyConstraintHandler().supports(unrelated)),
                () -> assertFalse(new PatternConstraintHandler().supports(unrelated)),
                () -> assertFalse(new EmailConstraintHandler().supports(unrelated))
        );
    }
}
