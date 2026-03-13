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

import java.util.Locale;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ParameterProcessorImpl}.
 *
 * <p>Covers parameter location mapping ({@code path}, {@code query}, {@code header},
 * {@code cookie}), name resolution from annotation attributes, the {@code required} flag,
 * exclusion of {@code @RequestBody} and unannotated parameters, Swagger
 * {@code @Parameter} enrichment and hidden-flag filtering, {@code Pageable} expansion,
 * default and custom ignored-type lists, schema-override behaviour for otherwise-ignored
 * types annotated with {@code @Parameter(schema=@Schema(...))}, and method-level virtual
 * parameters declared via {@code @Parameter} / {@code @Parameters} on the method itself.
 */
@ExtendWith(MockitoExtension.class)
class ParameterProcessorTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private ParameterProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenReturn(new Schema<>());
        processor = new ParameterProcessorImpl(schemaProcessor, true, Set.of());
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

        public void withHiddenPageable(
                @io.swagger.v3.oas.annotations.Parameter(hidden = true)
                org.springframework.data.domain.Pageable pageable) {}

        public void withLocale(@RequestParam Locale locale) {}
        public void withLocaleAndOther(@RequestParam Locale locale, @RequestParam String status) {}
        public void withCustomIgnoredType(@RequestParam java.util.Currency currency) {}
        public void withLocaleSchemaOverride(
                @RequestParam(value = "locale", required = false)
                @io.swagger.v3.oas.annotations.Parameter(
                        description = "Locale for filtering (e.g., en_US, pt_BR)",
                        schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", example = "en_US"))
                Locale locale) {}

        // ------------------------------------------------------------------
        // Virtual / method-level @Parameter fixtures
        // ------------------------------------------------------------------

        @io.swagger.v3.oas.annotations.Parameter(name = "page", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Integer.class), description = "Page number (0..N)")
        @io.swagger.v3.oas.annotations.Parameter(name = "size", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Integer.class), description = "Number of records per page")
        @io.swagger.v3.oas.annotations.Parameter(name = "sort", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class),  description = "Sort criteria", example = "id,asc")
        public java.util.List<String> withMethodLevelParams(
                @io.swagger.v3.oas.annotations.Parameter(hidden = true) org.springframework.data.domain.Pageable pageable) { return null; }

        @io.swagger.v3.oas.annotations.Parameters({
                @io.swagger.v3.oas.annotations.Parameter(name = "filter", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,  schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class)),
                @io.swagger.v3.oas.annotations.Parameter(name = "lang",   in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class))
        })
        public java.util.List<String> withParametersContainer() { return null; }

        @io.swagger.v3.oas.annotations.Parameter(name = "hidden-virtual", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, hidden = true)
        public void withMethodLevelHiddenParam() {}

        @io.swagger.v3.oas.annotations.Parameter(in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, description = "No name")
        public void withNamelessMethodLevelParam() {}

        @io.swagger.v3.oas.annotations.Parameter(name = "status", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, description = "Virtual description")
        public void withConcreteAndVirtualSameName(@RequestParam("status") String status) {}

        @io.swagger.v3.oas.annotations.Parameter(name = "x-locale", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER, required = true, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class), description = "Locale header")
        public void withMethodLevelHeaderParam() {}

        @io.swagger.v3.oas.annotations.Parameter(name = "deprecated-param", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, deprecated = true, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class))
        public void withDeprecatedVirtualParam() {}
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
    void swaggerParameter_hidden_isExcluded() throws Exception {
        List<Parameter> params = processor.processParameters(method("withHiddenSwaggerParam", String.class));
        assertTrue(params.isEmpty(), "Parameters with @Parameter(hidden = true) must be excluded");
    }

    @Test
    void hiddenPageable_isExcluded() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withHiddenPageable", org.springframework.data.domain.Pageable.class));
        assertTrue(params.isEmpty(), "Pageable with @Parameter(hidden = true) must be excluded");
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
                () -> new ParameterProcessorImpl(null, true, Set.of()));
    }

    // ==========================================================================
    // Ignored param types — default list
    // ==========================================================================

    @Test
    void locale_isIgnoredByDefault() throws Exception {
        List<Parameter> params = processor.processParameters(method("withLocale", Locale.class));
        assertTrue(params.isEmpty(), "Locale must be ignored by the default ignore list");
    }

    @Test
    void locale_isIncluded_whenDefaultsDisabled() throws Exception {
        ParameterProcessor noDefaults = new ParameterProcessorImpl(schemaProcessor, false, Set.of());
        List<Parameter> params = noDefaults.processParameters(method("withLocale", Locale.class));
        assertEquals(1, params.size(), "Locale must appear when default ignore list is disabled");
    }

    @Test
    void locale_ignored_otherParamStillIncluded() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withLocaleAndOther", Locale.class, String.class));
        assertEquals(1, params.size(), "Only the non-ignored param should be present");
        assertEquals("status", params.get(0).getName());
    }

    // ==========================================================================
    // Ignored param types — additional list
    // ==========================================================================

    @Test
    void additionalIgnoredType_isSkipped() throws Exception {
        ParameterProcessor withExtra = new ParameterProcessorImpl(
                schemaProcessor, true, Set.of("java.util.Currency"));
        List<Parameter> params = withExtra.processParameters(
                method("withCustomIgnoredType", java.util.Currency.class));
        assertTrue(params.isEmpty(), "Custom additional ignored type must be skipped");
    }

    @Test
    void additionalIgnoredType_doesNotAffectOtherProcessor() throws Exception {
        // The default processor does not ignore Currency
        List<Parameter> params = processor.processParameters(
                method("withCustomIgnoredType", java.util.Currency.class));
        assertEquals(1, params.size(), "Currency should not be ignored by the default processor");
    }

    @Test
    void constructor_nullAdditionalIgnoredTypes_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ParameterProcessorImpl(schemaProcessor, true, null));
    }

    // ==========================================================================
    // Ignored param type override via @Parameter(schema=...)
    // ==========================================================================

    @Test
    void locale_withExplicitSchemaType_isIncludedAsString() throws Exception {
        List<Parameter> params = processor.processParameters(method("withLocaleSchemaOverride", Locale.class));
        assertEquals(1, params.size(), "Locale with explicit @Parameter(schema=@Schema(type=\"string\")) must not be ignored");
        Parameter p = params.get(0);
        assertAll(
                () -> assertEquals("locale", p.getName()),
                () -> assertEquals("query", p.getIn()),
                () -> assertNotNull(p.getSchema()),
                () -> assertEquals("string", p.getSchema().getType()),
                () -> assertEquals("en_US", p.getSchema().getExample()),
                () -> assertEquals("Locale for filtering (e.g., en_US, pt_BR)", p.getDescription())
        );
    }

    // ==========================================================================
    // Method-level @Parameter (virtual parameters)
    // ==========================================================================

    @Test
    void methodLevelParams_hiddenPageable_producesThreeVirtualParams() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withMethodLevelParams", org.springframework.data.domain.Pageable.class));
        assertEquals(3, params.size(), "Hidden Pageable + 3 method-level @Parameter should yield 3 virtual params");
        assertTrue(params.stream().anyMatch(p -> "page".equals(p.getName())));
        assertTrue(params.stream().anyMatch(p -> "size".equals(p.getName())));
        assertTrue(params.stream().anyMatch(p -> "sort".equals(p.getName())));
    }

    @Test
    void methodLevelParams_allInQuery() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withMethodLevelParams", org.springframework.data.domain.Pageable.class));
        assertTrue(params.stream().allMatch(p -> "query".equals(p.getIn())));
    }

    @Test
    void methodLevelParams_descriptionAndExampleSet() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withMethodLevelParams", org.springframework.data.domain.Pageable.class));
        Parameter sort = params.stream().filter(p -> "sort".equals(p.getName())).findFirst().orElseThrow();
        assertAll(
                () -> assertEquals("Sort criteria", sort.getDescription()),
                () -> assertEquals("id,asc", sort.getExample())
        );
    }

    @Test
    void parametersContainer_bothParamsProduced() throws Exception {
        List<Parameter> params = processor.processParameters(method("withParametersContainer"));
        assertEquals(2, params.size());
        assertTrue(params.stream().anyMatch(p -> "filter".equals(p.getName()) && "query".equals(p.getIn())));
        assertTrue(params.stream().anyMatch(p -> "lang".equals(p.getName())   && "header".equals(p.getIn())));
    }

    @Test
    void methodLevelHiddenParam_isSkipped() throws Exception {
        List<Parameter> params = processor.processParameters(method("withMethodLevelHiddenParam"));
        assertTrue(params.isEmpty(), "Method-level @Parameter(hidden=true) must be skipped");
    }

    @Test
    void namelessMethodLevelParam_isSkipped() throws Exception {
        List<Parameter> params = processor.processParameters(method("withNamelessMethodLevelParam"));
        assertTrue(params.isEmpty(), "Method-level @Parameter with blank name must be skipped");
    }

    @Test
    void concreteParamWins_whenNameClashesWithVirtual() throws Exception {
        List<Parameter> params = processor.processParameters(
                method("withConcreteAndVirtualSameName", String.class));
        assertEquals(1, params.size(), "Concrete and virtual with same name must produce exactly one param");
        // Concrete wins — description comes from @RequestParam, not from virtual @Parameter
        assertFalse("Virtual description".equals(params.get(0).getDescription()),
                "Concrete @RequestParam must win over method-level virtual @Parameter");
    }

    @Test
    void methodLevelHeaderParam_setsHeaderLocationAndRequired() throws Exception {
        List<Parameter> params = processor.processParameters(method("withMethodLevelHeaderParam"));
        assertEquals(1, params.size());
        Parameter p = params.get(0);
        assertAll(
                () -> assertEquals("x-locale", p.getName()),
                () -> assertEquals("header", p.getIn()),
                () -> assertTrue(p.getRequired()),
                () -> assertEquals("Locale header", p.getDescription())
        );
    }

    @Test
    void deprecatedVirtualParam_setsDeprecatedFlag() throws Exception {
        List<Parameter> params = processor.processParameters(method("withDeprecatedVirtualParam"));
        assertEquals(1, params.size());
        assertTrue(params.get(0).getDeprecated(), "deprecated=true on @Parameter must propagate to OpenAPI");
    }
}
