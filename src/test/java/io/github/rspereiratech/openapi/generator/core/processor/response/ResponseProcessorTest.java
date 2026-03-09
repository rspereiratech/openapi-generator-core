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
package io.github.rspereiratech.openapi.generator.core.processor.response;

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ResponseProcessorTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private ResponseProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenAnswer(inv -> {
            Type type = inv.getArgument(0);
            if (type == void.class || type == Void.class) return null;
            return new Schema<>();
        });
        lenient().when(schemaProcessor.toSchema(any(), any())).thenAnswer(inv -> {
            Type type = inv.getArgument(0);
            if (type == void.class || type == Void.class) return null;
            return new Schema<>();
        });
        processor = new ResponseProcessorImpl(schemaProcessor);
    }

    // ==========================================================================
    // Fixture
    // ==========================================================================

    @SuppressWarnings("unused")
    static class Fixtures {

        @GetMapping
        public String getReturnsString() { return ""; }

        @PostMapping
        public String postReturnsString() { return ""; }

        @DeleteMapping
        public void deleteReturnsVoid() {}

        @GetMapping
        @ResponseStatus(HttpStatus.CREATED)
        public String withResponseStatus() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Accepted")
        public String withExplicitApiResponse() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
        })
        public String withMultipleApiResponses() { return ""; }

        @GetMapping
        public ResponseEntity<String> returnsResponseEntity() { return null; }

        @PutMapping
        public String putReturnsString() { return ""; }

        @PatchMapping
        public String patchReturnsString() { return ""; }

        @DeleteMapping
        public String deleteReturnsNonVoid() { return ""; }

        @GetMapping(produces = "application/xml")
        public String producesXml() { return ""; }

        @GetMapping
        @ResponseStatus(code = HttpStatus.ACCEPTED)
        public String withResponseStatusCode() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/xml"))
        public String apiResponseWithExplicitContent() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                implementation = String.class)))
        public String apiResponseWithSchemaImplementation() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                implementation = Void.class)))
        public String apiResponseWithVoidSchemaImplementation() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.Operation(
                responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @io.swagger.v3.oas.annotations.media.Content(
                                mediaType = "application/json",
                                schema = @io.swagger.v3.oas.annotations.media.Schema(
                                        implementation = String.class))))
        public String apiResponseNestedInOperation() { return ""; }
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, params);
    }

    // ==========================================================================
    // Default status-code inference
    // ==========================================================================

    @Test
    void get_nonVoidReturn_produces200() throws Exception {
        ApiResponses responses = processor.processResponses(method("getReturnsString"), "GET");
        assertTrue(responses.containsKey("200"), "GET with non-void return should produce 200");
    }

    @Test
    void post_nonVoidReturn_produces201() throws Exception {
        ApiResponses responses = processor.processResponses(method("postReturnsString"), "POST");
        assertTrue(responses.containsKey("201"), "POST should produce 201 by default");
    }

    @Test
    void delete_voidReturn_produces204() throws Exception {
        ApiResponses responses = processor.processResponses(method("deleteReturnsVoid"), "DELETE");
        assertTrue(responses.containsKey("204"), "Void return type should produce 204");
    }

    // ==========================================================================
    // @ResponseStatus override
    // ==========================================================================

    @Test
    void responseStatus_annotation_overridesInferredCode() throws Exception {
        ApiResponses responses = processor.processResponses(method("withResponseStatus"), "GET");
        assertTrue(responses.containsKey("201"),
                "@ResponseStatus(CREATED) must override the inferred 200");
        assertFalse(responses.containsKey("200"),
                "Inferred 200 must not appear when @ResponseStatus overrides it");
    }

    // ==========================================================================
    // Explicit @ApiResponse / @ApiResponses
    // ==========================================================================

    @Test
    void explicitApiResponse_usesAnnotatedResponseCode() throws Exception {
        ApiResponses responses = processor.processResponses(method("withExplicitApiResponse"), "GET");
        assertTrue(responses.containsKey("202"), "Explicit @ApiResponse code must take precedence");
    }

    @Test
    void explicitApiResponse_setsDescription() throws Exception {
        ApiResponses responses = processor.processResponses(method("withExplicitApiResponse"), "GET");
        assertEquals("Accepted", responses.get("202").getDescription());
    }

    @Test
    void multipleApiResponses_allCodesPresent() throws Exception {
        ApiResponses responses = processor.processResponses(method("withMultipleApiResponses"), "GET");
        assertAll(
                () -> assertTrue(responses.containsKey("200")),
                () -> assertTrue(responses.containsKey("404"))
        );
    }

    // ==========================================================================
    // Content / schema
    // ==========================================================================

    @Test
    void nonVoidMethod_responseHasContent() throws Exception {
        ApiResponses responses = processor.processResponses(method("getReturnsString"), "GET");
        assertNotNull(responses.get("200").getContent(),
                "Non-void return type must produce a response with content");
    }

    @Test
    void voidMethod_responseHasNoContent() throws Exception {
        ApiResponses responses = processor.processResponses(method("deleteReturnsVoid"), "DELETE");
        assertNull(responses.get("204").getContent(),
                "Void return type must produce a response without content");
    }

    @Test
    void responseEntityReturn_responseHasContent() throws Exception {
        ApiResponses responses = processor.processResponses(method("returnsResponseEntity"), "GET");
        assertNotNull(responses.get("200").getContent(),
                "ResponseEntity<String> return must produce response content");
    }

    // ==========================================================================
    // PUT / PATCH → 200
    // ==========================================================================

    @Test
    void put_nonVoidReturn_produces200() throws Exception {
        ApiResponses responses = processor.processResponses(method("putReturnsString"), "PUT");
        assertTrue(responses.containsKey("200"), "PUT with non-void return should produce 200");
    }

    @Test
    void patch_nonVoidReturn_produces200() throws Exception {
        ApiResponses responses = processor.processResponses(method("patchReturnsString"), "PATCH");
        assertTrue(responses.containsKey("200"), "PATCH with non-void return should produce 200");
    }

    @Test
    void delete_nonVoidReturn_produces200() throws Exception {
        ApiResponses responses = processor.processResponses(method("deleteReturnsNonVoid"), "DELETE");
        assertTrue(responses.containsKey("200"), "DELETE with non-void return type should produce 200");
    }

    // ==========================================================================
    // produces → media type
    // ==========================================================================

    @Test
    void producesXml_responseContentIsApplicationXml() throws Exception {
        ApiResponses responses = processor.processResponses(method("producesXml"), "GET");
        assertNotNull(responses.get("200").getContent());
        assertTrue(responses.get("200").getContent().containsKey("application/xml"),
                "produces attribute must control the response media type");
    }

    // ==========================================================================
    // @ResponseStatus with code attribute
    // ==========================================================================

    @Test
    void responseStatus_codeAttribute_overridesInferred() throws Exception {
        ApiResponses responses = processor.processResponses(method("withResponseStatusCode"), "GET");
        assertTrue(responses.containsKey("202"),
                "@ResponseStatus(code = ACCEPTED) must produce 202");
    }

    // ==========================================================================
    // @ApiResponse with explicit @Content
    // ==========================================================================

    @Test
    void apiResponseWithExplicitContent_responseCodePresent() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithExplicitContent"), "GET");
        assertNotNull(responses.get("200"),
                "Response code 200 must be registered from the @ApiResponse annotation");
    }

    // ==========================================================================
    // @ApiResponse with @Schema(implementation=...) — schemaFromAnnotation path
    // ==========================================================================

    @Test
    void apiResponseWithSchemaImplementation_responseCodePresent() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseWithSchemaImplementation"), "GET");
        assertNotNull(responses.get("200"),
                "Response 200 must be present when @Schema(implementation=...) is declared");
    }

    @Test
    void apiResponseWithVoidSchemaImplementation_fallsBackToReturnType() throws Exception {
        // implementation = Void.class → schemaFromAnnotation returns null → falls back to return type
        ApiResponses responses = processor.processResponses(
                method("apiResponseWithVoidSchemaImplementation"), "GET");
        assertNotNull(responses.get("200"),
                "Response 200 must still be present even when implementation is Void.class");
    }

    // ==========================================================================
    // @ApiResponse nested inside @Operation.responses
    // ==========================================================================

    @Test
    void apiResponseNestedInOperation_responseCodePresent() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseNestedInOperation"), "GET");
        assertNotNull(responses.get("200"),
                "Response 200 must be registered when @ApiResponse is nested inside @Operation.responses");
    }

    @Test
    void apiResponseNestedInOperation_mediaTypeHonoured() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseNestedInOperation"), "GET");
        assertNotNull(responses.get("200").getContent().get("application/json"),
                "Explicit mediaType from @Content inside @Operation.responses must be preserved");
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullSchemaProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ResponseProcessorImpl(null));
    }

    @Test
    void constructor_nullStatusResolver_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ResponseProcessorImpl(schemaProcessor, null));
    }
}
