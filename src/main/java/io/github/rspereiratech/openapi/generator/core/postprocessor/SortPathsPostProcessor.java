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
import io.swagger.v3.oas.models.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sorts the paths in the generated OpenAPI spec alphabetically.
 *
 * <p>Without this, path order depends on the order controllers and methods
 * are returned by reflection, which is not guaranteed to be deterministic
 * across JVMs, machines, or build environments.
 *
 * @author ruispereira
 */
public class SortPathsPostProcessor implements PostProcessor {

    @Override
    public void process(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.isEmpty()) return;

        List<String> sorted = new ArrayList<>(paths.keySet());
        Collections.sort(sorted);

        Paths ordered = new Paths();
        sorted.forEach(path -> ordered.addPathItem(path, paths.get(path)));
        openAPI.setPaths(ordered);
    }
}
