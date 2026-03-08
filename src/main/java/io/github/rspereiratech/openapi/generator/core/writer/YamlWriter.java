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

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import io.github.rspereiratech.openapi.generator.core.utils.FileUtils;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link OpenApiWriter} implementation that produces a YAML file.
 *
 * <p>Serialisation is delegated to {@link io.swagger.v3.core.util.Yaml}
 * (bundled with {@code swagger-core}), the same serialiser used by SpringDoc
 * at runtime.  This guarantees spec-compliant output including proper handling
 * of {@code $ref}, {@code example}, and {@code nullable} fields.
 *
 * <p>The output file's parent directories are created automatically if they do
 * not yet exist, so callers do not need to pre-create the target directory.
  *
 * @author ruispereira
 */
@Slf4j
public class YamlWriter implements OpenApiWriter {
    /** Whether to use indented (human-readable) YAML output. */
    private final boolean prettyPrint;

    /** Creates a writer with pretty-printing enabled. */
    public YamlWriter() {
        this(true);
    }

    /**
     * Creates a writer with configurable pretty-printing.
     *
     * @param prettyPrint {@code true} to use {@link io.swagger.v3.core.util.Yaml#pretty(Object)}
     *                    (human-friendly indentation); {@code false} for compact output
     */
    public YamlWriter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    /**
     * Serialises the {@link OpenAPI} model to a YAML file at {@code outputPath}.
     *
     * <p>Parent directories of {@code outputPath} are created if they do not already exist.
     *
     * @param openAPI    the OpenAPI model to serialise
     * @param outputPath target file path; parent directories are created if absent
     * @throws NullPointerException if {@code openAPI} or {@code outputPath} is {@code null}
     * @throws IOException          if the directories cannot be created or the file cannot be written
     */
    @Override
    public void write(OpenAPI openAPI, Path outputPath) throws IOException {
        Preconditions.checkNotNull(openAPI,    "openAPI must not be null");
        Preconditions.checkNotNull(outputPath, "outputPath must not be null");

        // create dirs
        FileUtils.createParentDirectories(outputPath);

        String yaml = prettyPrint
                ? Yaml.pretty(openAPI)
                : Yaml.mapper().writeValueAsString(openAPI);

        Files.writeString(outputPath, yaml, StandardCharsets.UTF_8);
        log.info("OpenAPI spec written to: {}", outputPath.toAbsolutePath());
    }

}
