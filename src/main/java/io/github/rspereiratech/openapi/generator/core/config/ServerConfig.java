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
 * Configuration for a single OpenAPI server entry (url + description).
  *
 * @author ruispereira
 */
public record ServerConfig(String url, String description) {

    public ServerConfig {
        Preconditions.checkNotNull(url, "Server url must not be null");
        Preconditions.checkArgument(!url.isBlank(), "Server url must not be blank");
        description = description != null ? description : "";
    }

    /**
     * Creates a server with only a URL.
     *
     * @param url the server URL; must not be {@code null} or blank
     * @return a new {@link ServerConfig}
     * @throws NullPointerException     if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     */
    public static ServerConfig of(String url) {
        return new ServerConfig(url, null);
    }

    /**
     * Creates a server with a URL and a description.
     *
     * @param url         the server URL; must not be {@code null} or blank
     * @param description optional human-readable description; may be {@code null}
     * @return a new {@link ServerConfig}
     * @throws NullPointerException     if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     */
    public static ServerConfig of(String url, String description) {
        return new ServerConfig(url, description);
    }
}
