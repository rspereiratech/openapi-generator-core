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


/**
 * Unit tests for {@link ServerConfig}.
 *
 * <p>Covers factory-method variants, null and blank validation on the {@code url} field,
 * normalisation of a {@code null} description to an empty string, and record equality.
 */
class ServerConfigTest {

    // ==========================================================================
    // of(url)
    // ==========================================================================

    @Test
    void of_urlOnly_setsUrlAndDefaultsDescriptionToEmpty() {
        ServerConfig config = ServerConfig.of("https://api.example.com");

        Assertions.assertAll(
                () -> Assertions.assertEquals("https://api.example.com", config.url()),
                () -> Assertions.assertEquals("", config.description(),
                        "description must default to empty string when not provided")
        );
    }

    // ==========================================================================
    // of(url, description)
    // ==========================================================================

    @Test
    void of_urlAndDescription_setsBothFieldsCorrectly() {
        ServerConfig config = ServerConfig.of("https://api.example.com", "Production server");

        Assertions.assertAll(
                () -> Assertions.assertEquals("https://api.example.com", config.url()),
                () -> Assertions.assertEquals("Production server", config.description())
        );
    }

    @Test
    void of_urlAndEmptyDescription_storesEmptyDescription() {
        ServerConfig config = ServerConfig.of("https://example.com", "");

        Assertions.assertEquals("", config.description());
    }

    @Test
    void of_urlAndBlankDescription_storesBlankDescription() {
        // A non-null blank description is NOT normalised; it is stored as-is.
        ServerConfig config = ServerConfig.of("https://example.com", "   ");
        Assertions.assertEquals("   ", config.description());
    }

    // ==========================================================================
    // Null handling
    // ==========================================================================

    @Test
    void of_nullUrl_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> ServerConfig.of(null),
                "A null URL must immediately throw NullPointerException"
        );
    }

    @Test
    void of_nullUrlWithDescription_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> ServerConfig.of(null, "Some description"),
                "A null URL must throw NullPointerException even when description is provided"
        );
    }

    @Test
    void of_emptyUrl_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.of(""),
                "An empty URL must throw IllegalArgumentException"
        );
    }

    @Test
    void of_blankUrl_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.of("   "),
                "A blank URL must throw IllegalArgumentException"
        );
    }

    @Test
    void of_nullDescription_normalisedToEmptyString() {
        // The compact constructor normalises null description to ""
        ServerConfig config = ServerConfig.of("https://example.com", null);
        Assertions.assertEquals("", config.description(),
                "A null description must be normalised to an empty string");
    }

    // ==========================================================================
    // Record accessor methods
    // ==========================================================================

    @Test
    void url_accessorMethod_returnsCorrectValue() {
        ServerConfig config = new ServerConfig("https://example.com", "desc");
        Assertions.assertEquals("https://example.com", config.url());
    }

    @Test
    void description_accessorMethod_returnsCorrectValue() {
        ServerConfig config = new ServerConfig("https://example.com", "My description");
        Assertions.assertEquals("My description", config.description());
    }

    @Test
    void equals_twoRecordsWithSameValues_areEqual() {
        ServerConfig a = ServerConfig.of("https://example.com", "Production");
        ServerConfig b = ServerConfig.of("https://example.com", "Production");
        Assertions.assertEquals(a, b, "Two ServerConfig records with identical values must be equal");
    }
}
