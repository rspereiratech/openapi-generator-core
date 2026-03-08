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
package io.github.rspereiratech.openapi.generator.core;

import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;

/**
 * Contract for OpenAPI specification generators.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *     <li>Scanning application classes on the classpath</li>
 *     <li>Processing Spring MVC controllers, operations, parameters, request bodies, and responses</li>
 *     <li>Accumulating component schemas</li>
 *     <li>Writing the resulting OpenAPI document to disk in the desired format</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * GeneratorConfig config = GeneratorConfig.builder()
 *     .basePackage("com.example.myapp")
 *     .title("My API")
 *     .version("2.0.0")
 *     .serverUrl("https://api.example.com")
 *     .outputFile("target/openapi.yaml")
 *     .build();
 *
 * new OpenApiGeneratorImpl().generate(config, myClassLoader);
 * }</pre>
 *
 * <p><strong>Note:</strong> The caller should set the correct
 * {@link Thread#setContextClassLoader(ClassLoader) context class loader} before
 * calling {@link #generate}, so that reflective schema building (e.g., via Jackson)
 * can access the application classes.</p>
 *
 * @author ruispereira
 * @see OpenApiGeneratorImpl
 */
public interface OpenApiGenerator {

    /**
     * Runs the full OpenAPI generation pipeline.
     *
     * @param config      generation parameters; must not be {@code null}
     * @param classLoader classloader that owns the compiled application classes; must not be {@code null}
     * @throws Exception if scanning fails, I/O errors occur, or classpath issues prevent processing
     */
    @SuppressWarnings("java:S112")
    void generate(GeneratorConfig config, ClassLoader classLoader) throws Exception;
}
