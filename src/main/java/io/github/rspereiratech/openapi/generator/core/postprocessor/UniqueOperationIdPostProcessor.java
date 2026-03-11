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
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Guarantees that every {@link Operation#getOperationId()} in the document is unique.
 *
 * <p>Collisions arise naturally when multiple controllers define handler methods with
 * the same name (e.g. {@code getById} in {@code TenantController} and
 * {@code AgentController}). Without disambiguation, some code-generation tools that
 * consume the OpenAPI document would either fail or silently overwrite one of the
 * conflicting operations.
 *
 * <p><b>Disambiguation strategy</b> (matches SpringDoc's default behaviour):
 * <ul>
 *   <li>The <em>first</em> occurrence of a given {@code operationId} is left unchanged.</li>
 *   <li>Each subsequent occurrence is suffixed with {@code _N}, where N starts at 1 and
 *       increments for every additional duplicate (e.g. {@code getById}, {@code getById_1},
 *       {@code getById_2}, …).</li>
 * </ul>
 *
 * <p>Operations are visited in path-declaration order, which is the order controllers
 * are processed and paths are added to {@link io.swagger.v3.oas.models.Paths}.
 *
 * @author ruispereira
 */
public class UniqueOperationIdPostProcessor implements PostProcessor {

    /**
     * Traverses every path item in the document and disambiguates duplicate
     * {@code operationId} values by appending {@code _N} suffixes.
     *
     * <p>The first occurrence of any given {@code operationId} is left unchanged.
     * Each subsequent duplicate is suffixed with {@code _1}, {@code _2}, etc.
     * Operations without an {@code operationId} are skipped.</p>
     *
     * @param openAPI the OpenAPI model to update in-place; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        if (openAPI.getPaths() == null) return;

        Map<String, Integer> seen = new HashMap<>();

        for (PathItem pathItem : openAPI.getPaths().values()) {
            for (Operation op : pathItem.readOperations()) {
                if (op == null || op.getOperationId() == null) continue;

                String base  = op.getOperationId();
                int    count = seen.getOrDefault(base, 0);
                if (count > 0) {
                    op.setOperationId(base + "_" + count);
                }
                seen.put(base, count + 1);
            }
        }
    }
}
