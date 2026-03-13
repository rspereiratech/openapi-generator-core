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
package io.github.rspereiratech.openapi.generator.core.processor.response.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DefaultHttpStatusResolver}.
 *
 * <p>Verifies {@link DefaultHttpStatusResolver#resolveCode} precondition enforcement,
 * default status-code inference rules (GET/PUT/PATCH/DELETE→200, POST→201),
 * {@code @ResponseStatus} override via both {@code value} and {@code code} attributes,
 * and {@link DefaultHttpStatusResolver#describeCode} delegation to
 * {@link org.springframework.http.HttpStatus#getReasonPhrase()}.
 */
class DefaultHttpStatusResolverTest {

    private HttpStatusResolver resolver;

    @SuppressWarnings("unused")
    static class Fixtures {
        public String plain() { return ""; }
        public void voidReturn() {}

        @ResponseStatus(HttpStatus.CREATED)
        public String withResponseStatusValue() { return ""; }

        @ResponseStatus(code = HttpStatus.ACCEPTED)
        public String withResponseStatusCode() { return ""; }

        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void withResponseStatusNoContent() {}
    }

    @BeforeEach
    void setUp() {
        resolver = new DefaultHttpStatusResolver();
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, params);
    }

    // ==========================================================================
    // resolveCode — preconditions
    // ==========================================================================

    @Test
    void resolveCode_nullMethod_throwsNullPointerException() throws Exception {
        Method m = method("plain");
        assertThrows(NullPointerException.class,
                () -> resolver.resolveCode(null, "GET", m.getGenericReturnType()));
    }

    @Test
    void resolveCode_nullHttpMethod_throwsNullPointerException() throws Exception {
        Method m = method("plain");
        assertThrows(NullPointerException.class,
                () -> resolver.resolveCode(m, null, m.getGenericReturnType()));
    }

    @Test
    void resolveCode_blankHttpMethod_throwsIllegalArgumentException() throws Exception {
        Method m = method("plain");
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveCode(m, "  ", m.getGenericReturnType()));
    }

    @Test
    void resolveCode_nullReturnType_throwsNullPointerException() throws Exception {
        Method m = method("plain");
        assertThrows(NullPointerException.class,
                () -> resolver.resolveCode(m, "GET", null));
    }

    // ==========================================================================
    // resolveCode — default inference
    // ==========================================================================

    @Test
    void get_nonVoidReturn_resolves200() throws Exception {
        Method m = method("plain");
        assertEquals("200", resolver.resolveCode(m, "GET", m.getGenericReturnType()));
    }

    @Test
    void put_nonVoidReturn_resolves200() throws Exception {
        Method m = method("plain");
        assertEquals("200", resolver.resolveCode(m, "PUT", m.getGenericReturnType()));
    }

    @Test
    void patch_nonVoidReturn_resolves200() throws Exception {
        Method m = method("plain");
        assertEquals("200", resolver.resolveCode(m, "PATCH", m.getGenericReturnType()));
    }

    @Test
    void delete_nonVoidReturn_resolves200() throws Exception {
        Method m = method("plain");
        assertEquals("200", resolver.resolveCode(m, "DELETE", m.getGenericReturnType()));
    }

    @Test
    void post_nonVoidReturn_resolves201() throws Exception {
        Method m = method("plain");
        assertEquals("201", resolver.resolveCode(m, "POST", m.getGenericReturnType()));
    }

    @Test
    void get_voidReturn_resolves200() throws Exception {
        Method m = method("voidReturn");
        assertEquals("200", resolver.resolveCode(m, "GET", m.getGenericReturnType()));
    }

    @Test
    void delete_voidReturn_resolves200() throws Exception {
        Method m = method("voidReturn");
        assertEquals("200", resolver.resolveCode(m, "DELETE", m.getGenericReturnType()));
    }

    // ==========================================================================
    // resolveCode — @ResponseStatus overrides
    // ==========================================================================

    @Test
    void responseStatus_valueAttribute_overridesDefault() throws Exception {
        Method m = method("withResponseStatusValue");
        assertEquals("201", resolver.resolveCode(m, "GET", m.getGenericReturnType()),
                "@ResponseStatus(CREATED) must override inferred 200");
    }

    @Test
    void responseStatus_codeAttribute_overridesDefault() throws Exception {
        Method m = method("withResponseStatusCode");
        assertEquals("202", resolver.resolveCode(m, "GET", m.getGenericReturnType()),
                "@ResponseStatus(code = ACCEPTED) must override inferred 200");
    }

    @Test
    void responseStatus_overridesPost201() throws Exception {
        Method m = method("withResponseStatusNoContent");
        assertEquals("204", resolver.resolveCode(m, "POST", m.getGenericReturnType()),
                "@ResponseStatus must win over POST→201 rule");
    }

    // ==========================================================================
    // describeCode — delegates to HttpStatus.getReasonPhrase()
    // ==========================================================================

    @ParameterizedTest(name = "describeCode({0}) = {1}")
    @CsvSource({
            "200, OK",
            "201, Created",
            "204, No Content",
            "301, Moved Permanently",
            "302, Found",
            "304, Not Modified",
            "400, Bad Request",
            "401, Unauthorized",
            "403, Forbidden",
            "404, Not Found",
            "409, Conflict",
            "422, Unprocessable Entity",
            "429, Too Many Requests",
            "500, Internal Server Error",
            "503, Service Unavailable",
            "default, default response"
    })
    void describeCode_knownCodes_returnExpectedPhrase(String code, String expected) {
        assertEquals(expected, resolver.describeCode(code));
    }

    @Test
    void describeCode_unknown_returnsResponse() {
        assertEquals("Response", resolver.describeCode("999"),
                "Unrecognised status code must fall back to 'Response'");
    }

    @Test
    void describeCode_nonNumeric_returnsResponse() {
        assertEquals("Response", resolver.describeCode("not-a-number"),
                "Non-numeric input must fall back to 'Response'");
    }
}
