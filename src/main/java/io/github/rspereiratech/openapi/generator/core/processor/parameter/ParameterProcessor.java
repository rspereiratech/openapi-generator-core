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
package io.github.rspereiratech.openapi.generator.core.processor.parameter;

import io.swagger.v3.oas.models.parameters.Parameter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

/**
 * Contract for extracting OpenAPI {@link Parameter} objects from
 * Spring MVC controller methods.
 *
 * <p>This interface covers non-body parameters, including:
 * <ul>
 *     <li>Path parameters ({@code @PathVariable})</li>
 *     <li>Query parameters ({@code @RequestParam})</li>
 *     <li>Header parameters ({@code @RequestHeader})</li>
 *     <li>Cookie parameters ({@code @CookieValue})</li>
 * </ul>
 * Request body parameters ({@code @RequestBody}) are handled separately by
 * {@link io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor}.
 * </p>
 *
 * <p>Implementations should produce {@link Parameter} objects suitable
 * for inclusion in OpenAPI specifications. They may also resolve generic
 * type parameters using {@link TypeVariable} mappings.</p>
 *
 * @author ruispereira
 * @see ParameterProcessorImpl
 * @see io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor
 */
public interface ParameterProcessor {
    /**
     * Extracts all non-body parameters from the given controller method.
     *
     * <p>
     * Non-body parameters include those annotated with Spring MVC annotations such as:
     * {@code @PathVariable}, {@code @RequestParam}, {@code @RequestHeader}, {@code @CookieValue}.
     * {@code @RequestBody} parameters are intentionally excluded.
     * </p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return a list of OpenAPI {@link Parameter} objects representing the method parameters;
     *         may be empty but never {@code null}
     */
    default List<Parameter> processParameters(Method method) {
        return processParameters(method, Map.of());
    }

    /**
     * Extracts all non-body parameters from the given controller method,
     * resolving any {@link TypeVariable}s in parameter types using the supplied mapping.
     *
     * <p>
     * Non-body parameters include those annotated with Spring MVC annotations such as:
     * {@code @PathVariable}, {@code @RequestParam}, {@code @RequestHeader}, {@code @CookieValue}.
     * {@code @RequestBody} parameters are intentionally excluded.
     * </p>
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param typeVarMap mapping of type variables to concrete types for generic controllers; must not be {@code null}
     * @return a list of OpenAPI {@link Parameter} objects representing the method parameters;
     *         may be empty but never {@code null}
     */
    List<Parameter> processParameters(Method method, Map<TypeVariable<?>, Type> typeVarMap);
}
