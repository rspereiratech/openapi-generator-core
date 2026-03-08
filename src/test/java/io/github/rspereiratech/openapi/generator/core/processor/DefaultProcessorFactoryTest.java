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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class DefaultProcessorFactoryTest {

    private ProcessorFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultProcessorFactory();
    }

    @Test
    void createSchemaProcessor_returnsSchemaProcessorImpl() {
        SchemaProcessor sp = factory.createSchemaProcessor();
        assertNotNull(sp);
        assertInstanceOf(SchemaProcessorImpl.class, sp);
    }

    @Test
    void createSchemaProcessor_eachCallReturnsNewInstance() {
        assertNotSame(factory.createSchemaProcessor(), factory.createSchemaProcessor());
    }

    @Test
    void createParameterProcessor_returnsParameterProcessorImpl() {
        SchemaProcessor sp = factory.createSchemaProcessor();
        ParameterProcessor pp = factory.createParameterProcessor(sp);
        assertNotNull(pp);
        assertInstanceOf(ParameterProcessorImpl.class, pp);
    }

    @Test
    void createRequestBodyProcessor_returnsRequestBodyProcessorImpl() {
        SchemaProcessor sp = factory.createSchemaProcessor();
        RequestBodyProcessor rb = factory.createRequestBodyProcessor(sp);
        assertNotNull(rb);
        assertInstanceOf(RequestBodyProcessorImpl.class, rb);
    }

    @Test
    void createResponseProcessor_returnsResponseProcessorImpl() {
        SchemaProcessor sp = factory.createSchemaProcessor();
        ResponseProcessor rp = factory.createResponseProcessor(sp);
        assertNotNull(rp);
        assertInstanceOf(ResponseProcessorImpl.class, rp);
    }

    @Test
    void createOperationProcessor_returnsOperationProcessorImpl() {
        SchemaProcessor sp = factory.createSchemaProcessor();
        OperationProcessor op = factory.createOperationProcessor(
                factory.createParameterProcessor(sp),
                factory.createRequestBodyProcessor(sp),
                factory.createResponseProcessor(sp));
        assertNotNull(op);
        assertInstanceOf(OperationProcessorImpl.class, op);
    }

    @Test
    void createControllerProcessor_returnsControllerProcessorImpl() {
        ControllerProcessor cp = factory.createControllerProcessor(
                Mockito.mock(OperationProcessor.class));
        assertNotNull(cp);
        assertInstanceOf(ControllerProcessorImpl.class, cp);
    }
}
