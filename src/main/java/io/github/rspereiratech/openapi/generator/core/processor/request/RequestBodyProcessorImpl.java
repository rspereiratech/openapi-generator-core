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

    /** Default media type applied when no {@code consumes} attribute is declared on the handler method. */
    private static final String DEFAULT_MEDIA_TYPE = "application/json";

    /** Shared {@link SchemaProcessor} used to derive request-body schemas from parameter types. */
    private final SchemaProcessor schemaProcessor;

    /**
     * Creates a new {@code RequestBodyProcessorImpl}.
     *
     * @param schemaProcessor the shared schema processor; must not be {@code null}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    public RequestBodyProcessorImpl(SchemaProcessor schemaProcessor) {
        this.schemaProcessor = Preconditions.checkNotNull(schemaProcessor, "'schemaProcessor' must not be null");
    }

    @Override
    @SuppressWarnings("java:S1872")
    public Optional<RequestBody> processRequestBody(Method method, Map<TypeVariable<?>, Type> typeVarMap) {
        Preconditions.checkNotNull(method,     "'method' must not be null");
        Preconditions.checkNotNull(typeVarMap, "'typeVarMap' must not be null");

        Schema<?> swaggerOverrideSchema = resolveSwaggerRequestBodySchema(method);
        Parameter[] params = method.getParameters();

        return IntStream.range(0, params.length)
                .mapToObj(i -> Arrays.stream(AnnotationUtils.getAllParameterAnnotations(method, i))
                        .filter(ann -> "RequestBody".equals(ann.annotationType().getSimpleName()))
                        .findFirst()
                        .map(ann -> {
                            boolean required = getRequired(ann);
                            Schema<?> schema = swaggerOverrideSchema != null
                                    ? swaggerOverrideSchema
                                    : schemaProcessor.toSchema(params[i].getParameterizedType(), typeVarMap);
                            return buildRequestBody(schema, required, method);
                        }))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Searches all method parameters for a Swagger
     * {@code @io.swagger.v3.oas.annotations.parameters.RequestBody} that declares an explicit
     * {@code content[0].schema.implementation}, and returns the corresponding schema.
     *
     * <p>Returns {@code null} when no such annotation is found or the implementation class
     * cannot be extracted.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the resolved {@link Schema}, or {@code null} if no override is present
     */
    private Schema<?> resolveSwaggerRequestBodySchema(Method method) {
        return IntStream.range(0, method.getParameterCount())
                .boxed()
                .flatMap(i -> Arrays.stream(AnnotationUtils.getAllParameterAnnotations(method, i)))
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "RequestBody"))
                .findFirst()
                .flatMap(this::extractImplementationClass)
                .map(schemaProcessor::toSchema)
                .orElse(null);
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
        try {
            Annotation[] contentArr = (Annotation[]) requestBodyAnn.annotationType()
                    .getDeclaredMethod("content").invoke(requestBodyAnn);
            if (contentArr == null || contentArr.length == 0) return Optional.empty();

            Object schemaResult = contentArr[0].annotationType()
                    .getDeclaredMethod("schema").invoke(contentArr[0]);

            Optional<Annotation> schemaAnn = firstAnnotation(schemaResult);
            if (schemaAnn.isEmpty()) return Optional.empty();

            Object impl = schemaAnn.get().annotationType()
                    .getDeclaredMethod("implementation").invoke(schemaAnn.get());
            return impl instanceof Class<?> c && c != Void.class
                    ? Optional.of(c)
                    : Optional.empty();
        } catch (Exception e) {
            log.warn("Could not read implementation class from @RequestBody content: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Coerces a reflectively-obtained value to a single {@link Annotation}.
     *
     * <p>Handles both direct {@link Annotation} values and {@code Annotation[]} arrays,
     * returning the first element in the latter case.</p>
     *
     * @param value the object returned by an annotation attribute accessor; may be {@code null}
     * @return the first annotation found, or empty if {@code value} is neither an
     *         {@link Annotation} nor a non-empty {@code Annotation[]}
     */
    private static Optional<Annotation> firstAnnotation(Object value) {
        return switch (value) {
            case Annotation a                          -> Optional.of(a);
            case Annotation[] arr when arr.length > 0 -> Optional.of(arr[0]);
            default                                    -> Optional.empty();
        };
    }

    /**
     * Assembles an OpenAPI {@link RequestBody} from the resolved schema.
     *
     * <p>The media type is determined by {@link #resolveConsumes(Method)}, defaulting to
     * {@value #DEFAULT_MEDIA_TYPE} when no {@code consumes} attribute is declared.</p>
     *
     * @param schema   the schema describing the body content; must not be {@code null}
     * @param required whether the request body is required
     * @param method   the controller method; used to resolve the media type
     * @return the populated {@link RequestBody}
     */
    private RequestBody buildRequestBody(Schema<?> schema, boolean required, Method method) {
        String mediaType = resolveConsumes(method);

        Content content = new Content();
        content.addMediaType(mediaType, new MediaType().schema(schema));

        RequestBody body = new RequestBody();
        body.setRequired(required);
        body.setContent(content);

        log.trace("RequestBody → mediaType:{} required:{}", mediaType, required);
        return body;
    }

    /**
     * Resolves the request body media type from the {@code consumes} attribute of any
     * annotation present on the method (e.g. {@code @PostMapping(consumes = "...")} or
     * {@code @RequestMapping(consumes = "...")}).
     *
     * <p>Returns the first non-blank value found, or {@value #DEFAULT_MEDIA_TYPE} if none is declared.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the resolved media type; never {@code null}
     */
    private String resolveConsumes(Method method) {
        return AnnotationUtils.resolveStringArrayAttribute(method, "consumes", DEFAULT_MEDIA_TYPE);
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
        try {
            return (boolean) requestBodyAnnotation.annotationType()
                    .getDeclaredMethod("required").invoke(requestBodyAnnotation);
        } catch (Exception e) {
            log.warn("Could not read 'required' from @RequestBody; defaulting to true: {}", e.getMessage());
            return true;
        }
    }
}
