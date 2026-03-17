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

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Default {@link RequestBodyProcessor} implementation.
 *
 * <p>Builds an OpenAPI {@link RequestBody} from a method annotated with
 * {@code @RequestBody} (Spring MVC) or {@code @io.swagger.v3.oas.annotations.parameters.RequestBody}.
 *
 * @author ruispereira
 */
@Slf4j
public class RequestBodyProcessorImpl implements RequestBodyProcessor {

    /** Default media type for request bodies — mirrors {@code springdoc.default-consumes-media-type}. */
    private static final String FALLBACK_CONSUMES_MEDIA_TYPE = "application/json";

    /** Shared {@link SchemaProcessor} used to derive request-body schemas from parameter types. */
    private final SchemaProcessor schemaProcessor;
    /** Default media type used when no {@code consumes} attribute is declared on the handler method. */
    private final String          defaultConsumesMediaType;

    /**
     * Creates a new {@code RequestBodyProcessorImpl} with a configurable default consumes media type.
     *
     * @param schemaProcessor        the shared schema processor; must not be {@code null}
     * @param defaultConsumesMediaType the default media type when no {@code consumes} is declared;
     *                               {@code null} or blank falls back to
     *                               {@value #FALLBACK_CONSUMES_MEDIA_TYPE}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    public RequestBodyProcessorImpl(SchemaProcessor schemaProcessor, String defaultConsumesMediaType) {
        this.schemaProcessor         = Preconditions.checkNotNull(schemaProcessor, "'schemaProcessor' must not be null");
        this.defaultConsumesMediaType = (defaultConsumesMediaType == null || defaultConsumesMediaType.isBlank())
                ? FALLBACK_CONSUMES_MEDIA_TYPE : defaultConsumesMediaType;
    }

    /**
     * Carries the metadata extracted from a Swagger
     * {@code @io.swagger.v3.oas.annotations.parameters.RequestBody} annotation.
     *
     * @param schema      the schema override resolved from {@code content[0].schema.implementation},
     *                    or {@code null} when not declared
     * @param description the value of the {@code description} attribute, or {@code ""} when absent
     */
    private record SwaggerBodyHints(Schema<?> schema, String description) {}

    @Override
    @SuppressWarnings("java:S1872")
    public Optional<RequestBody> processRequestBody(Method method, Map<TypeVariable<?>, Type> typeVarMap) {
        Preconditions.checkNotNull(method,     "'method' must not be null");
        Preconditions.checkNotNull(typeVarMap, "'typeVarMap' must not be null");

        SwaggerBodyHints hints = resolveSwaggerBodyHints(method);
        Parameter[] params = method.getParameters();

        return IntStream.range(0, params.length)
                .mapToObj(i -> Arrays.stream(AnnotationUtils.getAllParameterAnnotations(method, i))
                        .filter(ann -> "RequestBody".equals(ann.annotationType().getSimpleName()))
                        .findFirst()
                        .map(ann -> {
                            boolean  required = getRequired(ann);
                            Schema<?> schema  = hints.schema() != null
                                    ? hints.schema()
                                    : schemaProcessor.toSchema(params[i].getParameterizedType(), typeVarMap);
                            return buildRequestBody(schema, required, hints.description(), method);
                        }))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Resolves Swagger {@code @RequestBody} metadata for the given method using a two-step
     * priority chain:
     *
     * <ol>
     *   <li><b>Inline annotation on a method parameter</b> —
     *       {@code @io.swagger.v3.oas.annotations.parameters.RequestBody} placed directly on
     *       the parameter is the most specific source and always wins.</li>
     *   <li><b>{@code @Operation.requestBody}</b> — fallback when no inline annotation is present.
     *       Because {@code @Operation.requestBody()} always provides an empty default value,
     *       the annotation is only used when it carries at least one non-default attribute
     *       (see {@link #isNonDefaultRequestBodyAnnotation}).</li>
     * </ol>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the extracted hints; never {@code null}
     */
    private SwaggerBodyHints resolveSwaggerBodyHints(Method method) {
        // 1. Inline @RequestBody on a method parameter — most specific
        Optional<SwaggerBodyHints> fromParam = IntStream.range(0, method.getParameterCount())
                .mapToObj(i -> AnnotationUtils.findSwaggerAnnotation(
                        AnnotationUtils.getAllParameterAnnotations(method, i), "RequestBody"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(this::hintsFromAnnotation);

        if (fromParam.isPresent()) return fromParam.get();

        // 2. @Operation.requestBody — fallback
        return AnnotationUtils.findSwaggerAnnotation(method, "Operation")
                .flatMap(opAnn -> AnnotationAttributeUtils.getAnnotationAttribute(opAnn, "requestBody"))
                .filter(RequestBodyProcessorImpl::isNonDefaultRequestBodyAnnotation)
                .map(this::hintsFromAnnotation)
                .orElse(new SwaggerBodyHints(null, ""));
    }

    /**
     * Extracts {@link SwaggerBodyHints} from a Swagger
     * {@code @io.swagger.v3.oas.annotations.parameters.RequestBody} annotation instance.
     *
     * @param ann the annotation instance; must not be {@code null}
     * @return the extracted hints; never {@code null}
     */
    private SwaggerBodyHints hintsFromAnnotation(Annotation ann) {
        String    description = AnnotationAttributeUtils.getStringAttribute(ann, "description");
        Schema<?> schema      = extractImplementationClass(ann).map(schemaProcessor::toSchema).orElse(null);
        return new SwaggerBodyHints(schema, description);
    }

    /**
     * Returns {@code true} if the {@code @RequestBody} annotation carries at least one
     * non-default value.
     *
     * <p>This guard is necessary because {@code @Operation.requestBody()} always provides a
     * structurally present but semantically empty default annotation instance. Without this
     * check every method with {@code @Operation} would appear to have a Swagger request-body
     * hint, even when none was declared.</p>
     *
     * @param ann the annotation instance to test; must not be {@code null}
     * @return {@code true} if the annotation has at least one meaningful attribute
     */
    private static boolean isNonDefaultRequestBodyAnnotation(Annotation ann) {
        if (!AnnotationAttributeUtils.getStringAttribute(ann, "description").isBlank()) return true;
        if (!AnnotationAttributeUtils.getStringAttribute(ann, "ref").isBlank())         return true;
        if (!AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "content").isEmpty()) return true;
        return AnnotationAttributeUtils.getBooleanAttribute(ann, "required", false);
    }

    /**
     * Navigates the {@code content[0] → schema → implementation} chain of a Swagger
     * {@code @RequestBody} annotation and returns the declared implementation class.
     *
     * <p>Returns empty if any step in the chain is absent or if reflection fails.</p>
     *
     * @param requestBodyAnn the Swagger {@code @RequestBody} annotation instance; must not be {@code null}
     * @return the implementation {@link Class}, or empty if not present or unresolvable
     */
    private Optional<Class<?>> extractImplementationClass(Annotation requestBodyAnn) {
        var contentArr = AnnotationAttributeUtils.getAnnotationArrayAttribute(requestBodyAnn, "content");
        if (contentArr.isEmpty()) return Optional.empty();

        return AnnotationAttributeUtils.getAnnotationAttribute(contentArr.getFirst(), "schema")
                .flatMap(schemaAnn -> AnnotationAttributeUtils.getClassAttribute(schemaAnn, "implementation"))
                .filter(c -> c != Void.class);
    }

    /**
     * Assembles an OpenAPI {@link RequestBody} from the resolved schema and metadata.
     *
     * <p>The media type is determined by {@link #resolveConsumes(Method)}, defaulting to
     * {@value #FALLBACK_CONSUMES_MEDIA_TYPE} when no {@code consumes} attribute is declared.
     * The {@code description} is applied when non-blank.</p>
     *
     * @param schema      the schema describing the body content; must not be {@code null}
     * @param required    whether the request body is required
     * @param description the description from the Swagger {@code @RequestBody} annotation, or {@code ""}
     * @param method      the controller method; used to resolve the media type
     * @return the populated {@link RequestBody}
     */
    private RequestBody buildRequestBody(Schema<?> schema, boolean required, String description, Method method) {
        var mediaType = resolveConsumes(method);

        var content = new Content();
        content.addMediaType(mediaType, new MediaType().schema(schema));

        var body = new RequestBody();
        body.setRequired(required);
        body.setContent(content);
        if (!description.isBlank()) body.setDescription(description);

        log.trace("RequestBody → mediaType:{} required:{} description:{}", mediaType, required, description);
        return body;
    }

    /**
     * Resolves the request body media type from the {@code consumes} attribute of any
     * annotation present on the method (e.g. {@code @PostMapping(consumes = "...")} or
     * {@code @RequestMapping(consumes = "...")}).
     *
     * <p>Returns the first non-blank value found, or {@link #defaultConsumesMediaType} if none
     * is declared.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the resolved media type; never {@code null}
     */
    private String resolveConsumes(Method method) {
        return AnnotationUtils.resolveStringArrayAttribute(method, "consumes", defaultConsumesMediaType);
    }

    /**
     * Reads the {@code required} attribute from a {@code @RequestBody} annotation via reflection.
     *
     * <p>Defaults to {@code true} if the attribute cannot be read (e.g. the annotation type
     * does not declare a {@code required} method or reflection fails).</p>
     *
     * @param requestBodyAnnotation the {@code @RequestBody} annotation instance; must not be {@code null}
     * @return the value of the {@code required} attribute, or {@code true} as a safe default
     */
    private boolean getRequired(Annotation requestBodyAnnotation) {
        return AnnotationAttributeUtils.getBooleanAttribute(requestBodyAnnotation, "required", true);
    }
}
