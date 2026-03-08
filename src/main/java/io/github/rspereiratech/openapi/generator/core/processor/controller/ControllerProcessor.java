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
package io.github.rspereiratech.openapi.generator.core.processor.controller;

import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Contract for processing a Spring MVC controller class and populating
 * the {@link OpenAPI} model with the paths, operations, and parameters
 * declared in the controller.
 *
 * <p>Implementations should discover all request mappings (e.g., via
 * {@code @RequestMapping}, {@code @GetMapping}, {@code @PostMapping}, etc.)
 * and merge them into the provided {@link OpenAPI} model.
 * Parameters of each operation should be extracted via a {@link ParameterProcessor}.</p>
 *
 * @author ruispereira
 * @see ControllerProcessorImpl
 * @see ParameterProcessor
 * @see io.swagger.v3.oas.models.OpenAPI
 */
public interface ControllerProcessor {

    /**
     * Analyses the given {@code controllerClass} and merges the discovered
     * paths and operations into the supplied {@code openAPI} model.
     *
     * <p>All controller methods are inspected, and their request mappings
     * and associated parameters are converted into OpenAPI {@link io.swagger.v3.oas.models.parameters.Parameter}
     * objects where applicable.</p>
     *
     * @param controllerClass Spring MVC controller class; must not be {@code null}
     * @param openAPI         target OpenAPI model to populate; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    void process(Class<?> controllerClass, OpenAPI openAPI);
}
