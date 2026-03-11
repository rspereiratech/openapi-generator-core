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
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PostProcessor} that sorts the {@code paths} section and the
 * {@code responses} map of every operation alphabetically, producing a
 * deterministic spec regardless of controller discovery order or JVM behaviour.
 *
 * <h3>What is sorted</h3>
 * <ul>
 *   <li><b>Paths</b> — top-level path strings sorted alphabetically
 *       (e.g. {@code /agents} before {@code /queues}).</li>
 *   <li><b>Responses</b> — HTTP status-code keys within each operation sorted
 *       lexicographically (e.g. {@code "200"} before {@code "404"}).</li>
 * </ul>
 *
 * <p>Without explicit sorting, both orderings depend on the sequence in which
 * controllers and methods are returned by reflection — which is not guaranteed
 * to be stable across JVMs, operating systems, or build environments. This
 * processor ensures that every generation run produces the same output, making
 * spec diffs readable and CI comparisons reliable.
 *
 * <p>Sorting is enabled by passing {@code sortOutput = true} to the constructor.
 * When disabled, {@link #process(OpenAPI)} is a no-op, allowing the processor
 * to always be present in the pipeline without conditional inclusion logic in
 * the factory.
 *
 * <p>Null or empty collections are silently skipped when sorting is enabled.
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig#sortOutput()
 */
public class SortSpecPostProcessor implements PostProcessor {

    /** Whether sorting is active for this processor instance. */
    private final boolean sortOutput;

    /**
     * Creates a new processor.
     *
     * @param sortOutput {@code true} to sort paths and responses alphabetically;
     *                   {@code false} to make {@link #process(OpenAPI)} a no-op
     */
    public SortSpecPostProcessor(boolean sortOutput) {
        this.sortOutput = sortOutput;
    }

    /**
     * Sorts the {@code paths} block and the {@code responses} map of every
     * operation when {@code sortOutput} is {@code true}. When {@code sortOutput}
     * is {@code false} this method returns immediately without modifying the model.
     *
     * @param openAPI the OpenAPI model to process; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        if (!sortOutput) return;
        sortPaths(openAPI);
        sortResponses(openAPI);
    }

    /**
     * Replaces the {@code paths} block with a new one whose entries are sorted
     * alphabetically by path string. All path items and their operations are
     * preserved unchanged.
     */
    private void sortPaths(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.isEmpty()) return;

        List<String> sorted = new ArrayList<>(paths.keySet());
        sorted.sort(null);

        Paths ordered = new Paths();
        sorted.forEach(path -> ordered.addPathItem(path, paths.get(path)));
        openAPI.setPaths(ordered);
    }

    /**
     * Replaces the {@code responses} map of every operation with a new one
     * whose entries are sorted lexicographically by HTTP status-code string.
     * All response objects are preserved unchanged.
     */
    private void sortResponses(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null) return;

        paths.values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(op -> op.getResponses() != null && !op.getResponses().isEmpty())
                .forEach(op -> {
                    ApiResponses responses = op.getResponses();
                    List<String> sorted = new ArrayList<>(responses.keySet());
                    sorted.sort(null);

                    ApiResponses ordered = new ApiResponses();
                    sorted.forEach(code -> ordered.addApiResponse(code, responses.get(code)));
                    op.setResponses(ordered);
                });
    }
}
