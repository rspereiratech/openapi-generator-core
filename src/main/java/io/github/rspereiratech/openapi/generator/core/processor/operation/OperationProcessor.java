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
package io.github.rspereiratech.openapi.generator.core.processor.operation;

import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.swagger.v3.oas.models.Operation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Contract for transforming a single Spring MVC controller {@link Method}
 * into an OpenAPI {@link Operation}.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *     <li>Resolving the HTTP method (GET, POST, etc.)</li>
 *     <li>Extracting parameters via {@link ParameterProcessor}</li>
 *     <li>Generating request and response schemas</li>
 *     <li>Assigning tags inherited from the owning controller</li>
 * </ul>
 * </p>
 *
 * <p>This allows the OpenAPI model to accurately represent the operations
 * declared by the controller.</p>
 *
 * @author ruispereira
 * @see OperationProcessorImpl
 * @see io.swagger.v3.oas.models.Operation
 */
public interface OperationProcessor {

    /**
     * Builds an {@link Operation} object from a controller method.
     *
     * <p>Delegates to {@link #buildOperation(Method, String, List, Map)} with an empty type-variable map.</p>
     *
     * @param method     the controller method to process; must not be {@code null}
     * @param httpMethod the uppercase HTTP verb resolved by the caller (e.g. {@code "GET"})
     * @param tags       tags inherited from the owning controller; may be empty
     * @return the populated {@link Operation} representing the controller method
     */
    default Operation buildOperation(Method method, String httpMethod, Collection<String> tags) {
        return buildOperation(method, httpMethod, tags, Map.of());
    }

    /**
     * Variant that also accepts a type-variable map for resolving generic type parameters
     * (e.g. {@code T} in {@code ResponseEntity<T>}) to their concrete types before schema generation.
     *
     * <p>Delegates to {@link #buildOperation(Method, String, Collection, Map, List)} with empty mapping headers.</p>
     *
     * @param method     the controller method to process; must not be {@code null}
     * @param httpMethod the uppercase HTTP verb (e.g. {@code "POST"})
     * @param tags       tags inherited from the owning controller; may be empty
     * @param typeVarMap mapping of type variables to concrete types; may be empty
     * @return the populated {@link Operation} representing the controller method
     */
    default Operation buildOperation(Method method, String httpMethod, Collection<String> tags,
                                     Map<TypeVariable<?>, Type> typeVarMap) {
        return buildOperation(method, httpMethod, tags, typeVarMap, List.of());
    }

    /**
     * Variant that also accepts header expressions from the Spring MVC mapping annotation's
     * {@code headers} attribute (e.g. {@code @PostMapping(headers = "x-dashboard-name=pcs")}).
     *
     * <p>Header expressions are forwarded to the parameter processor to generate
     * additional {@code in: header} parameters in the OpenAPI operation.</p>
     *
     * @param method         the controller method to process; must not be {@code null}
     * @param httpMethod     the uppercase HTTP verb (e.g. {@code "POST"})
     * @param tags           tags inherited from the owning controller; may be empty
     * @param typeVarMap     mapping of type variables to concrete types; may be empty
     * @param mappingHeaders header expressions from the mapping annotation; may be empty
     * @return the populated {@link Operation} representing the controller method
     */
    Operation buildOperation(Method method, String httpMethod, Collection<String> tags,
                             Map<TypeVariable<?>, Type> typeVarMap, List<String> mappingHeaders);
}
