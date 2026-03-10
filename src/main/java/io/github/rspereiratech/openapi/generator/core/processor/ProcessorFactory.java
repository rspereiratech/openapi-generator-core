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
package io.github.rspereiratech.openapi.generator.core.processor;

import io.github.rspereiratech.openapi.generator.core.postprocessor.PostProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.controller.ControllerProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;

import java.util.List;

/**
 * Factory interface for creating the OpenAPI processor chain.
 *
 * <p>Decouples {@link io.github.rspereiratech.openapi.generator.core.OpenApiGeneratorImpl}
 * from concrete processor implementations, following the Dependency-Inversion Principle.
 * Custom implementations can substitute any processor in the chain (e.g., for testing
 * or supporting additional frameworks) without modifying the generator itself.</p>
 *
 * <p>The default production implementation is {@link DefaultProcessorFactory}.</p>
 *
 * <p>Responsibilities include creating shared or per-controller processors for:
 * <ul>
 *     <li>{@link SchemaProcessor}</li>
 *     <li>{@link ParameterProcessor}</li>
 *     <li>{@link RequestBodyProcessor}</li>
 *     <li>{@link ResponseProcessor}</li>
 *     <li>{@link OperationProcessor}</li>
 *     <li>{@link ControllerProcessor}</li>
 * </ul>
 * </p>
 *
 * @author ruispereira
 * @see DefaultProcessorFactory
 */
public interface ProcessorFactory {

    /**
     * Creates the shared schema processor that accumulates component schemas
     * across all controllers during one generation run.
     *
     * @return a new {@link SchemaProcessor} instance
     */
    SchemaProcessor createSchemaProcessor();

    /**
     * Creates the parameter processor.
     *
     * @param schemaProcessor the shared schema processor created by {@link #createSchemaProcessor()}
     * @return a new {@link ParameterProcessor} instance
     */
    ParameterProcessor createParameterProcessor(SchemaProcessor schemaProcessor);

    /**
     * Creates the request-body processor.
     *
     * @param schemaProcessor the shared schema processor
     * @return a new {@link RequestBodyProcessor} instance
     */
    RequestBodyProcessor createRequestBodyProcessor(SchemaProcessor schemaProcessor);

    /**
     * Creates the response processor.
     *
     * @param schemaProcessor the shared schema processor
     * @return a new {@link ResponseProcessor} instance
     */
    ResponseProcessor createResponseProcessor(SchemaProcessor schemaProcessor);

    /**
     * Creates the operation processor from the three lower-level processors.
     *
     * @param parameterProcessor   the parameter processor
     * @param requestBodyProcessor the request-body processor
     * @param responseProcessor    the response processor
     * @return a new {@link OperationProcessor} instance
     */
    OperationProcessor createOperationProcessor(ParameterProcessor parameterProcessor,
                                                RequestBodyProcessor requestBodyProcessor,
                                                ResponseProcessor responseProcessor);

    /**
     * Creates the controller processor that drives the full per-controller pipeline.
     *
     * @param operationProcessor the operation processor
     * @return a new {@link ControllerProcessor} instance
     */
    ControllerProcessor createControllerProcessor(OperationProcessor operationProcessor);

    /**
     * Creates the ordered list of {@link PostProcessor}s to apply after all
     * controllers have been processed.
     *
     * <p>All post-processors are always included in the returned list. Behaviour
     * that depends on configuration (e.g. path sorting) is controlled by passing
     * the relevant flag into the processor itself rather than by omitting it from
     * the list.
     *
     * @param schemaProcessor the shared schema processor, passed to processors that need it;
     *                        must not be {@code null}
     * @param sortOutput      forwarded to {@link io.github.rspereiratech.openapi.generator.core.postprocessor.SortPathsPostProcessor};
     *                        when {@code true} the processor sorts paths alphabetically,
     *                        otherwise it is a no-op
     * @return the post-processor chain; never {@code null}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    List<PostProcessor> createPostProcessors(SchemaProcessor schemaProcessor, boolean sortOutput);
}
