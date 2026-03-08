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

/**
 * Supported serialisation formats for the generated OpenAPI specification.
 *
 * <p>Selected via {@link GeneratorConfig.Builder#outputFormat(OutputFormat)}
 * and honoured by {@link io.github.rspereiratech.openapi.generator.core.OpenApiGenerator}
 * when choosing the appropriate {@link io.github.rspereiratech.openapi.generator.core.writer.OpenApiWriter}
 * implementation.
 *
 * @author ruispereira
 */
public enum OutputFormat {

    /**
     * YAML format (default).
     * Human-readable; recommended for source-controlled specs and most tooling.
     */
    YAML,

    /**
     * JSON format.
     * Useful for tooling that only consumes JSON, or when embedding the spec
     * inside an existing JSON pipeline.
     */
    JSON
}
