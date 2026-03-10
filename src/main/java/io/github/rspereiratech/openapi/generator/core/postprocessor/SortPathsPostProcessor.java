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

import com.google.common.base.Preconditions;
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
 * <p>Sorting is enabled by passing {@code sortOutput = true} to the constructor.
 * When disabled, {@link #process(OpenAPI)} is a no-op and the paths block is
 * left unchanged. This allows the processor to always be present in the pipeline
 * without conditional inclusion logic in the factory.
 *
 * <p>Null or empty {@code paths} blocks are silently skipped when sorting is enabled.
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig#sortOutput()
 */
public class SortPathsPostProcessor implements PostProcessor {

    /** Whether path sorting is active for this processor instance. */
    private final boolean sortOutput;

    /**
     * Creates a new processor.
     *
     * @param sortOutput {@code true} to sort paths alphabetically;
     *                   {@code false} to make {@link #process(OpenAPI)} a no-op
     */
    public SortPathsPostProcessor(boolean sortOutput) {
        this.sortOutput = sortOutput;
    }

    /**
     * Replaces the {@code paths} block with a new one whose entries are sorted
     * alphabetically by path string when {@code sortOutput} is {@code true}.
     * When {@code sortOutput} is {@code false} this method returns immediately
     * without modifying the model. All path items and their operations are
     * preserved unchanged.
     *
     * @param openAPI the OpenAPI model to process; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        if (!sortOutput) return;
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.isEmpty()) return;

        List<String> sorted = new ArrayList<>(paths.keySet());
        Collections.sort(sorted);

        Paths ordered = new Paths();
        sorted.forEach(path -> ordered.addPathItem(path, paths.get(path)));
        openAPI.setPaths(ordered);
    }
}
