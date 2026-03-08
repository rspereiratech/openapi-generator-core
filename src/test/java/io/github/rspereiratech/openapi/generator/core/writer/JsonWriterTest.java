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
package io.github.rspereiratech.openapi.generator.core.writer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonWriterTest {

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private static OpenAPI minimalOpenApi() {
        return new OpenAPI()
                .openapi("3.0.3")
                .info(new Info().title("Test API").version("1.0.0"));
    }

    // ==========================================================================
    // File creation
    // ==========================================================================

    @Test
    void write_createsOutputFile(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        new JsonWriter().write(minimalOpenApi(), output);
        assertTrue(Files.exists(output), "Output file must be created");
    }

    @Test
    void write_parentDirectoriesCreatedAutomatically(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("a/b/c/openapi.json");
        new JsonWriter().write(minimalOpenApi(), nested);
        assertTrue(Files.exists(nested), "Writer must create missing parent directories");
    }

    @Test
    void write_overwritesExistingFile(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        Files.writeString(output, "old content", StandardCharsets.UTF_8);
        new JsonWriter().write(minimalOpenApi(), output);
        String content = Files.readString(output, StandardCharsets.UTF_8);
        assertFalse(content.contains("old content"), "Writer must overwrite the existing file");
    }

    // ==========================================================================
    // JSON content
    // ==========================================================================

    @Test
    void write_jsonContainsOpenApiVersion(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        new JsonWriter().write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"openapi\""), "JSON must include the openapi key");
        assertTrue(json.contains("3.0.3"), "JSON must include the openapi version value");
    }

    @Test
    void write_jsonContainsTitle(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        new JsonWriter().write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.contains("Test API"), "JSON must include the API title");
    }

    @Test
    void write_jsonContainsVersion(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        new JsonWriter().write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.contains("1.0.0"), "JSON must include the API version");
    }

    @Test
    void write_outputIsValidJson(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.json");
        new JsonWriter().write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        // Minimal structural check: JSON must start with '{' and end with '}'
        assertTrue(json.trim().startsWith("{"), "JSON output must start with '{'");
        assertTrue(json.trim().endsWith("}"), "JSON output must end with '}'");
    }

    // ==========================================================================
    // Pretty-print vs compact
    // ==========================================================================

    @Test
    void write_prettyPrintTrue_producesMultilineOutput(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("pretty.json");
        new JsonWriter(true).write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.lines().count() > 3,
                "Pretty-printed JSON must span more than 3 lines");
    }

    @Test
    void write_prettyPrintFalse_producesSingleLineOutput(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("compact.json");
        new JsonWriter(false).write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.contains("openapi"), "Compact JSON must still contain required fields");
        // Compact output should be a single line (no newlines except possibly trailing)
        assertTrue(json.lines().count() <= 2,
                "Compact JSON must be single-line");
    }

    @Test
    void write_defaultConstructor_usesPrettyPrint(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("default.json");
        new JsonWriter().write(minimalOpenApi(), output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.lines().count() > 3,
                "Default constructor must produce pretty-printed (multi-line) JSON");
    }

    // ==========================================================================
    // Encoding
    // ==========================================================================

    @Test
    void write_utf8Encoding_specialCharactersPreserved(@TempDir Path tempDir) throws Exception {
        OpenAPI api = new OpenAPI()
                .openapi("3.0.3")
                .info(new Info().title("ÄÖÜ-API").version("1.0.0"));
        Path output = tempDir.resolve("utf8.json");
        new JsonWriter().write(api, output);
        String json = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(json.contains("ÄÖÜ-API"), "Special characters must be preserved in UTF-8 output");
    }
}
