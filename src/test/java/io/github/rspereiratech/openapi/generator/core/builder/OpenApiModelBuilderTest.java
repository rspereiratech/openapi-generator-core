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
package io.github.rspereiratech.openapi.generator.core.builder;

import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenApiModelBuilderTest {

    private GeneratorConfig configWithAppName(String contextPath) {
        return GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://api.example.com", "Production")
                .server("https://staging.example.com/", "Staging")
                .contextPath(contextPath)
                .build();
    }

    private GeneratorConfig configWithoutAppName() {
        return GeneratorConfig.builder()
                .basePackage("com.example")
                .server("https://api.example.com", "Production")
                .build();
    }

    // ==========================================================================
    // buildServerUrl — no contextPath
    // ==========================================================================

    @Test
    void buildServerUrl_noApplicationName_returnsOriginalUrl() {
        OpenApiModelBuilder builder = new OpenApiModelBuilder(configWithoutAppName());
        assertEquals("https://api.example.com", builder.buildServerUrl("https://api.example.com"));
    }

    @Test
    void buildServerUrl_nullApplicationName_returnsOriginalUrl() {
        OpenApiModelBuilder builder = new OpenApiModelBuilder(configWithAppName(null));
        assertEquals("https://api.example.com", builder.buildServerUrl("https://api.example.com"));
    }

    // ==========================================================================
    // buildServerUrl — with contextPath
    // ==========================================================================

    @Test
    void buildServerUrl_withApplicationName_appendsPathWithTrailingSlash() {
        OpenApiModelBuilder builder = new OpenApiModelBuilder(configWithAppName("vcc-superx-api"));
        assertEquals("https://api.example.com/vcc-superx-api/",
                builder.buildServerUrl("https://api.example.com"));
    }

    @Test
    void buildServerUrl_baseUrlHasTrailingSlash_doesNotDoubleSlash() {
        OpenApiModelBuilder builder = new OpenApiModelBuilder(configWithAppName("vcc-superx-api"));
        assertEquals("https://api.example.com/vcc-superx-api/",
                builder.buildServerUrl("https://api.example.com/"));
    }

    // ==========================================================================
    // build() — servers with contextPath applied
    // ==========================================================================

    @Test
    void build_contextPath_appendedToAllServerUrls() {
        OpenAPI api = new OpenApiModelBuilder(configWithAppName("vcc-superx-api")).build();

        List<Server> servers = api.getServers();
        assertAll(
                () -> assertEquals(2, servers.size()),
                () -> assertEquals("https://api.example.com/vcc-superx-api/", servers.get(0).getUrl()),
                () -> assertEquals("https://staging.example.com/vcc-superx-api/", servers.get(1).getUrl())
        );
    }

    @Test
    void build_noApplicationName_serverUrlsUnchanged() {
        OpenAPI api = new OpenApiModelBuilder(configWithoutAppName()).build();

        assertEquals("https://api.example.com", api.getServers().get(0).getUrl());
    }

    // ==========================================================================
    // build() — null / missing contextPath does not create components
    // ==========================================================================

    @Test
    void build_noSecuritySchemes_componentsIsNull() {
        OpenAPI api = new OpenApiModelBuilder(configWithoutAppName()).build();
        assertNull(api.getComponents());
    }
}
