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
package io.github.rspereiratech.openapi.generator.core.processor.operation;

import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessorImpl.methodNameToSentence;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationProcessorTest {

    @Mock
    ParameterProcessor parameterProcessor;

    @Mock
    RequestBodyProcessor requestBodyProcessor;

    @Mock
    ResponseProcessor responseProcessor;

    private OperationProcessor processor;

    @BeforeEach
    void setUp() {
        ApiResponses defaultResponses = new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("OK"))
                .addApiResponse("default", new ApiResponse().description("Unexpected error"));

        lenient().when(parameterProcessor.processParameters(any(), any(), any())).thenReturn(List.of());
        lenient().when(requestBodyProcessor.processRequestBody(any(), any())).thenReturn(Optional.empty());
        lenient().when(responseProcessor.processResponses(any(), anyString(), any())).thenReturn(defaultResponses);

        processor = new OperationProcessorImpl(parameterProcessor, requestBodyProcessor, responseProcessor);
    }

    // ==========================================================================
    // Fixture
    // ==========================================================================

    @SuppressWarnings("unused")
    static class Fixtures {

        @GetMapping
        public String getUserById(@PathVariable Long id) { return ""; }

        @PostMapping
        public String createOrder(@org.springframework.web.bind.annotation.RequestBody String body) { return ""; }

        @GetMapping
        public String filterItems(@RequestParam(required = false) String q) { return ""; }

        @GetMapping
        @Operation(summary = "Custom summary", description = "Custom description", operationId = "customOpId")
        public String withSwaggerOperation() { return ""; }

        @GetMapping
        @Operation(hidden = true)
        public String hiddenOperation() { return ""; }

        @GetMapping
        @Deprecated
        public String deprecatedMethod() { return ""; }

        @GetMapping
        @Operation(tags = {"extra-tag", "another-tag"})
        public String operationWithExtraTags() { return ""; }
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, params);
    }

    // ==========================================================================
    // methodNameToSentence (static helper)
    // ==========================================================================

    @Test
    void methodNameToSentence_camelCase_convertsToSpacedSentence() {
        assertEquals("Get user by id", methodNameToSentence("getUserById"));
    }

    @Test
    void methodNameToSentence_singleWord_lowercased() {
        assertEquals("Create", methodNameToSentence("create"));
    }

    @Test
    void methodNameToSentence_allCaps_spacedAndLowered() {
        assertEquals("Delete product", methodNameToSentence("deleteProduct"));
    }

    @Test
    void methodNameToSentence_null_returnsNull() {
        assertNull(methodNameToSentence(null));
    }

    @Test
    void methodNameToSentence_blank_returnsBlank() {
        assertEquals("   ", methodNameToSentence("   "));
    }

    @Test
    void methodNameToSentence_multipleUpperCase_allSpaced() {
        assertEquals("Find all by name", methodNameToSentence("findAllByName"));
    }

    // ==========================================================================
    // operationId and summary defaults
    // ==========================================================================

    @Test
    void buildOperation_noSwaggerAnnotation_operationIdIsMethodName() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());
        assertEquals("getUserById", op.getOperationId());
    }

    @Test
    void buildOperation_noSwaggerAnnotation_summaryIsDerivedFromMethodName() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());
        assertEquals("Get user by id", op.getSummary());
    }

    // ==========================================================================
    // Tags
    // ==========================================================================

    @Test
    void buildOperation_tagsPassedThroughToOperation() throws Exception {
        List<String> tags = List.of("users", "admin");
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", tags);
        assertTrue(op.getTags().containsAll(tags));
    }

    @Test
    void buildOperation_noTags_operationHasNoTags() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());
        assertTrue(op.getTags() == null || op.getTags().isEmpty());
    }

    // ==========================================================================
    // Parameters — delegation to ParameterProcessor
    // ==========================================================================

    @Test
    void buildOperation_methodWithPathVariable_parameterPresent() throws Exception {
        Parameter pathParam = new Parameter().name("id").in("path");
        when(parameterProcessor.processParameters(any(), any(), any())).thenReturn(List.of(pathParam));

        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());

        assertNotNull(op.getParameters());
        assertEquals(1, op.getParameters().size());
        assertEquals("path", op.getParameters().get(0).getIn());
        verify(parameterProcessor).processParameters(eq(method("getUserById", Long.class)), any(), any());
    }

    @Test
    void buildOperation_methodWithRequestParam_parameterPresent() throws Exception {
        Parameter queryParam = new Parameter().name("q").in("query");
        when(parameterProcessor.processParameters(any(), any(), any())).thenReturn(List.of(queryParam));

        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("filterItems", String.class), "GET", List.of());

        assertNotNull(op.getParameters());
        List<Parameter> params = op.getParameters();
        assertTrue(params.stream().anyMatch(p -> "query".equals(p.getIn())));
        verify(parameterProcessor).processParameters(eq(method("filterItems", String.class)), any(), any());
    }

    @Test
    void buildOperation_methodWithNoParams_parametersAbsent() throws Exception {
        // Default stub returns List.of() → no parameters set on the operation
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("withSwaggerOperation"), "GET", List.of());
        assertTrue(op.getParameters() == null || op.getParameters().isEmpty());
    }

    // ==========================================================================
    // Request body — delegation to RequestBodyProcessor
    // ==========================================================================

    @Test
    void buildOperation_methodWithRequestBody_requestBodyPresent() throws Exception {
        when(requestBodyProcessor.processRequestBody(any(), any()))
                .thenReturn(Optional.of(new io.swagger.v3.oas.models.parameters.RequestBody()));

        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("createOrder", String.class), "POST", List.of());

        assertNotNull(op.getRequestBody(), "POST method with @RequestBody must produce a requestBody");
        verify(requestBodyProcessor).processRequestBody(eq(method("createOrder", String.class)), any());
    }

    @Test
    void buildOperation_methodWithoutRequestBody_requestBodyAbsent() throws Exception {
        // Default stub returns Optional.empty() → no requestBody set
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());
        assertNull(op.getRequestBody(), "GET without @RequestBody must not produce a requestBody");
    }

    // ==========================================================================
    // Responses — delegation to ResponseProcessor
    // ==========================================================================

    @Test
    void buildOperation_responsesAlwaysPresent() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());

        assertNotNull(op.getResponses());
        assertFalse(op.getResponses().isEmpty());
        verify(responseProcessor).processResponses(eq(method("getUserById", Long.class)), eq("GET"), any());
    }

    // ==========================================================================
    // @Deprecated
    // ==========================================================================

    @Test
    void buildOperation_deprecatedMethod_setsDeprecatedFlag() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("deprecatedMethod"), "GET", List.of());
        assertTrue(Boolean.TRUE.equals(op.getDeprecated()),
                "@Deprecated must set operation.deprecated = true");
    }

    @Test
    void buildOperation_nonDeprecatedMethod_deprecatedFlagAbsent() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("getUserById", Long.class), "GET", List.of());
        assertTrue(op.getDeprecated() == null || !op.getDeprecated(),
                "Non-deprecated method must not set deprecated flag");
    }

    // ==========================================================================
    // Swagger @Operation enrichment
    // ==========================================================================

    @Test
    void swaggerOperation_summaryOverridesDefault() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("withSwaggerOperation"), "GET", List.of());
        assertEquals("Custom summary", op.getSummary());
    }

    @Test
    void swaggerOperation_descriptionSet() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("withSwaggerOperation"), "GET", List.of());
        assertEquals("Custom description", op.getDescription());
    }

    @Test
    void swaggerOperation_operationIdOverridesDefault() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("withSwaggerOperation"), "GET", List.of());
        assertEquals("customOpId", op.getOperationId());
    }

    @Test
    void swaggerOperation_hidden_addsXHiddenExtension() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("hiddenOperation"), "GET", List.of());
        assertNotNull(op.getExtensions(), "Hidden operation must have extensions");
        assertEquals(true, op.getExtensions().get("x-hidden"));
    }

    @Test
    void swaggerOperation_extraTagsAddedToOperation() throws Exception {
        io.swagger.v3.oas.models.Operation op =
                processor.buildOperation(method("operationWithExtraTags"), "GET", List.of());
        assertNotNull(op.getTags());
        assertTrue(op.getTags().contains("extra-tag"));
        assertTrue(op.getTags().contains("another-tag"));
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullParameterProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new OperationProcessorImpl(null, requestBodyProcessor, responseProcessor));
    }

    @Test
    void constructor_nullRequestBodyProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new OperationProcessorImpl(parameterProcessor, null, responseProcessor));
    }

    @Test
    void constructor_nullResponseProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new OperationProcessorImpl(parameterProcessor, requestBodyProcessor, null));
    }
}
