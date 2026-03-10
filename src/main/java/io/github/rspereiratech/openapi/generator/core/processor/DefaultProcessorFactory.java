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
import io.github.rspereiratech.openapi.generator.core.postprocessor.PruneUnreferencedSchemasPostProcessor;
import io.github.rspereiratech.openapi.generator.core.postprocessor.SchemaRegistryMergePostProcessor;
import io.github.rspereiratech.openapi.generator.core.postprocessor.SortSpecPostProcessor;
import io.github.rspereiratech.openapi.generator.core.postprocessor.UniqueOperationIdPostProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.controller.ControllerProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.controller.ControllerProcessorImpl;
import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessorImpl;
import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessorImpl;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessorImpl;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessorImpl;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessorImpl;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Default {@link ProcessorFactory} implementation.
 *
 * <p>Instantiates the standard processor chain using the concrete {@code Impl}
 * classes. This is the factory used by
 * {@link io.github.rspereiratech.openapi.generator.core.OpenApiGeneratorImpl} unless
 * a custom factory is supplied.
 *
 * @author ruispereira
 */
public class DefaultProcessorFactory implements ProcessorFactory {

    @Override
    public SchemaProcessor createSchemaProcessor() {
        return new SchemaProcessorImpl();
    }

    @Override
    public ParameterProcessor createParameterProcessor(SchemaProcessor schemaProcessor) {
        return new ParameterProcessorImpl(schemaProcessor);
    }

    @Override
    public RequestBodyProcessor createRequestBodyProcessor(SchemaProcessor schemaProcessor) {
        return new RequestBodyProcessorImpl(schemaProcessor);
    }

    @Override
    public ResponseProcessor createResponseProcessor(SchemaProcessor schemaProcessor) {
        return new ResponseProcessorImpl(schemaProcessor);
    }

    @Override
    public OperationProcessor createOperationProcessor(ParameterProcessor parameterProcessor,
                                                       RequestBodyProcessor requestBodyProcessor,
                                                       ResponseProcessor responseProcessor) {
        return new OperationProcessorImpl(parameterProcessor, requestBodyProcessor, responseProcessor);
    }

    @Override
    public ControllerProcessor createControllerProcessor(OperationProcessor operationProcessor) {
        return new ControllerProcessorImpl(operationProcessor);
    }

    @Override
    public List<PostProcessor> createPostProcessors(SchemaProcessor schemaProcessor, boolean sortOutput) {
        Preconditions.checkNotNull(schemaProcessor, "'schemaProcessor' must not be null");
        return List.of(
                new SchemaRegistryMergePostProcessor(schemaProcessor),
                new PruneUnreferencedSchemasPostProcessor(),
                new SortSpecPostProcessor(sortOutput),
                new UniqueOperationIdPostProcessor()
        );
    }
}
