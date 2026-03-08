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

import com.google.common.base.Preconditions;

/**
 * Configuration for a single OpenAPI
 * <a href="https://swagger.io/specification/#security-scheme-object">Security Scheme</a> entry.
 *
 * <p>Maps to one entry under {@code components/securitySchemes} in the generated specification,
 * and automatically adds a corresponding entry to the root {@code security} list.</p>
 *
 * <h3>Supported scheme types</h3>
 * <table>
 *   <tr><th>type</th><th>Required fields</th><th>Optional fields</th></tr>
 *   <tr><td>{@code http}</td><td>{@code scheme} (e.g. {@code bearer})</td><td>{@code bearerFormat}, {@code description}</td></tr>
 *   <tr><td>{@code apiKey}</td><td>{@code in}, {@code parameterName}</td><td>{@code description}</td></tr>
 *   <tr><td>{@code openIdConnect}</td><td>{@code openIdConnectUrl}</td><td>{@code description}</td></tr>
 *   <tr><td>{@code oauth2}</td><td>(flows configured separately)</td><td>{@code description}</td></tr>
 * </table>
 *
 * <h3>Example — Bearer JWT (most common)</h3>
 * <pre>{@code
 * SecuritySchemeConfig.of("bearerAuth", "http")
 *     .withScheme("bearer")
 *     .withBearerFormat("JWT")
 * }</pre>
 *
 * @param name             the key used in {@code components/securitySchemes} and the {@code security} list;
 *                         must not be {@code null} or blank
 * @param type             the scheme type: {@code http}, {@code apiKey}, {@code oauth2}, or
 *                         {@code openIdConnect}; must not be {@code null} or blank
 * @param scheme           the HTTP authorisation scheme (e.g. {@code bearer}, {@code basic});
 *                         used when {@code type} is {@code http}; may be {@code null}
 * @param bearerFormat     hint for the token format (e.g. {@code JWT});
 *                         used when {@code type=http} and {@code scheme=bearer}; may be {@code null}
 * @param description      human-readable description of the security scheme; may be {@code null}
 * @param in               location of the API key ({@code header}, {@code query}, or {@code cookie});
 *                         used when {@code type} is {@code apiKey}; may be {@code null}
 * @param parameterName    name of the header or query parameter that carries the API key;
 *                         used when {@code type} is {@code apiKey}; may be {@code null}
 * @param openIdConnectUrl well-known URL for the OpenID Connect configuration;
 *                         used when {@code type} is {@code openIdConnect}; may be {@code null}
 *
 * @author ruispereira
 */
public record SecuritySchemeConfig(
        String name,
        String type,
        String scheme,
        String bearerFormat,
        String description,
        String in,
        String parameterName,
        String openIdConnectUrl
) {
    /**
     * Compact constructor — validates mandatory fields.
     *
     * @throws NullPointerException     if {@code name} or {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code type} is blank
     */
    public SecuritySchemeConfig {
        Preconditions.checkNotNull(name, "SecuritySchemeConfig: name must not be null");
        Preconditions.checkArgument(!name.isBlank(), "SecuritySchemeConfig: name must not be blank");
        Preconditions.checkNotNull(type, "SecuritySchemeConfig: type must not be null");
        Preconditions.checkArgument(!type.isBlank(), "SecuritySchemeConfig: type must not be blank");
    }

    /**
     * Creates a minimal {@link SecuritySchemeConfig} with just a name and type.
     * All other fields default to {@code null}.
     *
     * @param name the scheme name (key in {@code components/securitySchemes})
     * @param type the scheme type ({@code http}, {@code apiKey}, etc.)
     * @return a new {@link SecuritySchemeConfig}
     * @throws NullPointerException     if {@code name} or {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code type} is blank
     */
    public static SecuritySchemeConfig of(String name, String type) {
        return new SecuritySchemeConfig(name, type, null, null, null, null, null, null);
    }

    /**
     * Returns a copy of this config with the given HTTP {@code scheme} set.
     *
     * @param scheme e.g. {@code "bearer"} or {@code "basic"}
     * @return a new record with the scheme applied
     */
    public SecuritySchemeConfig withScheme(String scheme) {
        return new SecuritySchemeConfig(name, type, scheme, bearerFormat, description, in, parameterName, openIdConnectUrl);
    }

    /**
     * Returns a copy of this config with the given {@code bearerFormat} set.
     *
     * @param bearerFormat e.g. {@code "JWT"}
     * @return a new record with the bearerFormat applied
     */
    public SecuritySchemeConfig withBearerFormat(String bearerFormat) {
        return new SecuritySchemeConfig(name, type, scheme, bearerFormat, description, in, parameterName, openIdConnectUrl);
    }

    /**
     * Returns a copy of this config with the given {@code description} set.
     *
     * @param description human-readable description
     * @return a new record with the description applied
     */
    public SecuritySchemeConfig withDescription(String description) {
        return new SecuritySchemeConfig(name, type, scheme, bearerFormat, description, in, parameterName, openIdConnectUrl);
    }
}
