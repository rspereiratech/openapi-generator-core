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
 * {@link PostProcessor} that sorts the {@code paths} section of the generated
 * OpenAPI spec alphabetically by path string.
 *
 * <p>Without explicit sorting, path order depends on the order in which
 * controllers and methods are returned by reflection — which is not guaranteed
 * to be stable across JVMs, operating systems, or build environments. This
 * processor ensures that every generation run produces the same path ordering,
 * making spec diffs readable and CI comparisons reliable.
 *
 * <p>This processor is included in the pipeline only when
 * {@link io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig#sortOutput()}
 * is {@code true}.
 *
 * <p>Null or empty {@code paths} blocks are silently skipped.
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig#sortOutput()
 */
public class SortPathsPostProcessor implements PostProcessor {

    /**
     * Replaces the {@code paths} block with a new one whose entries are
     * sorted alphabetically by path string. All path items and their
     * operations are preserved unchanged.
     *
     * @param openAPI the OpenAPI model to sort; must not be {@code null}
     */
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
