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

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for discovering Spring controller classes on the classpath.
 *
 * <p>Implementations may scan directories, JARs, or any other source of compiled classes.
 * Classes are filtered by base package and annotation type.</p>
 *
 * <p>Built-in Spring controller stereotypes ({@code @RestController}, {@code @Controller})
 * are always recognized. Additional custom annotations can be supplied via
 * {@code additionalControllerAnnotations}. Detection is recursive: classes carrying
 * composed annotations that are meta-annotated with known stereotypes are also matched.</p>
 *
 * <p>This interface allows pluggable scanning strategies for OpenAPI generation
 * without hardcoding classpath scanning logic.</p>
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.OpenApiGeneratorImpl
 */
public interface ClasspathScanner {
    /**
     * Scans the given {@code classLoader}'s accessible resources and returns
     * all classes whose package starts with one of the {@code basePackages} and
     * that are annotated with a Spring controller stereotype or with any of the
     * {@code additionalControllerAnnotations}.
     *
     * @param basePackages                  packages to restrict the scan to; must not be empty
     * @param classLoader                   classloader that owns the compiled classes; must not be {@code null}
     * @param additionalControllerAnnotations fully-qualified annotation names to treat as
     *                                       controller stereotypes in addition to the built-in Spring ones; may be empty
     * @return discovered controller classes; never {@code null}
     * @throws Exception if an unrecoverable I/O error occurs during scanning
     */
    @SuppressWarnings("java:S112")
    List<Class<?>> scan(List<String> basePackages, ClassLoader classLoader, Set<String> additionalControllerAnnotations) throws Exception;

    /**
     * Convenience overload that uses only the built-in Spring controller stereotypes.
     * Delegates to {@link #scan(List, ClassLoader, Set)}.
     *
     * @param basePackages packages to restrict the scan to; must not be empty
     * @param classLoader  classloader that owns the compiled classes; must not be {@code null}
     * @return discovered controller classes; never {@code null}
     * @throws Exception if an unrecoverable I/O error occurs during scanning
     */
    default List<Class<?>> scan(List<String> basePackages, ClassLoader classLoader) throws Exception {
        return scan(basePackages, classLoader, Set.of());
    }
}
