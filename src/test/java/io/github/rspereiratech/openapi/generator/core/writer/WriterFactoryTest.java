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
import io.github.rspereiratech.openapi.generator.core.config.OutputFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Unit tests for {@link io.github.rspereiratech.openapi.generator.core.writer.WriterFactory}.
 *
 * <p>Verifies that the factory returns the correct {@link io.github.rspereiratech.openapi.generator.core.writer.OpenApiWriter}
 * implementation for each {@link io.github.rspereiratech.openapi.generator.core.config.OutputFormat},
 * and that each call produces a new instance.
 */
class WriterFactoryTest {

    private static GeneratorConfig config(OutputFormat format, boolean prettyPrint) {
        return GeneratorConfig.builder()
                .basePackage("com.example")
                .outputFormat(format)
                .prettyPrint(prettyPrint)
                .build();
    }

    @Test
    void yaml_returnsYamlWriter() {
        OpenApiWriter writer = WriterFactory.create(config(OutputFormat.YAML, true));
        assertNotNull(writer);
        assertInstanceOf(YamlWriter.class, writer);
    }

    @Test
    void json_returnsJsonWriter() {
        OpenApiWriter writer = WriterFactory.create(config(OutputFormat.JSON, true));
        assertNotNull(writer);
        assertInstanceOf(JsonWriter.class, writer);
    }

    @Test
    void yaml_prettyFalse_returnsYamlWriter() {
        OpenApiWriter writer = WriterFactory.create(config(OutputFormat.YAML, false));
        assertInstanceOf(YamlWriter.class, writer);
    }

    @Test
    void json_prettyFalse_returnsJsonWriter() {
        OpenApiWriter writer = WriterFactory.create(config(OutputFormat.JSON, false));
        assertInstanceOf(JsonWriter.class, writer);
    }

    @Test
    void eachCallReturnsNewInstance() {
        GeneratorConfig cfg = config(OutputFormat.YAML, true);
        assertNotSame(WriterFactory.create(cfg), WriterFactory.create(cfg));
    }
}
