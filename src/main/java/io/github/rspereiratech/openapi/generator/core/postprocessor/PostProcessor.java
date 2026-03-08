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
package io.github.rspereiratech.openapi.generator.core.postprocessor;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Post-processing step applied to the fully-built {@link OpenAPI} model
 * before it is serialised to disk.
 *
 * <p>Implementations are invoked in order after all controllers have been
 * processed and all component schemas have been merged.
 *
 * @author ruispereira
 */
@FunctionalInterface
public interface PostProcessor {

    /**
     * Applies this post-processing step to the given model.
     *
     * @param openAPI the OpenAPI model to transform; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    void process(OpenAPI openAPI);
}
