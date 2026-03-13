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
import io.swagger.v3.oas.models.media.ArraySchema;
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

/**
 * Unit tests for {@link ResponseProcessorImpl}.
 *
 * <p>Covers default status-code inference (GET→200, POST→201), {@code @ResponseStatus}
 * override, explicit {@code @ApiResponse} / {@code @ApiResponses} handling, schema resolution,
 * media-type derivation from {@code produces} attributes, composed schema variants
 * (oneOf/allOf/anyOf/{@code @ArraySchema}), configurable default produces media type,
 * and the {@code "default"} response-code sentinel.
 */
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
        processor = new ResponseProcessorImpl(schemaProcessor, "*/*");
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

        @PutMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content)
        })
        public void putWithExplicitResponses() {}

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

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                schema = @io.swagger.v3.oas.annotations.media.Schema(
                                        implementation = String.class))))
        public void apiResponseWithArraySchema() {}

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                oneOf = {String.class, Integer.class})))
        public String apiResponseWithOneOf() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                allOf = {String.class})))
        public String apiResponseWithAllOf() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                anyOf = {String.class, Integer.class})))
        public String apiResponseWithAnyOf() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                type = "string")))
        public String apiResponseWithSchemaType() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                oneOf = {String.class})))
        public String apiResponseWithOneOfAndMediaType() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.Operation(responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        description = "success",
                        content = @io.swagger.v3.oas.annotations.media.Content(
                                schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {String.class})))
        })
        public String apiResponseNoResponseCode() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default", description = "ok")
        public String apiResponseExplicitDefaultCode() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse
        public String apiResponseBlankDescription() { return ""; }

        // Rule 2 / Rule 3 fixtures

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
        public String apiResponse_2xx_noContentAttr() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
        public String apiResponse_4xx_noContentAttr() { return ""; }

        @GetMapping
        @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Bad Request",
                    content = @io.swagger.v3.oas.annotations.media.Content)
        })
        public String apiResponse_2xx_emptyContent_and_4xx_emptyContent() { return ""; }
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
    void delete_voidReturn_produces200() throws Exception {
        ApiResponses responses = processor.processResponses(method("deleteReturnsVoid"), "DELETE");
        assertTrue(responses.containsKey("200"), "Void return type without @ResponseStatus should produce 200");
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
        assertNull(responses.get("200").getContent(),
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
    void put_withExplicitApiResponses_usesAnnotationDescriptions() throws Exception {
        ApiResponses responses = processor.processResponses(method("putWithExplicitResponses"), "PUT");
        assertAll(
                () -> assertTrue(responses.containsKey("200"), "200 must be present"),
                () -> assertEquals("Updated successfully", responses.get("200").getDescription()),
                () -> assertTrue(responses.containsKey("404"), "404 must be present"),
                () -> assertEquals("Not Found", responses.get("404").getDescription())
        );
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
    // @ApiResponse with @ArraySchema
    // ==========================================================================

    @Test
    void apiResponseWithArraySchema_responseCodePresent() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseWithArraySchema"), "GET");
        assertNotNull(responses.get("200"),
                "Response 200 must be registered when @ArraySchema is used in @Content");
    }

    @Test
    void apiResponseWithArraySchema_schemaIsArrayType() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseWithArraySchema"), "GET");
        assertNotNull(responses.get("200").getContent(), "Content must not be null");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertTrue(schema instanceof ArraySchema,
                "Schema must be an ArraySchema when @Content(array=@ArraySchema(...)) is declared");
    }

    @Test
    void apiResponseWithArraySchema_itemSchemaIsResolved() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponseWithArraySchema"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertNotNull(((ArraySchema) schema).getItems(),
                "ArraySchema items must be resolved from @ArraySchema.schema.implementation");
    }

    // ==========================================================================
    // @Schema(oneOf/allOf/anyOf/type) — composed schema resolution
    // ==========================================================================

    @Test
    void apiResponseWithOneOf_schemaHasOneOfEntries() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithOneOf"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertNotNull(schema.getOneOf(),
                "oneOf declared in @Schema must produce a schema with oneOf entries");
        assertEquals(2, schema.getOneOf().size(),
                "Both classes declared in oneOf must be resolved");
    }

    @Test
    void apiResponseWithOneOf_noAllOfNoAnyOf() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithOneOf"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertNull(schema.getAllOf(), "allOf must be null when only oneOf is declared");
        assertNull(schema.getAnyOf(), "anyOf must be null when only oneOf is declared");
    }

    @Test
    void apiResponseWithAllOf_schemaHasAllOfEntries() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithAllOf"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertNotNull(schema.getAllOf(),
                "allOf declared in @Schema must produce a schema with allOf entries");
        assertEquals(1, schema.getAllOf().size());
    }

    @Test
    void apiResponseWithAnyOf_schemaHasAnyOfEntries() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithAnyOf"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertNotNull(schema.getAnyOf(),
                "anyOf declared in @Schema must produce a schema with anyOf entries");
        assertEquals(2, schema.getAnyOf().size());
    }

    @Test
    void apiResponseWithSchemaType_schemaHasType() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithSchemaType"), "GET");
        Schema<?> schema = responses.get("200").getContent().get("*/*").getSchema();
        assertEquals("string", schema.getType(),
                "type declared in @Schema must be set on the resolved schema");
    }

    @Test
    void apiResponseWithOneOfAndMediaType_usesExplicitMediaType() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseWithOneOfAndMediaType"), "GET");
        assertNotNull(responses.get("200").getContent().get("application/json"),
                "Explicit mediaType on @Content must be used even when schema uses oneOf");
    }

    // ==========================================================================
    // @ApiResponse with blank / "default" responseCode → "default" key
    // ==========================================================================

    @Test
    void apiResponseNoResponseCode_usesDefaultKey() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseNoResponseCode"), "GET");
        assertTrue(responses.containsKey("default"),
                "Blank responseCode must produce 'default' key, not '200'");
        assertFalse(responses.containsKey("200"),
                "'200' must not appear when responseCode is blank");
    }

    @Test
    void apiResponseExplicitDefaultCode_usesDefaultKey() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseExplicitDefaultCode"), "GET");
        assertTrue(responses.containsKey("default"),
                "Explicit responseCode='default' must produce 'default' key");
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullSchemaProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ResponseProcessorImpl(null, "*/*"));
    }

    @Test
    void constructor_nullStatusResolver_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ResponseProcessorImpl(schemaProcessor, "*/*", null));
    }

    // ==========================================================================
    // Configurable default produces media type
    // ==========================================================================

    @Test
    void customDefaultProducesMediaType_usedWhenNoProducesDeclared() throws Exception {
        ResponseProcessorImpl customProc = new ResponseProcessorImpl(schemaProcessor, "application/json");
        ApiResponses responses = customProc.processResponses(method("getReturnsString"), "GET");
        assertNotNull(responses.get("200").getContent().get("application/json"),
                "Custom defaultProducesMediaType must be used when no produces attribute is declared");
    }

    @Test
    void customDefaultProducesMediaType_explicitProducesOverridesDefault() throws Exception {
        ResponseProcessorImpl customProc = new ResponseProcessorImpl(schemaProcessor, "application/json");
        ApiResponses responses = customProc.processResponses(method("producesXml"), "GET");
        assertNotNull(responses.get("200").getContent().get("application/xml"),
                "Explicit produces attribute must override the configured default");
    }

    @Test
    void nullDefaultProducesMediaType_fallsBackToWildcard() throws Exception {
        ResponseProcessorImpl customProc = new ResponseProcessorImpl(schemaProcessor, null);
        ApiResponses responses = customProc.processResponses(method("getReturnsString"), "GET");
        assertNotNull(responses.get("200").getContent().get("*/*"),
                "null defaultProducesMediaType must fall back to '*/*'");
    }

    @Test
    void blankDefaultProducesMediaType_fallsBackToWildcard() throws Exception {
        ResponseProcessorImpl customProc = new ResponseProcessorImpl(schemaProcessor, "  ");
        ApiResponses responses = customProc.processResponses(method("getReturnsString"), "GET");
        assertNotNull(responses.get("200").getContent().get("*/*"),
                "Blank defaultProducesMediaType must fall back to '*/*'");
    }

    // ==========================================================================
    // Rule 2 — 2xx without content → infer schema from return type
    // ==========================================================================

    @Test
    void rule2_2xx_noContentAttr_inferredFromReturnType() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponse_2xx_noContentAttr"), "GET");
        assertNotNull(responses.get("200").getContent(),
                "Rule 2: 2xx @ApiResponse without content must infer schema from return type");
    }

    @Test
    void rule2_2xx_emptyContent_inferredFromReturnType() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponse_2xx_emptyContent_and_4xx_emptyContent"), "GET");
        assertNotNull(responses.get("200").getContent(),
                "Rule 2: 2xx @ApiResponse with empty @Content must infer schema from return type");
    }

    // ==========================================================================
    // Rule 3 — 4xx/5xx without content → no body
    // ==========================================================================

    @Test
    void rule3_4xx_noContentAttr_hasNoBody() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponse_4xx_noContentAttr"), "GET");
        assertNull(responses.get("404").getContent(),
                "Rule 3: 4xx @ApiResponse without content must produce no response body");
    }

    @Test
    void rule3_4xx_emptyContent_hasNoBody() throws Exception {
        ApiResponses responses = processor.processResponses(
                method("apiResponse_2xx_emptyContent_and_4xx_emptyContent"), "GET");
        assertNull(responses.get("400").getContent(),
                "Rule 3: 4xx @ApiResponse with empty @Content must produce no response body");
    }

    @Test
    void rule3_4xx_emptyContent_doesNotBleedReturnType() throws Exception {
        // Existing fixture: putWithExplicitResponses has 404 with content = @Content on a void method
        ApiResponses responses = processor.processResponses(method("putWithExplicitResponses"), "PUT");
        assertNull(responses.get("404").getContent(),
                "Rule 3: 4xx with empty @Content on a void method must still produce no body");
    }

    // ==========================================================================
    // Rule 4 — no @ApiResponse → status from resolver, schema from return type
    // ==========================================================================

    @Test
    void rule4_noApiResponse_nonVoid_produces200WithContent() throws Exception {
        ApiResponses responses = processor.processResponses(method("getReturnsString"), "GET");
        assertNotNull(responses.get("200"),       "Rule 4: no @ApiResponse + non-void → 200");
        assertNotNull(responses.get("200").getContent(), "Rule 4: content must be inferred from return type");
    }

    @Test
    void rule4_noApiResponse_void_produces200WithNoContent() throws Exception {
        ApiResponses responses = processor.processResponses(method("deleteReturnsVoid"), "DELETE");
        assertNotNull(responses.get("200"),       "Rule 4: no @ApiResponse + void → 200");
        assertNull(responses.get("200").getContent(), "Rule 4: void return must produce no body");
    }

    // ==========================================================================
    // description fallback for blank @ApiResponse.description
    // ==========================================================================

    @Test
    void apiResponseBlankDescription_defaultCode_fallsBackToDefaultResponse() throws Exception {
        ApiResponses responses = processor.processResponses(method("apiResponseBlankDescription"), "GET");
        assertEquals("default response", responses.get("default").getDescription(),
                "Blank description on a 'default' response must fall back to 'default response'");
    }
}
