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
package io.github.rspereiratech.openapi.generator.core.scanner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;


/**
 * Unit tests for {@link io.github.rspereiratech.openapi.generator.core.scanner.DefaultClasspathScanner}.
 *
 * <p>Covers discovery of classes annotated with {@code @RestController} (directly and via
 * composed / multi-level meta-annotations), custom annotation filtering via
 * {@code controllerAnnotations}, and exclusion of non-controller classes.
 */
class DefaultClasspathScannerTest {

    // ==========================================================================
    // Fixture annotations
    // ==========================================================================

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @RestController
    @interface Level1Composed {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Level1Composed
    @interface TestRestController {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface StandaloneEndpoint {}

    // ==========================================================================
    // Fixture classes
    // ==========================================================================

    @RestController
    @RequestMapping("/direct")
    static class DirectRestControllerClass {
        @GetMapping
        public String hello() { return "hello"; }
    }

    @TestRestController
    static class MetaAnnotatedControllerClass {
        @GetMapping("/meta")
        public String endpoint() { return "ok"; }
    }

    @StandaloneEndpoint
    static class StandaloneAnnotatedClass {}

    static class PlainClass {}

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private static final String TEST_PACKAGE =
            DefaultClasspathScannerTest.class.getPackageName();

    private static final ClassLoader CL =
            Thread.currentThread().getContextClassLoader();

    // ==========================================================================
    // isController via built-in @RestController
    // ==========================================================================

    @Test
    void scan_classWithRestController_isFound() throws Exception {
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of());

        Assertions.assertNotNull(found);
        Assertions.assertTrue(
                found.contains(DirectRestControllerClass.class),
                "DirectRestControllerClass (@RestController) must be detected"
        );
    }

    @Test
    void scan_plainClass_isNotFound() throws Exception {
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of());

        Assertions.assertFalse(
                found.contains(PlainClass.class),
                "A plain class with no annotations must not be detected as a controller"
        );
    }

    // ==========================================================================
    // Two-level meta-annotation detection (built-in)
    // ==========================================================================

    @Test
    void scan_twoLevelMetaAnnotatedClass_isFound() throws Exception {
        // TestRestController -> Level1Composed -> @RestController
        // The scanner's recursive hasControllerStereotype walk must reach @RestController.
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of());

        Assertions.assertTrue(
                found.contains(MetaAnnotatedControllerClass.class),
                "MetaAnnotatedControllerClass (two levels deep) must be detected via recursive traversal"
        );
    }

    // ==========================================================================
    // Standalone annotation – NOT detected without explicit config
    // ==========================================================================

    @Test
    void scan_standaloneAnnotationWithoutConfig_classNotFound() throws Exception {
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of());

        Assertions.assertFalse(
                found.contains(StandaloneAnnotatedClass.class),
                "StandaloneAnnotatedClass must NOT be detected when its annotation is not explicitly configured"
        );
    }

    // ==========================================================================
    // Custom annotation via additionalControllerAnnotations
    // ==========================================================================

    @Test
    void scan_standaloneAnnotationWithExplicitConfig_classIsFound() throws Exception {
        String fqn = StandaloneEndpoint.class.getName();

        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of(fqn));

        Assertions.assertTrue(
                found.contains(StandaloneAnnotatedClass.class),
                "StandaloneAnnotatedClass must be found when its annotation FQN is in additionalControllerAnnotations"
        );
    }

    @Test
    void scan_standaloneAnnotationWithExplicitConfig_doesNotAffectPlainClass() throws Exception {
        String fqn = StandaloneEndpoint.class.getName();

        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of(fqn));

        Assertions.assertFalse(
                found.contains(PlainClass.class),
                "A plain class without any annotation must never be found even with custom annotation config"
        );
    }

    // ==========================================================================
    // Package filtering
    // ==========================================================================

    @Test
    void scan_differentPackage_doesNotReturnFixtures() throws Exception {
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        // Using a package that contains no controllers
        List<Class<?>> found = scanner.scan(List.of("io.github.rspereiratech.openapi.generator.core.config"), CL, Set.of());

        Assertions.assertFalse(
                found.contains(DirectRestControllerClass.class),
                "Controllers in the test package must not be found when scanning a different package"
        );
    }

    @Test
    void scan_fixturePackage_findsKnownControllers() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(
                List.of("io.github.rspereiratech.openapi.generator.core.fixtures.integration"),
                loader, Set.of());

        Assertions.assertNotNull(found);
        Assertions.assertFalse(found.isEmpty(), "At least one controller must be found in the fixture package");

        List<String> simpleNames = found.stream().map(Class::getSimpleName).toList();
        Assertions.assertTrue(simpleNames.contains("UserController"),        "UserController must be found");
        Assertions.assertTrue(simpleNames.contains("ProductController"),     "ProductController must be found");
        Assertions.assertTrue(simpleNames.contains("OrderController"),       "OrderController must be found");
        Assertions.assertTrue(simpleNames.contains("NotificationController"),"NotificationController must be found");
    }

    // ==========================================================================
    // Interface and annotation types are excluded
    // ==========================================================================

    @Test
    void scan_interfaceWithRestController_isNotReturnedAsController() throws Exception {
        // Interfaces are excluded even if they carry a controller stereotype
        DefaultClasspathScanner scanner = new DefaultClasspathScanner();
        List<Class<?>> found = scanner.scan(List.of(TEST_PACKAGE), CL, Set.of());

        boolean anyInterface = found.stream().anyMatch(Class::isInterface);
        Assertions.assertFalse(anyInterface, "No interface should ever be included in scan results");
    }

    // ==========================================================================
    // Convenience overload (no additionalAnnotations)
    // ==========================================================================

}
