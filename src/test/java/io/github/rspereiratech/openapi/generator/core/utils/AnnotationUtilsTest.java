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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


class AnnotationUtilsTest {

    // ==========================================================================
    // Fixture annotations for meta-annotation tests
    // ==========================================================================

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @RestController
    @interface Level1Composed {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Level1Composed
    @interface Level2Composed {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Unrelated {}

    // ==========================================================================
    // Fixture classes / interfaces for getAllAnnotations tests
    // ==========================================================================

    @RequestMapping("/base")
    interface BaseApi {
        @GetMapping("/item")
        String read(@RequestParam String q);

        @PostMapping("/item")
        String create(@RequestParam String body);
    }

    @RequestMapping("/abstract")
    abstract static class AbstractBase {
        @DeleteMapping("/entry")
        public abstract void remove(@PathVariable Long id);
    }

    @RestController
    static class ConcreteController extends AbstractBase implements BaseApi {

        @Override
        @GetMapping("/overridden")
        public String read(@RequestParam String q) {
            return q;
        }

        @Override
        public String create(@RequestParam String body) {
            return body;
        }

        @Override
        public void remove(@PathVariable Long id) { /* no-op */ }
    }

    @RestController
    static class OverridelessController implements BaseApi {
        @Override
        public String read(@RequestParam String q) {
            return q;
        }

        @Override
        public String create(@RequestParam String body) {
            return body;
        }
    }

    // ==========================================================================
    // isMetaAnnotated
    // ==========================================================================

    @Test
    void isMetaAnnotated_directNameMatch_returnsTrue() {
        Assertions.assertTrue(AnnotationUtils.isMetaAnnotated(RestController.class, "RestController"));
    }

    @Test
    void isMetaAnnotated_oneLevelDeep_returnsTrue() {
        Assertions.assertTrue(AnnotationUtils.isMetaAnnotated(Level1Composed.class, "RestController"));
    }

    @Test
    void isMetaAnnotated_twoLevelsDeep_returnsTrue() {
        Assertions.assertTrue(AnnotationUtils.isMetaAnnotated(Level2Composed.class, "RestController"));
    }

    @Test
    void isMetaAnnotated_cycleGuard_doesNotInfiniteLoop() {
        boolean result = AnnotationUtils.isMetaAnnotated(Retention.class, "NonExistentAnnotation");
        Assertions.assertFalse(result);
    }

    @Test
    void isMetaAnnotated_unrelatedAnnotation_returnsFalse() {
        Assertions.assertFalse(AnnotationUtils.isMetaAnnotated(Unrelated.class, "RestController"));
    }

    // ==========================================================================
    // getAllAnnotations(Method)
    // ==========================================================================

    @Test
    void getAllAnnotations_method_concreteAnnotationWinsOverInterface() throws Exception {
        Method concreteRead = ConcreteController.class.getMethod("read", String.class);
        List<Annotation> anns = AnnotationUtils.getAllAnnotations(concreteRead);

        Assertions.assertEquals(
                1,
                anns.stream().filter(GetMapping.class::isInstance).count(),
                "Only one GetMapping annotation should be present"
        );

        GetMapping gm = anns.stream()
                .filter(a -> a instanceof GetMapping)
                .map(a -> (GetMapping) a)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("/overridden", gm.value()[0]);
    }

    @Test
    void getAllAnnotations_method_interfaceAnnotationInheritedWhenNotOverridden() throws Exception {
        Method method = OverridelessController.class.getMethod("read", String.class);
        List<Annotation> anns = AnnotationUtils.getAllAnnotations(method);

        Assertions.assertTrue(
                anns.stream().anyMatch(GetMapping.class::isInstance),
                "Interface @GetMapping should be inherited when the concrete method has no annotation");
    }

    @Test
    void getAllAnnotations_method_bothAnnotationsPresentWhenDifferentTypes() throws Exception {
        Method method = ConcreteController.class.getMethod("remove", Long.class);
        List<Annotation> anns = AnnotationUtils.getAllAnnotations(method);

        Assertions.assertTrue(
                anns.stream().anyMatch(DeleteMapping.class::isInstance),
                "@DeleteMapping from AbstractBase should be visible on the concrete override"
        );
    }

    // ==========================================================================
    // getAllParameterAnnotations(Method, int)
    // ==========================================================================

    @Test
    void getAllParameterAnnotations_inheritedFromInterface() throws Exception {
        Method method = OverridelessController.class.getMethod("read", String.class);
        Annotation[] paramAnns = AnnotationUtils.getAllParameterAnnotations(method, 0);

        Assertions.assertTrue(
                Arrays.stream(paramAnns).anyMatch(RequestParam.class::isInstance),
                "@RequestParam on the interface parameter should be inherited by the concrete override"
        );
    }

    @Test
    void getAllParameterAnnotations_concreteAnnotationWinsOverInterface() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        Annotation[] paramAnns = AnnotationUtils.getAllParameterAnnotations(method, 0);

        Assertions.assertTrue(
                Arrays.stream(paramAnns).anyMatch(RequestParam.class::isInstance),
                "@RequestParam should be present on parameter 0"
        );
    }

    // ==========================================================================
    // getAllAnnotations(Class<?>)
    // ==========================================================================

    @Test
    void getAllAnnotations_class_concreteAnnotationWinsOverInterface() {
        List<Annotation> anns = AnnotationUtils.getAllAnnotations(ConcreteController.class);

        Assertions.assertTrue(
                anns.stream().anyMatch(RestController.class::isInstance),
                "@RestController on the concrete class must be present"
        );

        Assertions.assertTrue(
                anns.stream().anyMatch(RequestMapping.class::isInstance),
                "@RequestMapping from a supertype should be inherited"
        );
    }

    @Test
    void getAllAnnotations_class_returnsAnnotationsFromInterface() {
        List<Annotation> anns = AnnotationUtils.getAllAnnotations(OverridelessController.class);

        Assertions.assertTrue(
                anns.stream().anyMatch(RequestMapping.class::isInstance),
                "@RequestMapping from the interface should appear on the class view");
    }

    // ==========================================================================
    // findAnnotation
    // ==========================================================================

    @Test
    void findAnnotation_directMatch_returnsAnnotation() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        java.util.Optional<GetMapping> result = AnnotationUtils.findAnnotation(method, GetMapping.class);
        Assertions.assertTrue(result.isPresent(), "Direct @GetMapping must be found");
        Assertions.assertEquals("/overridden", result.get().value()[0]);
    }

    @Test
    void findAnnotation_metaAnnotationMatch_returnsMetaAnnotation() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        java.util.Optional<RequestMapping> result = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        Assertions.assertTrue(result.isPresent(), "@RequestMapping via meta-annotation on @GetMapping must be found");
    }

    @Test
    void findAnnotation_notPresent_returnsEmpty() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        java.util.Optional<PostMapping> result = AnnotationUtils.findAnnotation(method, PostMapping.class);
        Assertions.assertTrue(result.isEmpty(), "Absent annotation must return empty Optional");
    }

    @Test
    void findAnnotation_onClass_directMatch() {
        java.util.Optional<RestController> result = AnnotationUtils.findAnnotation(
                ConcreteController.class, RestController.class);
        Assertions.assertTrue(result.isPresent(), "@RestController directly on the class must be found");
    }

    // ==========================================================================
    // collectAllBySimpleName
    // ==========================================================================

    @Test
    void collectAllBySimpleName_multipleHierarchyLevels_returnsAllMatches() {
        List<Annotation> found = AnnotationUtils.collectAllBySimpleName(ConcreteController.class, "RequestMapping");
        long count = found.stream().filter(RequestMapping.class::isInstance).count();
        Assertions.assertEquals(2, count,
                "Both @RequestMapping annotations (from AbstractBase and BaseApi) must be collected");
    }

    @Test
    void collectAllBySimpleName_singleAnnotation_returnsSingle() {
        List<Annotation> found = AnnotationUtils.collectAllBySimpleName(OverridelessController.class, "RequestMapping");
        Assertions.assertEquals(1, found.stream().filter(RequestMapping.class::isInstance).count(),
                "Exactly one @RequestMapping should be found");
        RequestMapping rm = found.stream()
                .filter(RequestMapping.class::isInstance)
                .map(a -> (RequestMapping) a)
                .findFirst().orElseThrow();
        Assertions.assertEquals("/base", rm.value()[0]);
    }

    @Test
    void collectAllBySimpleName_noMatch_returnsEmptyList() {
        List<Annotation> found = AnnotationUtils.collectAllBySimpleName(ConcreteController.class, "NonExistent");
        Assertions.assertTrue(found.isEmpty(), "No annotations should be found for a non-existent simple name");
    }

    // ==========================================================================
    // findAnnotationsBySimpleName
    // ==========================================================================

    @Test
    void findAnnotationsBySimpleName_matchingAnnotation_returnsIt() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        List<Annotation> found = AnnotationUtils.findAnnotationsBySimpleName(method, "GetMapping");
        Assertions.assertFalse(found.isEmpty(), "@GetMapping must be found by simple name");
        Assertions.assertTrue(found.get(0) instanceof GetMapping);
    }

    @Test
    void findAnnotationsBySimpleName_multipleNames_returnsAllMatches() {
        List<Annotation> found = AnnotationUtils.findAnnotationsBySimpleName(
                ConcreteController.class, "RestController", "Deprecated");
        Assertions.assertFalse(found.isEmpty(), "At least @RestController must be found");
        Assertions.assertTrue(found.stream().anyMatch(a -> a instanceof RestController));
    }

    @Test
    void findAnnotationsBySimpleName_noMatch_returnsEmptyList() throws Exception {
        Method method = ConcreteController.class.getMethod("read", String.class);
        List<Annotation> found = AnnotationUtils.findAnnotationsBySimpleName(method, "NonExistent");
        Assertions.assertTrue(found.isEmpty(), "Non-existent annotation must yield empty list");
    }
}
