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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for serialising an {@link OpenAPI} model to a file.
 *
 * <p>Implementations may produce YAML, JSON, or any other format suitable for
 * OpenAPI specifications. This allows pluggable output formats without changing
 * the generator logic.</p>
 *
 * <p>Typical implementations include {@code JsonOpenApiWriter} and
 * {@code YamlOpenApiWriter}.</p>
 *
 * <p>The interface ensures that the target file and its parent directories
 * are handled appropriately.</p>
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.OpenApiGeneratorImpl
 */
public interface OpenApiWriter {

    /**
     * Serialises the given OpenAPI model and writes it to the specified file path.
     *
     * @param openAPI    the model to serialise; must not be {@code null}
     * @param outputPath target file path; parent directories will be created if absent; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} or {@code outputPath} is {@code null}
     * @throws IOException          if the file cannot be written
     */
    void write(OpenAPI openAPI, Path outputPath) throws IOException;
}
