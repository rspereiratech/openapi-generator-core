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

import io.github.rspereiratech.openapi.generator.core.postprocessor.PostProcessor;
import com.google.common.base.Preconditions;
import io.github.rspereiratech.openapi.generator.core.builder.OpenApiModelBuilder;
import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import io.github.rspereiratech.openapi.generator.core.processor.DefaultProcessorFactory;
import io.github.rspereiratech.openapi.generator.core.processor.ProcessorFactory;
import io.github.rspereiratech.openapi.generator.core.processor.controller.ControllerProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.scanner.ClasspathScanner;
import io.github.rspereiratech.openapi.generator.core.scanner.DefaultClasspathScanner;
import io.github.rspereiratech.openapi.generator.core.writer.WriterFactory;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Default {@link OpenApiGenerator} implementation.
 *
 * <p>Orchestrates the full generation pipeline:
 * <ol>
 *   <li>Build the initial {@link OpenAPI} model (info, servers, security) via {@link OpenApiModelBuilder}.</li>
 *   <li>Scan the classpath for Spring MVC controller classes.</li>
 *   <li>Process each controller to extract paths and operations.</li>
 *   <li>Merge all component schemas collected during processing.</li>
 *   <li>Run each {@link PostProcessor} in order.</li>
 *   <li>Serialise the result to YAML or JSON (driven by
 *       {@link io.github.rspereiratech.openapi.generator.core.config.OutputFormat} in the config).</li>
 * </ol>
 *
 * <p>The processor chain is created by a {@link ProcessorFactory}. Pass a custom
 * factory to substitute any processor implementation without subclassing this class.
 *
 * <p>The class is designed to be usable from any build tool or standalone
 * program — it has no dependency on Maven or Spring Boot itself.
 *
 * @author ruispereira
 */
@Slf4j
public class OpenApiGeneratorImpl implements OpenApiGenerator {

    /** Scans the classpath to discover controller classes. */
    private final ClasspathScanner scanner;
    /** Factory used to create the processor chain for each generation run. */
    private final ProcessorFactory factory;

    /** Creates a generator using the default processor chain and default classpath scanner. */
    public OpenApiGeneratorImpl() {
        this(new DefaultProcessorFactory());
    }

    /**
     * Creates a generator with a custom {@link ProcessorFactory}.
     *
     * <p>Use this constructor to substitute any processor in the chain (e.g. for
     * testing or to add support for additional frameworks) without modifying this class.
     *
     * @param factory the factory used to create the processor chain
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public OpenApiGeneratorImpl(ProcessorFactory factory) {
        Preconditions.checkNotNull(factory, "'factory' must not be null");
        this.scanner = new DefaultClasspathScanner();
        this.factory = factory;
    }

    @Override
    public void generate(GeneratorConfig config, ClassLoader classLoader) throws Exception {
        Preconditions.checkNotNull(config,      "'config' must not be null");
        Preconditions.checkNotNull(classLoader, "'classLoader' must not be null");
        log.info("Starting OpenAPI generation for packages: {}", config.basePackages());

        // Build a fresh processor chain (avoids schema registry accumulation across calls).
        SchemaProcessor      sp           = factory.createSchemaProcessor();
        ParameterProcessor   paramProc    = factory.createParameterProcessor(sp);
        RequestBodyProcessor bodyProc     = factory.createRequestBodyProcessor(sp);
        ResponseProcessor    responseProc = factory.createResponseProcessor(sp);
        OperationProcessor   opProc       = factory.createOperationProcessor(paramProc, bodyProc, responseProc);
        ControllerProcessor  cp           = factory.createControllerProcessor(opProc);

        // 1 – Build initial model (info, servers, security schemes)
        OpenAPI openAPI = new OpenApiModelBuilder(config).build();

        // 2 – Scan for controllers
        List<Class<?>> controllers = scanner.scan(
                config.basePackages(), classLoader,
                Set.copyOf(config.controllerAnnotations()));
        if (controllers.isEmpty()) {
            log.warn("No controllers found in packages: {}", config.basePackages());
        }

        // 2a – Sort controllers for deterministic output (when enabled)
        if (config.sortOutput()) {
            controllers = controllers.stream()
                    .sorted(Comparator.comparing(Class::getCanonicalName))
                    .toList();
        }

        // 3 – Process each controller
        for (Class<?> controller : controllers) {
            try {
                cp.process(controller, openAPI);
            } catch (Exception e) {
                log.error("Failed to process controller {}: {}", controller.getName(), e.getMessage(), e);
            }
        }

        // 4 – Post-process
        factory.createPostProcessors(sp, config.sortOutput()).forEach(p -> p.process(openAPI));

        // 6 – Write output
        WriterFactory.create(config).write(openAPI, Path.of(config.outputFile()));
        log.info("Generation complete.");
    }
}
