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
package io.github.rspereiratech.openapi.generator.core.processor.response.resolver;

import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessorImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Strategy interface for HTTP status-code resolution used by {@link ResponseProcessorImpl}.
 *
 * <p>Separates two concerns from {@link ResponseProcessorImpl}:
 * <ul>
 *   <li><em>Code resolution</em> — deriving the numeric status code from the controller
 *       method, HTTP verb, and return type.</li>
 *   <li><em>Code description</em> — mapping a numeric code to a human-readable reason phrase.</li>
 * </ul>
 * </p>
 *
 * <p>The default implementation is {@link DefaultHttpStatusResolver}.</p>
 *
 * <p>Implementations can be substituted to customize status code derivation
 * or descriptions according to project conventions or business rules.</p>
 *
 * @author ruispereira
 * @see ResponseProcessorImpl
 * @see DefaultHttpStatusResolver
 */
public interface HttpStatusResolver {

    /**
     * Determines the HTTP status code for the default (inferred) response
     * of a controller method.
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param httpMethod uppercase HTTP verb (e.g. {@code "GET"}, {@code "POST"});
     *                   must not be {@code null} or blank
     * @param returnType the generic return type of the method; must not be {@code null}
     * @return the HTTP status code as a string (e.g. {@code "200"}, {@code "201"})
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code httpMethod} is blank
     */
    String resolveCode(Method method, String httpMethod, Type returnType);

    /**
     * Returns a human-readable description (reason phrase) for the given HTTP status code.
     *
     * <p>The special value {@code "default"} maps to the literal string
     * {@code "default response"} as required by the OpenAPI specification.
     * All other values are treated as numeric codes; unrecognised or non-numeric
     * values fall back to {@code "Response"}.</p>
     *
     * @param statusCode the HTTP status code as a string (e.g. {@code "200"}, {@code "404"}),
     *                   or the literal {@code "default"}; must not be {@code null}
     * @return the standard reason phrase (e.g. {@code "OK"}, {@code "Created"}),
     *         {@code "default response"} for {@code "default"},
     *         or {@code "Response"} for unrecognised or non-numeric codes
     * @throws NullPointerException if {@code statusCode} is {@code null}
     */
    String describeCode(String statusCode);
}
