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

import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory for {@link OpenApiWriter} instances.
 *
 * <p>Selects the appropriate implementation based on the
 * {@link io.github.rspereiratech.openapi.generator.core.config.OutputFormat}
 * and {@code prettyPrint} flag in the given {@link GeneratorConfig}.
 *
 * @author ruispereira
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WriterFactory {
    /**
     * Creates an {@link OpenApiWriter} for the format declared in {@code config}.
     *
     * @param config generation configuration; must not be {@code null}
     * @return a writer appropriate for the configured output format
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public static OpenApiWriter create(GeneratorConfig config) {
        Preconditions.checkNotNull(config, "'config' must not be null");

        return switch (config.outputFormat()) {
            case YAML -> new YamlWriter(config.prettyPrint());
            case JSON -> new JsonWriter(config.prettyPrint());
        };
    }
}
