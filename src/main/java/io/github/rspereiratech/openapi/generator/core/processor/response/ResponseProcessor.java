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
package io.github.rspereiratech.openapi.generator.core.processor.response;

import io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/**
 * Contract for building the {@link ApiResponses} block of an OpenAPI {@link Operation}
 * from a Spring MVC controller method.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *     <li>Analyzing the controller method return type</li>
 *     <li>Generating OpenAPI response schemas for success and error responses</li>
 *     <li>Handling generic return types via type-variable resolution</li>
 * </ul>
 * </p>
 *
 * <p>This allows each OpenAPI operation to accurately describe its possible responses.</p>
 *
 * <p>Other aspects of the operation, such as parameters or request bodies,
 * are handled by {@link ParameterProcessor}
 * and {@link RequestBodyProcessor}.</p>
 *
 * @author ruispereira
 * @see ResponseProcessorImpl
 * @see io.swagger.v3.oas.models.responses.ApiResponses
 */
public interface ResponseProcessor {

    /**
     * Builds the full {@link ApiResponses} object for the given controller method
     * and HTTP verb.
     *
     * <p>Delegates to {@link #processResponses(Method, String, Map)} with an empty
     * type-variable map.</p>
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param httpMethod the uppercase HTTP verb (e.g. {@code "GET"}, {@code "POST"})
     * @return the populated {@link ApiResponses}; never {@code null}
     */
    default ApiResponses processResponses(Method method, String httpMethod) {
        return processResponses(method, httpMethod, Map.of());
    }

    /**
     * Builds the full {@link ApiResponses} object for the given controller method
     * and HTTP verb, resolving any generic type parameters in the return type.
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param httpMethod the uppercase HTTP verb
     * @param typeVarMap mapping of type variables to concrete types; may be empty
     * @return the populated {@link ApiResponses}; never {@code null}
     */
    ApiResponses processResponses(Method method, String httpMethod, Map<TypeVariable<?>, Type> typeVarMap);
}
