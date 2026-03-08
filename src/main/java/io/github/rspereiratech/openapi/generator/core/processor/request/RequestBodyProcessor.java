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
package io.github.rspereiratech.openapi.generator.core.processor.request;

import io.swagger.v3.oas.models.parameters.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;

/**
 * Contract for building an OpenAPI {@link RequestBody} from a Spring MVC
 * controller method parameter annotated with {@code @RequestBody}.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *     <li>Locating the {@code @RequestBody} parameter in the method signature</li>
 *     <li>Resolving its type and generating the corresponding OpenAPI schema</li>
 *     <li>Returning the constructed {@link RequestBody} wrapped in an {@link Optional}</li>
 * </ul>
 * </p>
 *
 * <p>Request-body parameters are handled separately from path, query,
 * header, and cookie parameters, which are processed by
 * {@link io.github.rspereiratech.openapi.generator.core.processor.parameter.ParameterProcessor}.</p>
 *
 * @author ruispereira
 * @see RequestBodyProcessorImpl
 * @see io.swagger.v3.oas.models.parameters.RequestBody
 */
public interface RequestBodyProcessor {
    /**
     * Inspects the given controller method for a {@code @RequestBody} parameter.
     *
     * <p>Delegates to {@link #processRequestBody(Method, Map)} with an empty type-variable map.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return an {@link Optional} containing the {@link RequestBody} if found,
     *         or {@link Optional#empty()} if the method has no {@code @RequestBody} parameter
     */
    default Optional<RequestBody> processRequestBody(Method method) {
        return processRequestBody(method, Map.of());
    }

    /**
     * Variant that substitutes {@link TypeVariable}s in the request-body parameter type
     * using the supplied {@code typeVarMap} before resolving the schema.
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param typeVarMap mapping of type variables to concrete types; may be empty
     * @return an {@link Optional} containing the {@link RequestBody} if found,
     *         or {@link Optional#empty()} if none is present
     */
    Optional<RequestBody> processRequestBody(Method method, Map<TypeVariable<?>, Type> typeVarMap);
}
