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
import io.github.rspereiratech.openapi.generator.core.config.SecuritySchemeConfig;
import io.github.rspereiratech.openapi.generator.core.config.ServerConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the initial {@link OpenAPI} model from a {@link GeneratorConfig}.
 *
 * <p>Covers the static sections of the specification — info, servers, and security schemes —
 * which are derived directly from configuration and are independent of classpath scanning.
 *
 * @author ruispereira
 */
@Slf4j
public final class OpenApiModelBuilder {

    /** Configuration driving the OpenAPI model construction. */
    private final GeneratorConfig config;

    /**
     * Creates a builder for the given configuration.
     *
     * @param config the generator configuration; must not be {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public OpenApiModelBuilder(GeneratorConfig config) {
        this.config = Preconditions.checkNotNull(config, "'config' must not be null");
    }

    /**
     * Constructs and returns a populated {@link OpenAPI} model.
     *
     * @return a new {@link OpenAPI} with info, servers, and security schemes set
     */
    public OpenAPI build() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.openapi("3.0.3");
        openAPI.setInfo(buildInfo());

        if (!config.servers().isEmpty()) {
            openAPI.servers(buildServers());
        }

        if (!config.securitySchemes().isEmpty()) {
            openAPI.setComponents(new Components());
            openAPI.getComponents().setSecuritySchemes(buildSecuritySchemes());
            openAPI.security(buildSecurityRequirements());
        }
        return openAPI;
    }

    // ------------------------------------------------------------------
    // Info
    // ------------------------------------------------------------------

    private Info buildInfo() {
        Info info = new Info()
                .title(config.title())
                .version(config.version())
                .description(config.description());

        if (StringUtils.hasText(config.contactName())
                || StringUtils.hasText(config.contactEmail())
                || StringUtils.hasText(config.contactUrl())) {
            info.setContact(new Contact()
                    .name(config.contactName())
                    .email(config.contactEmail())
                    .url(config.contactUrl()));
        }

        if (StringUtils.hasText(config.licenseName())) {
            info.setLicense(new License()
                    .name(config.licenseName())
                    .url(config.licenseUrl()));
        }

        return info;
    }

    // ------------------------------------------------------------------
    // Servers
    // ------------------------------------------------------------------

    private List<Server> buildServers() {
        List<Server> servers = config.servers().stream()
                .map(cfg -> {
                    Server server = new Server().url(buildServerUrl(cfg.url()));
                    if (StringUtils.hasText(cfg.description())) server.setDescription(cfg.description());
                    return server;
                })
                .toList();
        log.info("{} server(s) configured", servers.size());
        return servers;
    }

    /**
     * Appends the configured application name to a server base URL.
     *
     * <p>When {@code contextPath} is set in the config, the URL
     * {@code https://api.example.com} becomes {@code https://api.example.com/my-app/}.
     * Any existing trailing slash on the base URL is stripped before appending.
     *
     * @param baseUrl the raw server URL from config
     * @return the URL with the application name appended, or the original URL when
     *         {@code contextPath} is not configured
     */
    String buildServerUrl(String baseUrl) {
        if (!StringUtils.hasText(config.contextPath())) {
            return baseUrl;
        }
        String stripped = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return stripped + "/" + config.contextPath() + "/";
    }

    // ------------------------------------------------------------------
    // Security schemes
    // ------------------------------------------------------------------

    /**
     * Registers each {@link SecuritySchemeConfig} under {@code components/securitySchemes}
     * and appends a matching entry to the root {@code security} list.
     *
     * <p>Scheme type and {@code in} values are parsed case-insensitively.
     *
     * @throws IllegalArgumentException if a scheme type or {@code in} value is unsupported
     */
    private Map<String, SecurityScheme> buildSecuritySchemes() {
        Map<String, SecurityScheme> schemes = new LinkedHashMap<>();
        for (SecuritySchemeConfig ssc : config.securitySchemes()) {
            SecurityScheme scheme = new SecurityScheme()
                    .type(parseSchemeType(ssc.type()));
            if (StringUtils.hasText(ssc.scheme()))           scheme.setScheme(ssc.scheme());
            if (StringUtils.hasText(ssc.bearerFormat()))     scheme.setBearerFormat(ssc.bearerFormat());
            if (StringUtils.hasText(ssc.description()))      scheme.setDescription(ssc.description());
            if (StringUtils.hasText(ssc.in()))               scheme.setIn(parseSchemeIn(ssc.in()));
            if (StringUtils.hasText(ssc.parameterName()))    scheme.setName(ssc.parameterName());
            if (StringUtils.hasText(ssc.openIdConnectUrl())) scheme.setOpenIdConnectUrl(ssc.openIdConnectUrl());
            schemes.put(ssc.name(), scheme);
        }
        log.info("Registered {} security scheme(s)", schemes.size());
        return schemes;
    }

    private List<SecurityRequirement> buildSecurityRequirements() {
        return config.securitySchemes().stream()
                .map(ssc -> new SecurityRequirement().addList(ssc.name()))
                .toList();
    }

    private static SecurityScheme.Type parseSchemeType(String type) {
        return switch (type.toLowerCase()) {
            case "apikey"        -> SecurityScheme.Type.APIKEY;
            case "http"          -> SecurityScheme.Type.HTTP;
            case "oauth2"        -> SecurityScheme.Type.OAUTH2;
            case "openidconnect" -> SecurityScheme.Type.OPENIDCONNECT;
            default -> throw new IllegalArgumentException(
                    "Unsupported security scheme type: '" + type
                    + "'. Accepted: apiKey, http, oauth2, openIdConnect");
        };
    }

    private static SecurityScheme.In parseSchemeIn(String in) {
        return switch (in.toLowerCase()) {
            case "header" -> SecurityScheme.In.HEADER;
            case "query"  -> SecurityScheme.In.QUERY;
            case "cookie" -> SecurityScheme.In.COOKIE;
            default -> throw new IllegalArgumentException(
                    "Unsupported security scheme 'in' value: '" + in
                    + "'. Accepted: header, query, cookie");
        };
    }
}
