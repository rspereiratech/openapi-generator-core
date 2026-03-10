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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Default {@link ClasspathScanner} implementation.
 *
 * <p>Walks every URL exposed by the provided {@link ClassLoader} (directories
 * and JARs) and loads each class whose fully-qualified name starts with one of
 * the requested base-packages.  A class is considered a "controller" when it
 * carries a known controller stereotype, detected recursively through the full
 * meta-annotation chain (supports custom composed annotations).
 *
 * <h3>Controller detection</h3>
 * <p>The built-in stereotypes are {@code @RestController} and {@code @Controller}.
 * Additional annotation FQNs can be supplied at scan time via
 * {@link #scan(List, ClassLoader, Set)} to extend detection without modifying
 * the generator itself (useful for project-specific composed annotations that do
 * <em>not</em> meta-annotate a Spring stereotype).
 *
 * @author ruispereira
 */
@Slf4j
public class DefaultClasspathScanner implements ClasspathScanner {
    /** File extension for compiled Java class files. */
    private static final String CLASS_EXTENSION = ".class";

    /** File extension for JAR archives. */
    private static final String JAR_EXTENSION = ".jar";

    /** Built-in FQNs that always identify a Spring MVC controller. */
    private static final Set<String> BUILTIN_CONTROLLER_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller"
    );

    // ------------------------------------------------------------------
    // ClasspathScanner
    // ------------------------------------------------------------------

    @Override
    public List<Class<?>> scan(List<String> basePackages, ClassLoader classLoader, Set<String> additionalControllerAnnotations) throws Exception {
        Preconditions.checkNotNull(basePackages,"'basePackages' must not be null");
        Preconditions.checkNotNull(classLoader,"'classLoader' must not be null");
        Preconditions.checkNotNull(additionalControllerAnnotations,"'additionalControllerAnnotations' must not be null");

        Set<String> effectiveAnnotations = additionalControllerAnnotations.isEmpty()
                ? BUILTIN_CONTROLLER_ANNOTATIONS
                : merge(additionalControllerAnnotations);

        List<Class<?>> controllers = new ArrayList<>();
        URL[] urls = resolveUrls(classLoader);

        for (URL url : urls) {
            if ("file".equals(url.getProtocol())) {
                File resource = new File(url.toURI());
                if (resource.isDirectory()) {
                    scanDirectory(resource, basePackages, classLoader, effectiveAnnotations, controllers);
                } else if (resource.getName().endsWith(JAR_EXTENSION)) {
                    scanJar(resource, basePackages, classLoader, effectiveAnnotations, controllers);
                }
            }
        }

        log.info("Classpath scan complete – {} controller(s) found in packages {}",
                controllers.size(), basePackages);
        return controllers;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Returns a new immutable set containing all elements from
     * {@link #BUILTIN_CONTROLLER_ANNOTATIONS} plus {@code extra}.
     *
     * @param extra the additional annotation FQNs to include
     * @return an immutable union of the built-in and extra annotation FQNs
     */
    private static Set<String> merge(Set<String> extra) {
        Set<String> merged = new HashSet<>(BUILTIN_CONTROLLER_ANNOTATIONS);
        merged.addAll(extra);
        return Set.copyOf(merged);
    }

    /**
     * Extracts the URL entries from the given class-loader.
     *
     * <p>For {@link URLClassLoader} instances the URLs are read directly.
     * For Java 9+ application class-loaders (which no longer extend
     * {@code URLClassLoader}), the system {@code java.class.path} property is
     * parsed as a fallback.
     *
     * @param classLoader the class-loader to interrogate
     * @return the array of URLs that make up the effective classpath
     */
    private URL[] resolveUrls(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader ucl) {
            return ucl.getURLs();
        }
        // Java 9+ application class-loader does not extend URLClassLoader; fall back to system class-path.
        String cp = System.getProperty("java.class.path", "");
        return convertClasspathToUrls(cp);
    }

    /**
     * Converts a {@link File#pathSeparator}-delimited classpath string to an
     * array of {@link URL} objects, silently skipping any entries that cannot
     * be converted.
     *
     * @param classpath the raw classpath string (e.g. the value of {@code java.class.path})
     * @return an array of URLs corresponding to the valid classpath entries
     */
    private URL[] convertClasspathToUrls(String classpath) {
        return Arrays.stream(classpath.split(File.pathSeparator))
                .map(this::toUrl)
                .flatMap(Optional::stream)
                .toArray(URL[]::new);
    }

    /**
     * Attempts to convert a single classpath entry string to a {@link URL}.
     *
     * @param entry a classpath entry (file path or JAR path)
     * @return an {@link Optional} containing the URL, or empty if the entry is invalid
     */
    private Optional<URL> toUrl(String entry) {
        try {
            return Optional.of(new File(entry).toURI().toURL());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Walks a directory tree of compiled {@code .class} files and collects controllers.
     *
     * @param root                  the classpath root directory, used for deriving class names
     * @param basePackages          package filters applied to each candidate class name
     * @param classLoader           class-loader used to load candidate classes
     * @param controllerAnnotations effective set of controller annotation FQNs
     * @param result                accumulator for discovered controller classes
     */
    private void scanDirectory(File root, List<String> basePackages,
                               ClassLoader classLoader,
                               Set<String> controllerAnnotations,
                               List<Class<?>> result) {
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(p -> !Files.isDirectory(p) && p.toString().endsWith(CLASS_EXTENSION))
                    .map(p -> deriveClassName(root, p.toFile()))
                    .forEach(className -> tryLoadController(className, basePackages, classLoader, controllerAnnotations, result));
        } catch (IOException e) {
            log.warn("Could not scan directory {}: {}", root, e.getMessage());
        }
    }

    /**
     * Walks all {@code .class} entries of a JAR file and collects controllers.
     *
     * @param jar                   the JAR file to scan
     * @param basePackages          package filters applied to each candidate class name
     * @param classLoader           class-loader used to load candidate classes
     * @param controllerAnnotations effective set of controller annotation FQNs
     * @param result                accumulator for discovered controller classes
     */
    private void scanJar(File jar, List<String> basePackages, ClassLoader classLoader,
                         Set<String> controllerAnnotations, List<Class<?>> result) {
        try (JarFile jarFile = new JarFile(jar)) {
            jarFile.stream()
                    .filter(e -> !e.isDirectory() && e.getName().endsWith(CLASS_EXTENSION))
                    .map(e -> e.getName().replace('/', '.').replace(CLASS_EXTENSION, ""))
                    .forEach(className -> tryLoadController(className, basePackages, classLoader, controllerAnnotations, result));
        } catch (Exception e) {
            log.debug("Could not scan JAR {}: {}", jar, e.getMessage());
        }
    }

    /**
     * Derives a fully-qualified class name from a {@code .class} file by relativising
     * its path against the classpath root and replacing path separators with dots.
     *
     * @param root      the classpath root directory
     * @param classFile the {@code .class} file to convert
     * @return the fully-qualified class name (e.g. {@code com.example.MyClass})
     */
    private String deriveClassName(File root, File classFile) {
        return root.toPath().relativize(classFile.toPath())
                .toString()
                .replace(File.separatorChar, '.')
                .replace(CLASS_EXTENSION, "");
    }

    /**
     * Loads {@code className} via {@code classLoader} and adds it to {@code result}
     * if it matches a base package and carries a controller stereotype.
     *
     * <p>{@link Throwable} is caught broadly because {@link NoClassDefFoundError}
     * is common for inner or anonymous classes whose dependencies are not on the
     * scanner's classpath — these are silently skipped at {@code TRACE} level.
     *
     * @param className             the fully-qualified class name to load
     * @param basePackages          package filters; the class is skipped if it matches none
     * @param classLoader           class-loader used to load the class
     * @param controllerAnnotations effective set of controller annotation FQNs
     * @param result                accumulator for discovered controller classes
     */
    private void tryLoadController(String className, List<String> basePackages,
                                   ClassLoader classLoader,
                                   Set<String> controllerAnnotations,
                                   List<Class<?>> result) {
        if (!matchesAnyPackage(className, basePackages)) return;

        try {
            Class<?> clazz = classLoader.loadClass(className);
            if (isController(clazz, controllerAnnotations)) {
                log.debug("Found controller: {}", className);
                result.add(clazz);
            }
        } catch (Throwable e) {
            // NoClassDefFoundError is common for inner/anonymous classes whose
            // dependencies aren't on the scanner's classpath – silently skip.
            log.trace("Skipping class {}: {}", className, e.getMessage());
        }
    }

    /**
     * Returns {@code true} if {@code className} belongs to at least one of the
     * given base packages.
     *
     * <p>A class is considered a match when its fully-qualified name either equals
     * the package name (for top-level types) or starts with {@code "<package>."}.
     *
     * @param className    the fully-qualified class name to test
     * @param basePackages the list of package prefixes to match against
     * @return {@code true} if the class is in one of the specified packages
     */
    private boolean matchesAnyPackage(String className, List<String> basePackages) {
        return basePackages.stream()
                .anyMatch(pkg -> className.startsWith(pkg + ".") || className.equals(pkg));
    }

    /**
     * Returns {@code true} if {@code clazz} carries a controller stereotype —
     * one of the known FQNs in {@code controllerAnnotations} — either directly
     * or via any depth of meta-annotation traversal.
     *
     * <p>Interfaces, annotation types, and enums are excluded. Checking by FQN
     * avoids requiring Spring on the generator's own classpath.
     *
     * @param clazz                 the class to test
     * @param controllerAnnotations effective set of controller annotation FQNs
     * @return {@code true} if {@code clazz} is annotated with a controller stereotype
     */
    private boolean isController(Class<?> clazz, Set<String> controllerAnnotations) {
        if (clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum()) return false;
        return Arrays.stream(clazz.getAnnotations())
                .anyMatch(ann -> hasControllerStereotype(ann.annotationType(), new HashSet<>(), controllerAnnotations));
    }

    /**
     * Recursively checks whether {@code type} is, or is transitively meta-annotated
     * by, one of the known controller annotation FQNs.
     *
     * <p>A visited set prevents infinite recursion through mutually meta-annotated
     * annotation types (e.g. {@code @Documented} ↔ {@code @Documented}).
     *
     * @param type                  the annotation type to inspect
     * @param visited               set of already-visited annotation types (cycle guard)
     * @param controllerAnnotations set of FQNs that identify a controller stereotype
     * @return {@code true} if {@code type} is or inherits a controller stereotype
     */
    private boolean hasControllerStereotype(Class<? extends Annotation> type, Set<Class<? extends Annotation>> visited, Set<String> controllerAnnotations) {
        if (!visited.add(type)) return false;
        if (controllerAnnotations.contains(type.getName())) return true;
        return Arrays.stream(type.getAnnotations())
                .anyMatch(meta -> hasControllerStereotype(meta.annotationType(), visited, controllerAnnotations));
    }
}
