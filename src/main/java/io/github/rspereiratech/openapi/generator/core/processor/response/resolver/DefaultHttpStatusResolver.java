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

import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import com.google.common.base.Preconditions;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Default {@link HttpStatusResolver} implementation.
 *
 * <p>Resolves HTTP status codes using the standard Spring MVC / SpringDoc rules:
 * <ol>
 *   <li>{@code @ResponseStatus} annotation wins if present.</li>
 *   <li>POST → 201.</li>
 *   <li>void return type → 204.</li>
 *   <li>Otherwise → 200.</li>
 * </ol>
 *
 * <p>Status-code descriptions are delegated to {@link HttpStatus#resolve(int)}, which
 * covers every code defined by the Spring {@link HttpStatus} enum (e.g. 200 → "OK",
 * 418 → "I'm a Teapot"). Codes that are not recognised by Spring fall back to
 * {@code "Response"}.
 *
 * @author ruispereira
 */
public class DefaultHttpStatusResolver implements HttpStatusResolver {

    @Override
    public String resolveCode(Method method, String httpMethod, Type returnType) {
        Preconditions.checkNotNull(method, "'method' must not be null");
        Preconditions.checkNotNull(httpMethod, "'httpMethod' must not be null");
        Preconditions.checkNotNull(returnType, "'returnType' must not be null");
        Preconditions.checkArgument(!httpMethod.isBlank(),  "'httpMethod' must not be blank");

        String statusFromAnnotation = extractStatusFromAnnotation(method);
        if (statusFromAnnotation != null) {
            return statusFromAnnotation;
        }

        // Default rule: POST → 201 Created
        if (HttpMethod.POST.matches(httpMethod)) {
            return String.valueOf(HttpStatus.CREATED.value());
        }

        // Void return type → 204 No Content
        if (TypeUtils.isVoid(TypeUtils.unwrapType(returnType))) {
            return String.valueOf(HttpStatus.NO_CONTENT.value());
        }

        // Fallback → 200 OK
        return String.valueOf(HttpStatus.OK.value());
    }

    @Override
    public String describeCode(String statusCode) {
        try {
            HttpStatus status = HttpStatus.resolve(Integer.parseInt(statusCode));
            return status != null ? status.getReasonPhrase() : "Response";
        } catch (NumberFormatException e) {
            return "Response";
        }
    }

    /**
     * Extracts the HTTP status code from a @ResponseStatus annotation, if present.
     */
    private String extractStatusFromAnnotation(Method method) {
        for (Annotation annotation : AnnotationUtils.getAllAnnotations(method)) {
            if (annotation instanceof ResponseStatus rs) {
                // Spring 5+: both `value` and `code` may be present; use non-500 if available
                HttpStatus status = (rs.value() != HttpStatus.INTERNAL_SERVER_ERROR)
                        ? rs.value()
                        : rs.code();
                return String.valueOf(status.value());
            }
        }
        return null;
    }
}
