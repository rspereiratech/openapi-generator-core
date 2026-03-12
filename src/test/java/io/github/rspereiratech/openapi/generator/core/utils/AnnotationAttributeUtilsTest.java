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
package io.github.rspereiratech.openapi.generator.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

/**
 * Unit tests for {@link io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils}.
 *
 * <p>Covers retrieval of string, class, boolean, and array attribute values from
 * annotations, including aliased attributes and edge cases such as missing attributes
 * and non-annotation-type arrays.
 */
class AnnotationAttributeUtilsTest {

    // ==========================================================================
    // Fixtures
    // ==========================================================================

    interface SampleApi {
        @GetMapping("/item")
        String read(@RequestParam String q);
    }

    static class SampleController implements SampleApi {
        @Override
        @GetMapping("/overridden")
        public String read(@RequestParam String q) { return q; }
    }

    interface PathlessController {
        @GetMapping
        String noPath();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface StringAttrAnnotation {
        String label() default "";
    }

    @StringAttrAnnotation(label = "hello")
    static class StringAttrClass {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface BooleanAttrAnnotation {
        boolean active() default false;
    }

    @BooleanAttrAnnotation(active = true)
    static class BooleanAttrClass {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface IntAttrAnnotation {
        int count() default 0;
        String label() default "";
    }

    @IntAttrAnnotation(count = 42, label = "test")
    static class IntAttrClass {}

    // --- fixtures for annotation/class attribute tests ---

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface InnerAnnotation {
        String value() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnotationAttrAnnotation {
        InnerAnnotation nested() default @InnerAnnotation("x");
        InnerAnnotation[] nestedArray() default { @InnerAnnotation("a"), @InnerAnnotation("b") };
        Class<?> impl() default Void.class;
    }

    @AnnotationAttrAnnotation(
            nested = @InnerAnnotation("hello"),
            nestedArray = { @InnerAnnotation("one"), @InnerAnnotation("two") },
            impl = String.class)
    static class AnnotationAttrClass {}

    // ==========================================================================
    // extractPath
    // ==========================================================================

    @Test
    void extractPath_returnsValueAttribute() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        Assertions.assertNotNull(gm);
        Assertions.assertEquals("/overridden", AnnotationAttributeUtils.extractPath(gm));
    }

    @Test
    void extractPath_returnsEmptyStringWhenNoPathDefined() throws Exception {
        Method method = PathlessController.class.getMethod("noPath");
        GetMapping gm = method.getAnnotation(GetMapping.class);
        Assertions.assertNotNull(gm);
        Assertions.assertEquals("", AnnotationAttributeUtils.extractPath(gm));
    }

    // ==========================================================================
    // getStringArrayValue
    // ==========================================================================

    @Test
    void getStringArrayValue_valueAttribute_returnsValues() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        List<String> values = AnnotationAttributeUtils.getStringArrayValue(gm, "value");
        Assertions.assertEquals(List.of("/overridden"), values);
    }

    @Test
    void getStringArrayValue_nonArrayAttribute_returnsEmptyList() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        List<String> values = AnnotationAttributeUtils.getStringArrayValue(gm, "produces");
        Assertions.assertTrue(values.isEmpty(), "Empty String[] attribute must return empty list");
    }

    @Test
    void getStringArrayValue_absentMethod_returnsEmptyList() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        List<String> values = AnnotationAttributeUtils.getStringArrayValue(gm, "nonExistentMethod");
        Assertions.assertTrue(values.isEmpty(), "Absent method must return empty list");
    }

    // ==========================================================================
    // getStringAttribute
    // ==========================================================================

    @Test
    void getStringAttribute_existingStringAttribute_returnsValue() {
        Annotation ann = StringAttrClass.class.getAnnotation(StringAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertEquals("hello", AnnotationAttributeUtils.getStringAttribute(ann, "label"));
    }

    @Test
    void getStringAttribute_nonStringAttribute_returnsEmptyString() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        Assertions.assertNotNull(gm);
        Assertions.assertEquals("", AnnotationAttributeUtils.getStringAttribute(gm, "value"),
                "getStringAttribute must return '' when the attribute type is not String");
    }

    @Test
    void getStringAttribute_absentAttribute_returnsEmptyString() throws Exception {
        Method method = SampleController.class.getMethod("read", String.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        Assertions.assertEquals("", AnnotationAttributeUtils.getStringAttribute(gm, "nonExistentAttr"));
    }

    // ==========================================================================
    // getBooleanAttribute
    // ==========================================================================

    @Test
    void getBooleanAttribute_existingBooleanAttribute_returnsValue() {
        Annotation ann = BooleanAttrClass.class.getAnnotation(BooleanAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertTrue(AnnotationAttributeUtils.getBooleanAttribute(ann, "active", false));
    }

    @Test
    void getBooleanAttribute_absentAttribute_returnsDefault() {
        Annotation ann = BooleanAttrClass.class.getAnnotation(BooleanAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertTrue(AnnotationAttributeUtils.getBooleanAttribute(ann, "nonExistent", true),
                "Should return the supplied defaultValue when the attribute does not exist");
    }

    @Test
    void getBooleanAttribute_absentAttributeDefaultFalse_returnsFalse() {
        Annotation ann = BooleanAttrClass.class.getAnnotation(BooleanAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertFalse(AnnotationAttributeUtils.getBooleanAttribute(ann, "nonExistent", false));
    }

    // ==========================================================================
    // getIntAttribute
    // ==========================================================================

    @Test
    void getIntAttribute_existingIntAttribute_returnsValue() {
        Annotation ann = IntAttrClass.class.getAnnotation(IntAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertEquals(42, AnnotationAttributeUtils.getIntAttribute(ann, "count", 0));
    }

    @Test
    void getIntAttribute_absentAttribute_returnsDefault() {
        Annotation ann = IntAttrClass.class.getAnnotation(IntAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertEquals(99, AnnotationAttributeUtils.getIntAttribute(ann, "nonExistent", 99),
                "Absent attribute must return the supplied defaultValue");
    }

    @Test
    void getIntAttribute_nonIntAttribute_returnsDefault() {
        Annotation ann = IntAttrClass.class.getAnnotation(IntAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        Assertions.assertEquals(7, AnnotationAttributeUtils.getIntAttribute(ann, "label", 7),
                "Non-int attribute must return the supplied defaultValue");
    }

    // ==========================================================================
    // getAnnotationAttribute
    // ==========================================================================

    @Test
    void getAnnotationAttribute_existingAnnotationAttribute_returnsPresent() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getAnnotationAttribute(ann, "nested");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertInstanceOf(InnerAnnotation.class, result.get());
    }

    @Test
    void getAnnotationAttribute_absentAttribute_returnsEmpty() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getAnnotationAttribute(ann, "nonExistent");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getAnnotationAttribute_nonAnnotationAttribute_returnsEmpty() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        // "impl" is a Class<?>, not an Annotation
        var result = AnnotationAttributeUtils.getAnnotationAttribute(ann, "impl");
        Assertions.assertTrue(result.isEmpty(), "Class<?> attribute must not be returned as Annotation");
    }

    // ==========================================================================
    // getAnnotationArrayAttribute
    // ==========================================================================

    @Test
    void getAnnotationArrayAttribute_existingArrayAttribute_returnsList() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "nestedArray");
        Assertions.assertEquals(2, result.size());
        result.forEach(a -> Assertions.assertInstanceOf(InnerAnnotation.class, a));
    }

    @Test
    void getAnnotationArrayAttribute_absentAttribute_returnsEmptyList() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "nonExistent");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getAnnotationArrayAttribute_nonArrayAttribute_returnsEmptyList() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        // "nested" is a single Annotation, not Annotation[]
        var result = AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "nested");
        Assertions.assertTrue(result.isEmpty(), "Single-annotation attribute must not be returned as list");
    }

    // ==========================================================================
    // getClassAttribute
    // ==========================================================================

    @Test
    void getClassAttribute_existingClassAttribute_returnsPresent() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getClassAttribute(ann, "impl");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(String.class, result.get());
    }

    @Test
    void getClassAttribute_absentAttribute_returnsEmpty() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        var result = AnnotationAttributeUtils.getClassAttribute(ann, "nonExistent");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getClassAttribute_nonClassAttribute_returnsEmpty() {
        Annotation ann = AnnotationAttrClass.class.getAnnotation(AnnotationAttrAnnotation.class);
        Assertions.assertNotNull(ann);
        // "nested" is an Annotation, not a Class<?>
        var result = AnnotationAttributeUtils.getClassAttribute(ann, "nested");
        Assertions.assertTrue(result.isEmpty(), "Annotation attribute must not be returned as Class<?>");
    }

    // ==========================================================================
    // tryParse
    // ==========================================================================

    @Test
    void tryParse_validInput_returnsParsedValue() {
        var result = AnnotationAttributeUtils.tryParse("123.45", BigDecimal::new);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(new BigDecimal("123.45"), result.get());
    }

    @Test
    void tryParse_invalidInput_returnsEmpty() {
        var result = AnnotationAttributeUtils.tryParse("not-a-number", BigDecimal::new);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void tryParse_parserThrowsRuntimeException_returnsEmpty() {
        var result = AnnotationAttributeUtils.tryParse("x", value -> {
            throw new IllegalArgumentException("bad value");
        });
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void tryParse_zero_returnsParsedValue() {
        var result = AnnotationAttributeUtils.tryParse("0", BigDecimal::new);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(BigDecimal.ZERO, result.get().stripTrailingZeros());
    }
}
