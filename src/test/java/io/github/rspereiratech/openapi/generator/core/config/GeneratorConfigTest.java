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

import java.util.List;


class GeneratorConfigTest {

    // ==========================================================================
    // Builder validation
    // ==========================================================================

    @Test
    void build_noBasePackage_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().build()
        );
    }

    @Test
    void basePackage_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage(null)
        );
    }

    @Test
    void basePackage_blank_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("   ")
        );
    }

    @Test
    void basePackage_empty_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("")
        );
    }

    @Test
    void basePackages_emptyList_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackages(List.of())
        );
    }

    @Test
    void basePackages_withNullElement_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackages(java.util.Arrays.asList("com.example", null)).build()
        );
    }

    @Test
    void basePackages_withBlankElement_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackages(java.util.Arrays.asList("com.example", "  ")).build()
        );
    }

    @Test
    void outputFile_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").outputFile(null).build()
        );
    }

    @Test
    void outputFile_blank_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").outputFile("  ").build()
        );
    }

    @Test
    void title_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").title(null).build()
        );
    }

    @Test
    void title_blank_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").title("  ").build()
        );
    }

    @Test
    void version_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").version(null).build()
        );
    }

    @Test
    void version_blank_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").version("  ").build()
        );
    }

    @Test
    void controllerAnnotation_blank_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").controllerAnnotation("")
        );
    }

    @Test
    void controllerAnnotations_emptyList_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").controllerAnnotations(List.of())
        );
    }

    @Test
    void controllerAnnotations_withBlankElement_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder()
                        .basePackage("com.example")
                        .controllerAnnotations(java.util.Arrays.asList("com.example.Ann", "  "))
                        .build()
        );
    }

    @Test
    void controllerAnnotations_withNullElement_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder()
                        .basePackage("com.example")
                        .controllerAnnotations(java.util.Arrays.asList("com.example.Ann", null))
                        .build()
        );
    }

    // ==========================================================================
    // Default values
    // ==========================================================================

    @Test
    void build_defaultValues_areCorrect() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals("API",          config.title(),       "default title"),
                () -> Assertions.assertEquals("1.0.0",        config.version(),     "default version"),
                () -> Assertions.assertEquals("docs/swagger/openapi.yaml", config.outputFile(),  "default outputFile"),
                () -> Assertions.assertTrue(config.prettyPrint(),                    "default prettyPrint"),
                () -> Assertions.assertTrue(config.servers().isEmpty(),             "default servers empty"),
                () -> Assertions.assertTrue(config.controllerAnnotations().isEmpty(),
                        "default controllerAnnotations empty"),
                () -> Assertions.assertEquals("",             config.description(), "default description blank"),
                () -> Assertions.assertEquals(OutputFormat.YAML, config.outputFormat(), "default outputFormat"),
                () -> Assertions.assertTrue(config.securitySchemes().isEmpty(), "default securitySchemes empty"),
                () -> Assertions.assertNull(config.contextPath(), "default contextPath is null")
        );
    }

    // ==========================================================================
    // basePackages
    // ==========================================================================

    @Test
    void basePackage_singlePackage_addedCorrectly() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.api")
                .build();

        Assertions.assertEquals(List.of("com.example.api"), config.basePackages());
    }

    @Test
    void basePackages_listVariant_addsAllPackages() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackages(List.of("com.example.a", "com.example.b"))
                .build();

        Assertions.assertEquals(List.of("com.example.a", "com.example.b"), config.basePackages());
    }

    @Test
    void basePackages_mixedCalls_accumulatesAll() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example.a")
                .basePackages(List.of("com.example.b", "com.example.c"))
                .build();

        Assertions.assertEquals(
                List.of("com.example.a", "com.example.b", "com.example.c"),
                config.basePackages()
        );
    }

    // ==========================================================================
    // Server configuration
    // ==========================================================================

    @Test
    void serverUrl_backwardCompat_addsSingleServerWithEmptyDescription() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .serverUrl("https://api.example.com")
                .build();

        Assertions.assertEquals(1, config.servers().size());
        ServerConfig server = config.servers().get(0);
        Assertions.assertAll(
                () -> Assertions.assertEquals("https://api.example.com", server.url()),
                () -> Assertions.assertEquals("", server.description())
        );
    }

    @Test
    void server_urlOnly_addsSingleServerWithEmptyDescription() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://example.com")
                .build();

        Assertions.assertEquals(1, config.servers().size());
        Assertions.assertEquals("https://example.com", config.servers().get(0).url());
        Assertions.assertEquals("", config.servers().get(0).description());
    }

    @Test
    void server_urlAndDescription_addsSingleServerWithDescription() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://example.com", "Production")
                .build();

        Assertions.assertEquals(1, config.servers().size());
        ServerConfig s = config.servers().get(0);
        Assertions.assertAll(
                () -> Assertions.assertEquals("https://example.com", s.url()),
                () -> Assertions.assertEquals("Production", s.description())
        );
    }

    @Test
    void servers_listVariant_replacesExistingServers() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://old.example.com")
                .servers(List.of(
                        ServerConfig.of("https://new1.example.com", "New 1"),
                        ServerConfig.of("https://new2.example.com", "New 2")
                ))
                .build();

        Assertions.assertEquals(2, config.servers().size());
        Assertions.assertEquals("https://new1.example.com", config.servers().get(0).url());
        Assertions.assertEquals("https://new2.example.com", config.servers().get(1).url());
    }

    @Test
    void servers_emptyList_throwsIllegalArgumentException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").servers(List.of())
        );
    }

    @Test
    void server_serverConfig_addedDirectly() {
        ServerConfig sc = ServerConfig.of("https://direct.example.com", "Direct");
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .server(sc)
                .build();

        Assertions.assertEquals(1, config.servers().size());
        Assertions.assertEquals(sc, config.servers().get(0));
    }

    // ==========================================================================
    // Controller annotations
    // ==========================================================================

    @Test
    void controllerAnnotation_singleFqn_addedCorrectly() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .controllerAnnotation("com.example.MyAnnotation")
                .build();

        Assertions.assertEquals(List.of("com.example.MyAnnotation"), config.controllerAnnotations());
    }

    @Test
    void controllerAnnotations_listVariant_replacesExistingAnnotations() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .controllerAnnotation("com.example.OldAnnotation")
                .controllerAnnotations(List.of(
                        "com.example.NewAnnotation1",
                        "com.example.NewAnnotation2"
                ))
                .build();

        Assertions.assertEquals(
                List.of("com.example.NewAnnotation1", "com.example.NewAnnotation2"),
                config.controllerAnnotations()
        );
    }

    @Test
    void controllerAnnotations_multipleAddCalls_accumulatesAll() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .controllerAnnotation("com.example.Ann1")
                .controllerAnnotation("com.example.Ann2")
                .build();

        Assertions.assertEquals(
                List.of("com.example.Ann1", "com.example.Ann2"),
                config.controllerAnnotations()
        );
    }

    // ==========================================================================
    // Security schemes
    // ==========================================================================

    @Test
    void securityScheme_singleEntry_addedCorrectly() {
        SecuritySchemeConfig scheme = SecuritySchemeConfig.of("bearerAuth", "http")
                .withScheme("bearer").withBearerFormat("JWT");
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .securityScheme(scheme)
                .build();

        Assertions.assertEquals(1, config.securitySchemes().size());
        Assertions.assertEquals(scheme, config.securitySchemes().get(0));
    }

    @Test
    void securityScheme_multipleAddCalls_accumulatesAll() {
        SecuritySchemeConfig s1 = SecuritySchemeConfig.of("bearerAuth", "http");
        SecuritySchemeConfig s2 = SecuritySchemeConfig.of("apiKeyAuth", "apiKey");
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .securityScheme(s1)
                .securityScheme(s2)
                .build();

        Assertions.assertEquals(2, config.securitySchemes().size());
    }

    @Test
    void securitySchemes_listVariant_replacesExistingSchemes() {
        SecuritySchemeConfig old = SecuritySchemeConfig.of("oldScheme", "http");
        SecuritySchemeConfig s1  = SecuritySchemeConfig.of("scheme1", "http");
        SecuritySchemeConfig s2  = SecuritySchemeConfig.of("scheme2", "apiKey");
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .securityScheme(old)
                .securitySchemes(List.of(s1, s2))
                .build();

        Assertions.assertEquals(List.of(s1, s2), config.securitySchemes());
    }

    @Test
    void securityScheme_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").securityScheme(null)
        );
    }

    @Test
    void securitySchemes_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder().basePackage("com.example").securitySchemes(null)
        );
    }

    @Test
    void getSecuritySchemes_returnedList_isUnmodifiable() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .securityScheme(SecuritySchemeConfig.of("s", "http"))
                .build();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> config.securitySchemes().add(SecuritySchemeConfig.of("hacked", "http")),
                "getSecuritySchemes() must return an unmodifiable list"
        );
    }

    // ==========================================================================
    // contextPath
    // ==========================================================================

    @Test
    void contextPath_set_storedCorrectly() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .contextPath("vcc-batatas")
                .build();

        Assertions.assertEquals("vcc-batatas", config.contextPath());
    }

    @Test
    void contextPath_null_storedAsNull() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .contextPath(null)
                .build();

        Assertions.assertNull(config.contextPath());
    }

    // ==========================================================================
    // Unmodifiability
    // ==========================================================================

    @Test
    void getBasePackages_returnedList_isUnmodifiable() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .build();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> config.basePackages().add("com.example.hacked"),
                "getBasePackages() must return an unmodifiable list"
        );
    }

    @Test
    void getServers_returnedList_isUnmodifiable() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://example.com")
                .build();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> config.servers().add(ServerConfig.of("https://hacked.com")),
                "getServers() must return an unmodifiable list"
        );
    }

    @Test
    void getControllerAnnotations_returnedList_isUnmodifiable() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .controllerAnnotation("com.example.Ann")
                .build();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> config.controllerAnnotations().add("com.example.Hacked"),
                "getControllerAnnotations() must return an unmodifiable list"
        );
    }

    // ==========================================================================
    // Other builder setters
    // ==========================================================================

    // ==========================================================================
    // outputFormat
    // ==========================================================================

    @Test
    void outputFormat_defaultsToYaml() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .build();
        Assertions.assertEquals(OutputFormat.YAML, config.outputFormat());
    }

    @Test
    void outputFormat_json_storedCorrectly() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .outputFormat(OutputFormat.JSON)
                .build();
        Assertions.assertEquals(OutputFormat.JSON, config.outputFormat());
    }

    @Test
    void outputFormat_null_throwsNullPointerException() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> GeneratorConfig.builder()
                        .basePackage("com.example")
                        .outputFormat(null)
                        .build()
        );
    }

    @Test
    void build_customScalarFields_areStoredCorrectly() {
        GeneratorConfig config = GeneratorConfig.builder()
                .basePackage("com.example")
                .title("My API")
                .version("2.0.0")
                .description("A great API")
                .outputFile("build/openapi.yaml")
                .prettyPrint(false)
                .contactName("John Doe")
                .contactEmail("john@example.com")
                .contactUrl("https://example.com/contact")
                .licenseName("MIT")
                .licenseUrl("https://opensource.org/licenses/MIT")
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals("My API",               config.title()),
                () -> Assertions.assertEquals("2.0.0",                config.version()),
                () -> Assertions.assertEquals("A great API",          config.description()),
                () -> Assertions.assertEquals("build/openapi.yaml",   config.outputFile()),
                () -> Assertions.assertAll(
                        () -> Assertions.assertTrue(!config.prettyPrint()),
                        () -> Assertions.assertEquals("John Doe",         config.contactName()),
                        () -> Assertions.assertEquals("john@example.com", config.contactEmail()),
                        () -> Assertions.assertEquals("https://example.com/contact", config.contactUrl()),
                        () -> Assertions.assertEquals("MIT",              config.licenseName()),
                        () -> Assertions.assertEquals("https://opensource.org/licenses/MIT", config.licenseUrl())
                )
        );
    }
}
