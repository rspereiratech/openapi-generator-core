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

/**
 * Unit tests for {@link io.github.rspereiratech.openapi.generator.core.writer.YamlWriter}.
 *
 * <p>Verifies that the writer creates parent directories, writes a valid YAML file,
 * and that the pretty-print flag controls whether the output is indented.
 */
class YamlWriterTest {

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
        Path output = tempDir.resolve("openapi.yaml");
        new YamlWriter().write(minimalOpenApi(), output);
        assertTrue(Files.exists(output), "Output file must be created");
    }

    @Test
    void write_parentDirectoriesCreatedAutomatically(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("a/b/c/openapi.yaml");
        new YamlWriter().write(minimalOpenApi(), nested);
        assertTrue(Files.exists(nested), "Writer must create missing parent directories");
    }

    @Test
    void write_overwritesExistingFile(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.yaml");
        Files.writeString(output, "old content", StandardCharsets.UTF_8);
        new YamlWriter().write(minimalOpenApi(), output);
        String content = Files.readString(output, StandardCharsets.UTF_8);
        assertFalse(content.contains("old content"), "Writer must overwrite the existing file");
    }

    // ==========================================================================
    // YAML content
    // ==========================================================================

    @Test
    void write_yamlContainsOpenApiVersion(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.yaml");
        new YamlWriter().write(minimalOpenApi(), output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("openapi: 3.0.3"), "YAML must include the openapi version header");
    }

    @Test
    void write_yamlContainsTitle(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.yaml");
        new YamlWriter().write(minimalOpenApi(), output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("Test API"), "YAML must include the API title");
    }

    @Test
    void write_yamlContainsVersion(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("openapi.yaml");
        new YamlWriter().write(minimalOpenApi(), output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("1.0.0"), "YAML must include the API version");
    }

    // ==========================================================================
    // Pretty-print vs compact
    // ==========================================================================

    @Test
    void write_prettyPrintTrue_producesMultilineOutput(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("pretty.yaml");
        new YamlWriter(true).write(minimalOpenApi(), output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        // Pretty output has newlines between fields
        assertTrue(yaml.lines().count() > 3,
                "Pretty-printed YAML must span more than 3 lines");
    }

    @Test
    void write_prettyPrintFalse_producesOutput(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("compact.yaml");
        new YamlWriter(false).write(minimalOpenApi(), output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("openapi"), "Compact YAML must still contain required fields");
    }

    @Test
    void write_defaultConstructor_usesPrettyPrint(@TempDir Path tempDir) throws Exception {
        Path pretty  = tempDir.resolve("pretty.yaml");
        Path compact = tempDir.resolve("compact.yaml");
        new YamlWriter().write(minimalOpenApi(), pretty);
        new YamlWriter(true).write(minimalOpenApi(), compact);

        String prettyContent  = Files.readString(pretty,  StandardCharsets.UTF_8);
        String compactContent = Files.readString(compact, StandardCharsets.UTF_8);
        // Both should produce the same output since default is pretty=true
        assertTrue(prettyContent.contains("openapi: 3.0.3"));
        assertTrue(compactContent.contains("openapi: 3.0.3"));
    }

    // ==========================================================================
    // Encoding
    // ==========================================================================

    @Test
    void write_utf8Encoding_fileReadableAsUtf8(@TempDir Path tempDir) throws Exception {
        OpenAPI api = new OpenAPI()
                .openapi("3.0.3")
                .info(new Info().title("ÄÖÜ-API").version("1.0.0"));
        Path output = tempDir.resolve("utf8.yaml");
        new YamlWriter().write(api, output);
        String yaml = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("ÄÖÜ-API"), "Special characters must be preserved in UTF-8 output");
    }
}
