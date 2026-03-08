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
package io.github.rspereiratech.openapi.generator.core.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SecuritySchemeConfigTest {

    // ==========================================================================
    // of(name, type)
    // ==========================================================================

    @Test
    void of_setsNameAndType() {
        SecuritySchemeConfig config = SecuritySchemeConfig.of("bearerAuth", "http");

        Assertions.assertAll(
                () -> Assertions.assertEquals("bearerAuth", config.name()),
                () -> Assertions.assertEquals("http",       config.type())
        );
    }

    @Test
    void of_optionalFieldsAreNull() {
        SecuritySchemeConfig config = SecuritySchemeConfig.of("bearerAuth", "http");

        Assertions.assertAll(
                () -> Assertions.assertNull(config.scheme(),           "scheme must default to null"),
                () -> Assertions.assertNull(config.bearerFormat(),     "bearerFormat must default to null"),
                () -> Assertions.assertNull(config.description(),      "description must default to null"),
                () -> Assertions.assertNull(config.in(),               "in must default to null"),
                () -> Assertions.assertNull(config.parameterName(),    "parameterName must default to null"),
                () -> Assertions.assertNull(config.openIdConnectUrl(), "openIdConnectUrl must default to null")
        );
    }

    // ==========================================================================
    // Null / blank validation on mandatory fields
    // ==========================================================================

    @Test
    void constructor_nullName_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> SecuritySchemeConfig.of(null, "http"),
                "A null name must throw NullPointerException"
        );
    }

    @Test
    void constructor_blankName_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SecuritySchemeConfig.of("   ", "http"),
                "A blank name must throw IllegalArgumentException"
        );
    }

    @Test
    void constructor_emptyName_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SecuritySchemeConfig.of("", "http"),
                "An empty name must throw IllegalArgumentException"
        );
    }

    @Test
    void constructor_nullType_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> SecuritySchemeConfig.of("bearerAuth", null),
                "A null type must throw NullPointerException"
        );
    }

    @Test
    void constructor_blankType_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SecuritySchemeConfig.of("bearerAuth", "  "),
                "A blank type must throw IllegalArgumentException"
        );
    }

    // ==========================================================================
    // Optional fields may be null
    // ==========================================================================

    @Test
    void constructor_allFieldsNull_exceptNameAndType_isValid() {
        SecuritySchemeConfig config = new SecuritySchemeConfig(
                "myScheme", "http", null, null, null, null, null, null);

        Assertions.assertAll(
                () -> Assertions.assertEquals("myScheme", config.name()),
                () -> Assertions.assertEquals("http",     config.type()),
                () -> Assertions.assertNull(config.scheme())
        );
    }

    @Test
    void constructor_allFieldsPopulated_storedCorrectly() {
        SecuritySchemeConfig config = new SecuritySchemeConfig(
                "apiKeyAuth", "apiKey", null, null, "API key in header",
                "header", "X-API-Key", null);

        Assertions.assertAll(
                () -> Assertions.assertEquals("apiKeyAuth",        config.name()),
                () -> Assertions.assertEquals("apiKey",            config.type()),
                () -> Assertions.assertEquals("API key in header", config.description()),
                () -> Assertions.assertEquals("header",            config.in()),
                () -> Assertions.assertEquals("X-API-Key",         config.parameterName())
        );
    }

    // ==========================================================================
    // withXxx wither methods
    // ==========================================================================

    @Test
    void withScheme_returnsNewRecordWithSchemeSet() {
        SecuritySchemeConfig original = SecuritySchemeConfig.of("bearerAuth", "http");
        SecuritySchemeConfig updated  = original.withScheme("bearer");

        Assertions.assertAll(
                () -> Assertions.assertEquals("bearer",    updated.scheme()),
                () -> Assertions.assertEquals("bearerAuth", updated.name()),
                () -> Assertions.assertEquals("http",      updated.type()),
                () -> Assertions.assertNotSame(original, updated, "withScheme must return a new instance")
        );
    }

    @Test
    void withBearerFormat_returnsNewRecordWithBearerFormatSet() {
        SecuritySchemeConfig config = SecuritySchemeConfig.of("bearerAuth", "http")
                .withScheme("bearer")
                .withBearerFormat("JWT");

        Assertions.assertAll(
                () -> Assertions.assertEquals("bearer", config.scheme()),
                () -> Assertions.assertEquals("JWT",    config.bearerFormat())
        );
    }

    @Test
    void withDescription_returnsNewRecordWithDescriptionSet() {
        SecuritySchemeConfig config = SecuritySchemeConfig.of("bearerAuth", "http")
                .withDescription("Bearer token authentication");

        Assertions.assertEquals("Bearer token authentication", config.description());
    }

    @Test
    void withScheme_doesNotMutateOriginal() {
        SecuritySchemeConfig original = SecuritySchemeConfig.of("s", "http");
        original.withScheme("bearer");

        Assertions.assertNull(original.scheme(), "Original must not be mutated by wither");
    }

    // ==========================================================================
    // Record equality
    // ==========================================================================

    @Test
    void equals_twoRecordsWithSameValues_areEqual() {
        SecuritySchemeConfig a = SecuritySchemeConfig.of("bearerAuth", "http").withScheme("bearer").withBearerFormat("JWT");
        SecuritySchemeConfig b = SecuritySchemeConfig.of("bearerAuth", "http").withScheme("bearer").withBearerFormat("JWT");

        Assertions.assertEquals(a, b, "Two SecuritySchemeConfig records with identical values must be equal");
    }

    @Test
    void equals_recordsWithDifferentName_areNotEqual() {
        SecuritySchemeConfig a = SecuritySchemeConfig.of("scheme1", "http");
        SecuritySchemeConfig b = SecuritySchemeConfig.of("scheme2", "http");

        Assertions.assertNotEquals(a, b);
    }
}
