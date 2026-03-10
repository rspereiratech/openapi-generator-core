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
package io.github.rspereiratech.openapi.generator.core.processor.request;

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestBodyProcessorTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private RequestBodyProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenReturn(new Schema<>());
        lenient().when(schemaProcessor.toSchema(any(), any())).thenReturn(new Schema<>());
        processor = new RequestBodyProcessorImpl(schemaProcessor);
    }

    // ==========================================================================
    // Fixture
    // ==========================================================================

    @SuppressWarnings("unused")
    static class Fixtures {
        @PostMapping
        public String withBody(@org.springframework.web.bind.annotation.RequestBody String body) { return ""; }

        @PostMapping
        public String withOptionalBody(@org.springframework.web.bind.annotation.RequestBody(required = false) String body) { return ""; }

        @GetMapping
        public String withoutBody(@RequestParam String q) { return ""; }

        @PostMapping(consumes = "application/xml")
        public String withXmlBody(@org.springframework.web.bind.annotation.RequestBody String body) { return ""; }

        @PostMapping
        public String bodyAsSecondParam(@RequestParam String q, @org.springframework.web.bind.annotation.RequestBody String body) { return ""; }

        @PostMapping
        public String withSwaggerRequestBodyOverride(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        content = @io.swagger.v3.oas.annotations.media.Content(
                                schema = @io.swagger.v3.oas.annotations.media.Schema(
                                        implementation = String.class)))
                @org.springframework.web.bind.annotation.RequestBody String body) { return ""; }
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, params);
    }

    // ==========================================================================
    // Presence / absence
    // ==========================================================================

    @Test
    void methodWithRequestBody_returnsNonEmpty() throws Exception {
        Optional<RequestBody> result = processor.processRequestBody(method("withBody", String.class));
        assertTrue(result.isPresent());
    }

    @Test
    void methodWithoutRequestBody_returnsEmpty() throws Exception {
        Optional<RequestBody> result = processor.processRequestBody(method("withoutBody", String.class));
        assertTrue(result.isEmpty());
    }

    // ==========================================================================
    // Required flag
    // ==========================================================================

    @Test
    void requestBody_defaultRequired_isTrue() throws Exception {
        RequestBody body = processor.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertTrue(body.getRequired());
    }

    @Test
    void requestBody_requiredFalse_isNotRequired() throws Exception {
        RequestBody body = processor.processRequestBody(method("withOptionalBody", String.class)).orElseThrow();
        assertFalse(body.getRequired());
    }

    // ==========================================================================
    // Media type
    // ==========================================================================

    @Test
    void requestBody_defaultMediaType_isApplicationJson() throws Exception {
        RequestBody body = processor.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertNotNull(body.getContent());
        assertTrue(body.getContent().containsKey("application/json"),
                "Default media type must be application/json");
    }

    @Test
    void requestBody_consumesXml_mediaTypeIsApplicationXml() throws Exception {
        RequestBody body = processor.processRequestBody(method("withXmlBody", String.class)).orElseThrow();
        assertNotNull(body.getContent());
        assertTrue(body.getContent().containsKey("application/xml"),
                "Media type must match the @PostMapping consumes attribute");
    }

    // ==========================================================================
    // Schema delegation
    // ==========================================================================

    @Test
    void requestBody_hasSchemaInContent() throws Exception {
        RequestBody body = processor.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertNotNull(body.getContent().get("application/json").getSchema(),
                "Request body content must carry a schema");
        verify(schemaProcessor).toSchema(eq(String.class), any());
    }

    // ==========================================================================
    // Body not first parameter
    // ==========================================================================

    @Test
    void requestBody_asSecondParam_stillDetected() throws Exception {
        Optional<RequestBody> result = processor.processRequestBody(
                method("bodyAsSecondParam", String.class, String.class));
        assertTrue(result.isPresent(), "@RequestBody as second param must still be detected");
    }

    // ==========================================================================
    // Swagger @RequestBody with @Schema(implementation=...) — firstAnnotation path
    // ==========================================================================

    @Test
    void swaggerRequestBodyOverride_usesImplementationClassSchema() throws Exception {
        // @io.swagger.v3.oas.annotations.parameters.RequestBody triggers extractImplementationClass
        // which calls firstAnnotation(schemaAnnotation) → covers the Annotation branch
        Optional<RequestBody> result = processor.processRequestBody(
                method("withSwaggerRequestBodyOverride", String.class));
        assertTrue(result.isPresent(), "RequestBody must be detected via Swagger @RequestBody override");
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullSchemaProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new RequestBodyProcessorImpl(null));
    }

    // ==========================================================================
    // Configurable default consumes media type
    // ==========================================================================

    @Test
    void customDefaultConsumesMediaType_usedWhenNoConsumesDeclared() throws Exception {
        RequestBodyProcessorImpl customProc = new RequestBodyProcessorImpl(schemaProcessor, "application/xml");
        RequestBody body = customProc.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertNotNull(body.getContent().get("application/xml"),
                "Custom defaultConsumesMediaType must be used when no consumes attribute is declared");
    }

    @Test
    void customDefaultConsumesMediaType_explicitConsumesOverridesDefault() throws Exception {
        RequestBodyProcessorImpl customProc = new RequestBodyProcessorImpl(schemaProcessor, "text/plain");
        RequestBody body = customProc.processRequestBody(method("withXmlBody", String.class)).orElseThrow();
        assertNotNull(body.getContent().get("application/xml"),
                "Explicit consumes attribute must override the configured default");
    }

    @Test
    void nullDefaultConsumesMediaType_fallsBackToApplicationJson() throws Exception {
        RequestBodyProcessorImpl customProc = new RequestBodyProcessorImpl(schemaProcessor, null);
        RequestBody body = customProc.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertNotNull(body.getContent().get("application/json"),
                "null defaultConsumesMediaType must fall back to 'application/json'");
    }

    @Test
    void blankDefaultConsumesMediaType_fallsBackToApplicationJson() throws Exception {
        RequestBodyProcessorImpl customProc = new RequestBodyProcessorImpl(schemaProcessor, "  ");
        RequestBody body = customProc.processRequestBody(method("withBody", String.class)).orElseThrow();
        assertNotNull(body.getContent().get("application/json"),
                "Blank defaultConsumesMediaType must fall back to 'application/json'");
    }
}
