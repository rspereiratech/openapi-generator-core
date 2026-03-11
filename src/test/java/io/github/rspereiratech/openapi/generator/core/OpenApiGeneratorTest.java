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
package io.github.rspereiratech.openapi.generator.core;

import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import io.github.rspereiratech.openapi.generator.core.config.OutputFormat;
import io.github.rspereiratech.openapi.generator.core.config.SecuritySchemeConfig;
import io.github.rspereiratech.openapi.generator.core.processor.DefaultProcessorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Integration tests for {@link OpenApiGeneratorImpl}.
 *
 * <p>Exercises the full generation pipeline end-to-end using fixture controllers
 * in {@code fixtures.integration}, verifying that the generated YAML/JSON spec
 * is written to disk and contains the expected structural elements.
 */
class OpenApiGeneratorTest {

    private static final String FIXTURE_PACKAGE =
            "io.github.rspereiratech.openapi.generator.core.fixtures.integration";

    // ==========================================================================
    // Tests
    // ==========================================================================

    @Test
    void generate_sampleControllers_producesValidOpenApiYaml(@TempDir Path tempDir) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage(FIXTURE_PACKAGE)
                .title("Integration Test API")
                .version("1.0.0")
                .outputFile(tempDir.resolve("openapi.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, loader);

        Path outputFile = tempDir.resolve("openapi.yaml");
        Assertions.assertTrue(Files.exists(outputFile), "Output file must exist after generation");

        String yaml = Files.readString(outputFile);

        Assertions.assertAll(
                "OpenAPI YAML content verification",

                // Version header
                () -> Assertions.assertTrue(yaml.contains("openapi: 3.0.3"),
                        "Generated YAML must start with openapi: 3.0.3"),

                // Paths for the four fixture controllers
                () -> Assertions.assertTrue(yaml.contains("/api/v1/users"),
                        "YAML must contain path /api/v1/users (UserController)"),
                () -> Assertions.assertTrue(yaml.contains("/api/v1/products"),
                        "YAML must contain path /api/v1/products (ProductController)"),
                () -> Assertions.assertTrue(yaml.contains("/api/v1/orders"),
                        "YAML must contain path /api/v1/orders (OrderController)"),
                () -> Assertions.assertTrue(yaml.contains("/api/v1/notifications"),
                        "YAML must contain path /api/v1/notifications (NotificationController)"),

                // Component schemas for request-body types
                () -> Assertions.assertTrue(yaml.contains("CreateUserRequest"),
                        "YAML must contain component schema CreateUserRequest (POST /users body)"),
                () -> Assertions.assertTrue(yaml.contains("ProductDto"),
                        "YAML must contain component schema ProductDto (POST /products body)"),
                () -> Assertions.assertTrue(yaml.contains("OrderDto"),
                        "YAML must contain component schema OrderDto (POST /orders body)"),
                () -> Assertions.assertTrue(yaml.contains("SendNotificationRequest"),
                        "YAML must contain component schema SendNotificationRequest (POST /notifications body)")
        );
    }

    @Test
    void generate_emptyBasePackage_writesMinimalYaml(@TempDir Path tempDir) throws Exception {
        // When no controllers are found the generator logs a warning but still writes a
        // valid (minimal) OpenAPI document.
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent.package.xyz")
                .title("Empty API")
                .version("0.0.1")
                .outputFile(tempDir.resolve("empty.yaml").toString())
                .build();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        new OpenApiGeneratorImpl().generate(config, loader);

        Path outputFile = tempDir.resolve("empty.yaml");
        Assertions.assertTrue(Files.exists(outputFile), "Output file must be created even when no controllers are found");

        String yaml = Files.readString(outputFile);
        Assertions.assertTrue(yaml.contains("openapi: 3.0.3"),
                "Minimal YAML must still include the openapi version header");
        Assertions.assertTrue(yaml.contains("Empty API"),
                "Minimal YAML must include the configured title");
    }

    @Test
    void generate_withServerUrl_serverAppearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage(FIXTURE_PACKAGE)
                .title("Server Test API")
                .version("1.0.0")
                .server("https://api.example.com", "Production")
                .outputFile(tempDir.resolve("server.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("server.yaml"));
        Assertions.assertTrue(yaml.contains("https://api.example.com"),
                "Configured server URL must appear in the generated YAML");
    }

    @Test
    void generate_outputFileInNestedDirectory_parentDirectoriesCreated(@TempDir Path tempDir) throws Exception {
        // The writer must create missing parent directories automatically.
        Path nestedOutput = tempDir.resolve("nested/deep/dir/openapi.yaml");

        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .outputFile(nestedOutput.toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(Files.exists(nestedOutput),
                "The generator must create parent directories and write the output file");
    }

    // ==========================================================================
    // DI constructor
    // ==========================================================================

    @Test
    void generate_withCustomFactory_producesOutput(@TempDir Path tempDir) throws Exception {
        OpenApiGenerator generator = new OpenApiGeneratorImpl(new DefaultProcessorFactory());

        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .outputFile(tempDir.resolve("custom-factory.yaml").toString())
                .build();

        generator.generate(config, Thread.currentThread().getContextClassLoader());

        Assertions.assertTrue(Files.exists(tempDir.resolve("custom-factory.yaml")),
                "Generator with custom factory must produce the output file");
    }

    // ==========================================================================
    // buildInfo: contact and license fields
    // ==========================================================================

    @Test
    void generate_withContactInfo_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .contactName("John Doe")
                .contactEmail("john@example.com")
                .contactUrl("https://example.com")
                .outputFile(tempDir.resolve("contact.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("contact.yaml"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(yaml.contains("John Doe"),   "Contact name must appear"),
                () -> Assertions.assertTrue(yaml.contains("john@example.com"), "Contact email must appear"),
                () -> Assertions.assertTrue(yaml.contains("https://example.com"), "Contact URL must appear")
        );
    }

    @Test
    void generate_withLicenseInfo_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .licenseName("Apache 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .outputFile(tempDir.resolve("license.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("license.yaml"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(yaml.contains("Apache 2.0"),    "License name must appear"),
                () -> Assertions.assertTrue(yaml.contains("apache.org"),     "License URL must appear")
        );
    }

    @Test
    void generate_withServerDescription_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .server("https://api.example.com", "Production server")
                .outputFile(tempDir.resolve("serverdesc.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("serverdesc.yaml"));
        Assertions.assertTrue(yaml.contains("Production server"),
                "Server description must appear in the generated YAML");
    }

    @Test
    void generate_withJsonFormat_producesValidJsonFile(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .title("JSON Test API")
                .version("1.0.0")
                .outputFormat(OutputFormat.JSON)
                .outputFile(tempDir.resolve("openapi.json").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        Path outputFile = tempDir.resolve("openapi.json");
        Assertions.assertTrue(Files.exists(outputFile), "JSON output file must be created");

        String json = Files.readString(outputFile);
        Assertions.assertAll(
                () -> Assertions.assertTrue(json.trim().startsWith("{"),   "JSON must start with '{'"),
                () -> Assertions.assertTrue(json.contains("\"openapi\""),  "JSON must contain the openapi key"),
                () -> Assertions.assertTrue(json.contains("3.0.3"),        "JSON must contain the openapi version"),
                () -> Assertions.assertTrue(json.contains("JSON Test API"),"JSON must contain the configured title")
        );
    }

    // ==========================================================================
    // Security schemes
    // ==========================================================================

    @Test
    void generate_withBearerJwtScheme_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .securityScheme(SecuritySchemeConfig.of("bearerAuth", "http")
                        .withScheme("bearer")
                        .withBearerFormat("JWT")
                        .withDescription("Bearer token authentication"))
                .outputFile(tempDir.resolve("security.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("security.yaml"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(yaml.contains("securitySchemes"),
                        "YAML must contain securitySchemes block"),
                () -> Assertions.assertTrue(yaml.contains("bearerAuth"),
                        "YAML must contain the scheme name 'bearerAuth'"),
                () -> Assertions.assertTrue(yaml.contains("bearer"),
                        "YAML must contain the scheme value 'bearer'"),
                () -> Assertions.assertTrue(yaml.contains("JWT"),
                        "YAML must contain bearerFormat JWT"),
                () -> Assertions.assertTrue(yaml.contains("security:"),
                        "YAML must contain root security list")
        );
    }

    @Test
    void generate_withApiKeyScheme_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .securityScheme(new SecuritySchemeConfig(
                        "apiKeyAuth", "apiKey", null, null,
                        "API key in header", "header", "X-API-Key", null))
                .outputFile(tempDir.resolve("apikey.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("apikey.yaml"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(yaml.contains("apiKeyAuth"),   "YAML must contain scheme name 'apiKeyAuth'"),
                () -> Assertions.assertTrue(yaml.contains("apiKey"),       "YAML must contain type 'apiKey'"),
                () -> Assertions.assertTrue(yaml.contains("X-API-Key"),    "YAML must contain the parameter name 'X-API-Key'"),
                () -> Assertions.assertTrue(yaml.contains("header"),       "YAML must contain in: header")
        );
    }

    @Test
    void generate_withMultipleSecuritySchemes_allAppearInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .securityScheme(SecuritySchemeConfig.of("bearerAuth", "http").withScheme("bearer"))
                .securityScheme(new SecuritySchemeConfig(
                        "apiKeyAuth", "apiKey", null, null, null, "header", "X-API-Key", null))
                .outputFile(tempDir.resolve("multi.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("multi.yaml"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(yaml.contains("bearerAuth"), "YAML must contain bearerAuth"),
                () -> Assertions.assertTrue(yaml.contains("apiKeyAuth"), "YAML must contain apiKeyAuth")
        );
    }

    @Test
    void generate_withNoSecuritySchemes_noSecurityBlockInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .outputFile(tempDir.resolve("nosec.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("nosec.yaml"));
        Assertions.assertFalse(yaml.contains("securitySchemes"),
                "YAML must not contain securitySchemes when none are configured");
    }

    @Test
    void generate_withUnsupportedSchemeType_throwsException(@TempDir Path tempDir) {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .securityScheme(SecuritySchemeConfig.of("badScheme", "unsupportedType"))
                .outputFile(tempDir.resolve("bad.yaml").toString())
                .build();

        Assertions.assertThrows(
                Exception.class,
                () -> new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader()),
                "An unsupported scheme type must cause generation to fail"
        );
    }

    @Test
    void generate_withSchemeDescription_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .securityScheme(SecuritySchemeConfig.of("bearerAuth", "http")
                        .withScheme("bearer")
                        .withDescription("Use a JWT obtained from /auth/token"))
                .outputFile(tempDir.resolve("desc.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("desc.yaml"));
        Assertions.assertTrue(yaml.contains("Use a JWT obtained from /auth/token"),
                "Scheme description must appear in the generated YAML");
    }

    @Test
    void generate_withDescription_appearsInYaml(@TempDir Path tempDir) throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.nonexistent")
                .description("My detailed API description")
                .outputFile(tempDir.resolve("desc.yaml").toString())
                .build();

        new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());

        String yaml = Files.readString(tempDir.resolve("desc.yaml"));
        Assertions.assertTrue(yaml.contains("My detailed API description"),
                "API description must appear in the generated YAML");
    }
}
