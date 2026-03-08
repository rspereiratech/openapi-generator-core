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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void get_voidReturn_resolves204() throws Exception {
        Method m = method("voidReturn");
        assertEquals("204", resolver.resolveCode(m, "GET", m.getGenericReturnType()));
    }

    @Test
    void delete_voidReturn_resolves204() throws Exception {
        Method m = method("voidReturn");
        assertEquals("204", resolver.resolveCode(m, "DELETE", m.getGenericReturnType()));
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

    // Common codes previously covered by the static map
    @Test void describeCode_200_returnsOk()                   { assertEquals("OK",                     resolver.describeCode("200")); }
    @Test void describeCode_201_returnsCreated()               { assertEquals("Created",                resolver.describeCode("201")); }
    @Test void describeCode_204_returnsNoContent()             { assertEquals("No Content",             resolver.describeCode("204")); }
    @Test void describeCode_400_returnsBadRequest()            { assertEquals("Bad Request",            resolver.describeCode("400")); }
    @Test void describeCode_401_returnsUnauthorized()          { assertEquals("Unauthorized",           resolver.describeCode("401")); }
    @Test void describeCode_403_returnsForbidden()             { assertEquals("Forbidden",              resolver.describeCode("403")); }
    @Test void describeCode_404_returnsNotFound()              { assertEquals("Not Found",              resolver.describeCode("404")); }
    @Test void describeCode_500_returnsInternalServerError()   { assertEquals("Internal Server Error",  resolver.describeCode("500")); }

    // Codes that the old static map did NOT cover — now resolved via HttpStatus
    @Test void describeCode_301_returnsMovedPermanently()      { assertEquals("Moved Permanently",      resolver.describeCode("301")); }
    @Test void describeCode_302_returnsFound()                 { assertEquals("Found",                  resolver.describeCode("302")); }
    @Test void describeCode_304_returnsNotModified()           { assertEquals("Not Modified",           resolver.describeCode("304")); }
    @Test void describeCode_409_returnsConflict()              { assertEquals("Conflict",               resolver.describeCode("409")); }
    @Test void describeCode_422_returnsUnprocessableEntity()   { assertEquals("Unprocessable Entity",   resolver.describeCode("422")); }
    @Test void describeCode_429_returnsTooManyRequests()       { assertEquals("Too Many Requests",      resolver.describeCode("429")); }
    @Test void describeCode_503_returnsServiceUnavailable()    { assertEquals("Service Unavailable",    resolver.describeCode("503")); }

    // Codes unknown to Spring's HttpStatus fall back to "Response"
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
