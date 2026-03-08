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
package io.github.rspereiratech.openapi.generator.core.processor.parameter;

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParameterProcessorTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private ParameterProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenReturn(new Schema<>());
        processor = new ParameterProcessorImpl(schemaProcessor);
    }

    // ==========================================================================
    // Fixture
    // ==========================================================================

    @SuppressWarnings("unused")
    static class Fixtures {
        public void withPathVariable(@PathVariable("userId") Long id) {}
        public void withPathVariableNoName(@PathVariable String name) {}
        public void withRequestParam(@RequestParam("status") String status) {}
        public void withRequiredRequestParam(@RequestParam(required = true) String q) {}
        public void withOptionalRequestParam(@RequestParam(required = false) String opt) {}
        public void withRequestHeader(@RequestHeader("X-Token") String token) {}
        public void withCookieValue(@CookieValue("session") String sess) {}
        public void withRequestBody(@RequestBody String body) {}
        public void withUnannotatedParam(String value) {}
        public void withMultipleParams(
                @PathVariable Long id,
                @RequestParam(required = false) String filter,
                @RequestBody String body) {}
        public void withSwaggerEnrichment(
                @PathVariable
                @io.swagger.v3.oas.annotations.Parameter(description = "The resource ID", example = "42")
                Long id) {}
        public void withRequestParamNameAttr(@RequestParam(name = "page") int pageNumber) {}
        public void withRequestHeaderName(@RequestHeader(name = "X-Api-Key") String apiKey) {}
        public void withCookieValueName(@CookieValue(name = "auth") String authCookie) {}
        public void withHiddenSwaggerParam(
                @RequestParam
                @io.swagger.v3.oas.annotations.Parameter(hidden = true)
                String secret) {}

        public void withPageable(
                @io.swagger.v3.oas.annotations.Parameter(description = "Pagination parameters")
                org.springframework.data.domain.Pageable pageable) {}
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, params);
    }

    // ==========================================================================
    // @PathVariable
    // ==========================================================================

    @Test
    void pathVariable_setsPathLocationAndIsRequired() throws Exception {
        List<Parameter> params = processor.processParameters(method("withPathVariable", Long.class));
        assertEquals(1, params.size());
        assertEquals("path", params.get(0).getIn());
        assertTrue(params.get(0).getRequired(), "@PathVariable must always be required");
    }

    @Test
    void pathVariable_explicitName_usedAsParameterName() throws Exception {
        List<Parameter> params = processor.processParameters(method("withPathVariable", Long.class));
        assertEquals("userId", params.get(0).getName());
    }

    @Test
    void pathVariable_noAnnotationName_fallsBackToJavaParamName() throws Exception {
        List<Parameter> params = processor.processParameters(method("withPathVariableNoName", String.class));
        assertEquals("name", params.get(0).getName());
    }

    // ==========================================================================
    // @RequestParam
    // ==========================================================================

    @Test
    void requestParam_setsQueryLocation() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequestParam", String.class));
        assertEquals(1, params.size());
        assertEquals("query", params.get(0).getIn());
        assertEquals("status", params.get(0).getName());
    }

    @Test
    void requestParam_requiredTrue_setsRequired() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequiredRequestParam", String.class));
        assertTrue(params.get(0).getRequired());
    }

    @Test
    void requestParam_requiredFalse_notRequired() throws Exception {
        List<Parameter> params = processor.processParameters(method("withOptionalRequestParam", String.class));
        assertFalse(params.get(0).getRequired());
    }

    // ==========================================================================
    // @RequestHeader / @CookieValue
    // ==========================================================================

    @Test
    void requestHeader_setsHeaderLocation() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequestHeader", String.class));
        assertEquals(1, params.size());
        assertEquals("header", params.get(0).getIn());
    }

    @Test
    void cookieValue_setsCookieLocation() throws Exception {
        List<Parameter> params = processor.processParameters(method("withCookieValue", String.class));
        assertEquals(1, params.size());
        assertEquals("cookie", params.get(0).getIn());
    }

    // ==========================================================================
    // Exclusions
    // ==========================================================================

    @Test
    void requestBody_isExcludedFromParameterList() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequestBody", String.class));
        assertTrue(params.isEmpty(), "@RequestBody must not appear in the parameters list");
    }

    @Test
    void unannotatedParam_isExcluded() throws Exception {
        List<Parameter> params = processor.processParameters(method("withUnannotatedParam", String.class));
        assertTrue(params.isEmpty(), "Unannotated parameters must be excluded");
    }

    // ==========================================================================
    // Multiple parameters
    // ==========================================================================

    @Test
    void multipleParams_requestBodyExcluded_returnsTwoParams() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withMultipleParams", Long.class, String.class, String.class));
        assertEquals(2, params.size(), "Only @PathVariable and @RequestParam should be included");
        assertTrue(params.stream().anyMatch(p -> "path".equals(p.getIn())));
        assertTrue(params.stream().anyMatch(p -> "query".equals(p.getIn())));
    }

    // ==========================================================================
    // Swagger @Parameter enrichment
    // ==========================================================================

    @Test
    void swaggerParameter_setsDescription() throws Exception {
        List<Parameter> params = processor.processParameters(method("withSwaggerEnrichment", Long.class));
        assertEquals("The resource ID", params.get(0).getDescription());
    }

    @Test
    void swaggerParameter_setsExample() throws Exception {
        List<Parameter> params = processor.processParameters(method("withSwaggerEnrichment", Long.class));
        assertEquals("42", params.get(0).getExample());
    }

    // ==========================================================================
    // Name resolution via name() attribute
    // ==========================================================================

    @Test
    void requestParam_nameAttribute_usedAsParameterName() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequestParamNameAttr", int.class));
        assertEquals("page", params.get(0).getName(),
                "@RequestParam(name=) attribute must be used as parameter name");
    }

    @Test
    void requestHeader_nameAttribute_usedAsParameterName() throws Exception {
        List<Parameter> params = processor.processParameters(method("withRequestHeaderName", String.class));
        assertEquals("X-Api-Key", params.get(0).getName());
    }

    @Test
    void cookieValue_nameAttribute_usedAsParameterName() throws Exception {
        List<Parameter> params = processor.processParameters(method("withCookieValueName", String.class));
        assertEquals("auth", params.get(0).getName());
    }

    // ==========================================================================
    // Swagger @Parameter hidden flag
    // ==========================================================================

    @Test
    void swaggerParameter_hidden_addsXHiddenExtension() throws Exception {
        List<Parameter> params = processor.processParameters(method("withHiddenSwaggerParam", String.class));
        assertEquals(1, params.size());
        assertNotNull(params.get(0).getExtensions(), "Hidden param must have extensions");
        assertEquals(true, params.get(0).getExtensions().get("x-hidden"));
    }

    // ==========================================================================
    // Schema delegation
    // ==========================================================================

    @Test
    void parameter_hasSchemaPopulated() throws Exception {
        List<Parameter> params = processor.processParameters(method("withPathVariable", Long.class));
        assertNotNull(params.get(0).getSchema(), "Every parameter must carry a schema");
        verify(schemaProcessor).toSchema(Long.class);
    }

    // ==========================================================================
    // Pageable parameter → single $ref query parameter
    // ==========================================================================

    @Test
    void pageable_parameter_producesRefToPageableSchema() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withPageable", org.springframework.data.domain.Pageable.class));
        assertEquals(1, params.size(), "Pageable must produce exactly one parameter");
        Parameter p = params.get(0);
        assertAll(
                () -> assertEquals("query", p.getIn()),
                () -> assertTrue(p.getRequired(), "Pageable parameter must be required"),
                () -> assertNotNull(p.getSchema()),
                () -> assertNotNull(p.getSchema().get$ref(), "$ref must point to Pageable schema"),
                () -> assertTrue(p.getSchema().get$ref().contains("Pageable"))
        );
    }

    @Test
    void pageable_parameter_descriptionFromSwaggerAnnotation() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withPageable", org.springframework.data.domain.Pageable.class));
        assertEquals("Pagination parameters", params.get(0).getDescription());
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullSchemaProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ParameterProcessorImpl(null));
    }
}
