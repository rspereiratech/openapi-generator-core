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

import io.github.rspereiratech.openapi.generator.core.processor.response.resolver.DefaultHttpStatusResolver;
import io.github.rspereiratech.openapi.generator.core.processor.response.resolver.HttpStatusResolver;
import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default {@link ResponseProcessor} implementation.
 *
 * <p>Builds {@link ApiResponses} for a controller method.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Explicit {@code @io.swagger.v3.oas.annotations.responses.ApiResponse} annotations.</li>
 *   <li>HTTP status inferred from {@code @ResponseStatus}.</li>
 *   <li>HTTP status inferred from the HTTP method (POST → 201, others → 200).</li>
 *   <li>Response schema derived from the method's return type.</li>
 * </ol>
 *
 * <p>HTTP status-code resolution is delegated to an {@link HttpStatusResolver} strategy,
 * which defaults to {@link DefaultHttpStatusResolver}.
 *
 * @author ruispereira
 */
@Slf4j
public class ResponseProcessorImpl implements ResponseProcessor {
    private static final String DEFAULT_MEDIA_TYPE = "*/*";
    /** Shared {@link SchemaProcessor} used to derive response schemas from method return types. */
    private final SchemaProcessor    schemaProcessor;
    /** Strategy for resolving HTTP status codes and their descriptions. */
    private final HttpStatusResolver statusResolver;

    /**
     * Creates a new {@code ResponseProcessorImpl} using the default {@link DefaultHttpStatusResolver}.
     *
     * @param schemaProcessor the shared schema processor; must not be {@code null}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    public ResponseProcessorImpl(SchemaProcessor schemaProcessor) {
        this(schemaProcessor, new DefaultHttpStatusResolver());
    }

    /**
     * Creates an instance with a custom {@link HttpStatusResolver}.
     *
     * @param schemaProcessor the shared schema processor; must not be null
     * @param statusResolver  strategy for status-code resolution and description; must not be null
     * @throws NullPointerException if either argument is null
     */
    public ResponseProcessorImpl(SchemaProcessor schemaProcessor, HttpStatusResolver statusResolver) {
        this.schemaProcessor = Preconditions.checkNotNull(schemaProcessor, "'schemaProcessor' must not be null");
        this.statusResolver  = Preconditions.checkNotNull(statusResolver,  "'statusResolver' must not be null");
    }

    @Override
    public ApiResponses processResponses(Method method, String httpMethod,
                                          Map<TypeVariable<?>, Type> typeVarMap) {
        Preconditions.checkNotNull(method,     "'method' must not be null");
        Preconditions.checkNotNull(httpMethod, "'httpMethod' must not be null");
        Preconditions.checkNotNull(typeVarMap, "'typeVarMap' must not be null");

        ApiResponses responses = new ApiResponses();

        // PUT and PATCH: SpringDoc does not inherit @ApiResponse from interfaces for these
        // methods — it falls back to the return type, producing only "200 OK".
        boolean hasExplicit = false;
        if (!HttpMethod.PUT.matches(httpMethod) && !HttpMethod.PATCH.matches(httpMethod)) {
            hasExplicit = processExplicitApiResponses(method, responses, typeVarMap);
        }

        if (!hasExplicit) {
            buildDefaultResponse(method, httpMethod, responses, typeVarMap);
        }

        return responses;
    }

    // ------------------------------------------------------------------
    // Explicit Swagger annotations
    // ------------------------------------------------------------------

    /**
     * Scans all effective annotations on {@code method} for Swagger {@code @ApiResponse}
     * (direct) or {@code @ApiResponses} (container) annotations and registers each one.
     *
     * <p>The check is name-based to remain compatible across classloader boundaries.</p>
     *
     * @param method     the controller method to inspect; must not be {@code null}
     * @param responses  the target {@link ApiResponses} to populate; must not be {@code null}
     * @param typeVarMap type-variable mappings for generic type resolution; must not be {@code null}
     * @return {@code true} if at least one explicit response was registered
     */
    private boolean processExplicitApiResponses(Method method, ApiResponses responses,
                                                 Map<TypeVariable<?>, Type> typeVarMap) {
        List<Annotation> allAnnotations = AnnotationUtils.getAllAnnotations(method);

        List<Annotation> direct = allAnnotations.stream()
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "ApiResponse"))
                .toList();

        List<Annotation> fromContainer = allAnnotations.stream()
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "ApiResponses"))
                .flatMap(ann -> AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "value").stream())
                .toList();

        List<Annotation> fromOperation = allAnnotations.stream()
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "Operation"))
                .flatMap(ann -> AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "responses").stream())
                .toList();

        Stream.concat(Stream.concat(direct.stream(), fromContainer.stream()), fromOperation.stream())
                .forEach(ann -> addSwaggerApiResponse(ann, method, responses, typeVarMap));

        return !direct.isEmpty() || !fromContainer.isEmpty() || !fromOperation.isEmpty();
    }

    /**
     * Builds and registers a single OpenAPI {@link ApiResponse} from a Swagger
     * {@code @ApiResponse} annotation instance.
     *
     * <p>Content is resolved via {@link #resolveContent}: the annotation's {@code content}
     * attribute is preferred; the method return type is used as fallback.</p>
     *
     * @param ann        the Swagger {@code @ApiResponse} annotation; must not be {@code null}
     * @param method     the controller method; used for return-type fallback
     * @param responses  the target {@link ApiResponses} to populate
     * @param typeVarMap type-variable mappings for generic type resolution
     */
    private void addSwaggerApiResponse(Annotation ann, Method method, ApiResponses responses,
                                        Map<TypeVariable<?>, Type> typeVarMap) {
        String responseCode = AnnotationAttributeUtils.getStringAttribute(ann, "responseCode");
        if (responseCode.isBlank() || "default".equals(responseCode))
            responseCode = String.valueOf(HttpStatus.OK.value());

        ApiResponse response = new ApiResponse()
                .description(AnnotationAttributeUtils.getStringAttribute(ann, "description"));

        Optional.ofNullable(resolveContent(ann, method, typeVarMap))
                .ifPresent(response::setContent);

        responses.addApiResponse(responseCode, response);
    }

    /**
     * Resolves the {@link Content} for a Swagger {@code @ApiResponse} annotation.
     *
     * <p>Attempts to read the {@code content} attribute from the annotation; falls back to
     * the method's return type if the attribute is absent, empty, or unreadable.</p>
     *
     * @param ann        the Swagger {@code @ApiResponse} annotation; must not be {@code null}
     * @param method     the controller method; used for return-type fallback
     * @param typeVarMap type-variable mappings for generic type resolution
     * @return the resolved {@link Content}, or {@code null} if the return type yields no schema
     */
    private Content resolveContent(Annotation ann, Method method, Map<TypeVariable<?>, Type> typeVarMap) {
        List<Annotation> contentArr = AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "content");
        return !contentArr.isEmpty()
                ? buildContentFromAnnotation(contentArr.getFirst(), method, typeVarMap)
                : buildContentFromReturnType(method, typeVarMap);
    }

    /**
     * Builds a {@link Content} from a Swagger {@code @Content} annotation.
     *
     * <p>Schema resolution order:
     * <ol>
     *   <li>{@code @Schema(implementation=...)} declared on the {@code @Content} annotation.</li>
     *   <li>{@code @ArraySchema(schema=@Schema(implementation=...))} declared on the {@code @Content} annotation.</li>
     *   <li>Method return type, via {@link SchemaProcessor}.</li>
     * </ol>
     * Returns {@code null} if neither source yields a schema.
     * Falls back to {@link #buildContentFromReturnType} on reflection failure.</p>
     *
     * @param contentAnn the Swagger {@code @Content} annotation; must not be {@code null}
     * @param method     the controller method; used as schema fallback
     * @param typeVarMap type-variable mappings for generic type resolution
     * @return the populated {@link Content}, or {@code null} if no schema is available
     */
    @SuppressWarnings("java:S1168") // null is intentional: signals "no body" (e.g. void return type)
    private Content buildContentFromAnnotation(Annotation contentAnn, Method method,
                                                Map<TypeVariable<?>, Type> typeVarMap) {
        String mediaType = AnnotationAttributeUtils.getStringAttribute(contentAnn, "mediaType");
        if (mediaType.isBlank()) mediaType = DEFAULT_MEDIA_TYPE;

        Schema<?> schema = AnnotationAttributeUtils.getAnnotationAttribute(contentAnn, "schema")
                .map(this::schemaFromAnnotation)
                .orElse(null);
        if (schema == null) schema = arraySchemaFromAnnotation(contentAnn);
        if (schema == null && !hasSchemaHints(contentAnn)) return null; // empty @Content → no body
        if (schema == null) schema = schemaProcessor.toSchema(method.getGenericReturnType(), typeVarMap);
        if (schema == null) return null;

        Content content = new Content();
        content.addMediaType(mediaType, new MediaType().schema(schema));
        return content;
    }

    /**
     * Returns {@code true} when the {@code @Content} annotation carries schema hints beyond
     * a plain {@code implementation=Void.class} — specifically a non-empty {@code oneOf},
     * {@code allOf}, or {@code anyOf} array, or an explicit {@code type} string.
     *
     * <p>Used to distinguish a truly empty {@code @Content} (signals "no body") from one
     * that carries intent but whose schema cannot be resolved via {@code implementation}.</p>
     */
    private boolean hasSchemaHints(Annotation contentAnn) {
        return AnnotationAttributeUtils.getAnnotationAttribute(contentAnn, "schema")
                .map(schemaAnn -> {
                    try {
                        Class<?>[] oneOf = (Class<?>[]) schemaAnn.annotationType().getMethod("oneOf").invoke(schemaAnn);
                        Class<?>[] allOf = (Class<?>[]) schemaAnn.annotationType().getMethod("allOf").invoke(schemaAnn);
                        Class<?>[] anyOf = (Class<?>[]) schemaAnn.annotationType().getMethod("anyOf").invoke(schemaAnn);
                        String     type  = (String)    schemaAnn.annotationType().getMethod("type").invoke(schemaAnn);
                        return oneOf.length > 0 || allOf.length > 0 || anyOf.length > 0 || !type.isBlank();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    /**
     * Reads the {@code array} attribute from a Swagger {@code @Content} annotation and
     * returns an {@link ArraySchema} whose items are resolved from
     * {@code @ArraySchema.schema.implementation}.
     *
     * <p>Returns {@code null} if the {@code array} attribute is absent, if
     * {@code @ArraySchema.schema.implementation} is {@link Void}, or if any step yields no schema.</p>
     *
     * @param contentAnn the Swagger {@code @Content} annotation instance; must not be {@code null}
     * @return an {@link ArraySchema} wrapping the item schema, or {@code null}
     */
    @SuppressWarnings("java:S1168")
    private Schema<?> arraySchemaFromAnnotation(Annotation contentAnn) {
        return AnnotationAttributeUtils.getAnnotationAttribute(contentAnn, "array")
                .flatMap(arrayAnn -> AnnotationAttributeUtils.getAnnotationAttribute(arrayAnn, "schema"))
                .map(this::schemaFromAnnotation)
                .map(itemSchema -> (Schema<?>) new ArraySchema().items(itemSchema))
                .orElse(null);
    }

    /**
     * Reads the {@code implementation} attribute from a Swagger {@code @Schema} annotation
     * and returns the corresponding OpenAPI {@link Schema}.
     *
     * <p>Returns {@code null} if the implementation class is {@link Void} or absent.</p>
     *
     * @param schemaAnn the Swagger {@code @Schema} annotation instance; must not be {@code null}
     * @return the resolved {@link Schema}, or {@code null}
     */
    @SuppressWarnings("java:S1168")
    private Schema<?> schemaFromAnnotation(Annotation schemaAnn) {
        return AnnotationAttributeUtils.getClassAttribute(schemaAnn, "implementation")
                .filter(c -> c != Void.class)
                .map(schemaProcessor::toSchema)
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // Default response from return type
    // ------------------------------------------------------------------

    /**
     * Builds and registers a default {@link ApiResponse} derived from the method's return type.
     *
     * <p>The HTTP status code and its description are resolved via {@link HttpStatusResolver}.
     * Content is omitted when the return type yields no schema (e.g. {@code void}).</p>
     *
     * @param method     the controller method; must not be {@code null}
     * @param httpMethod the HTTP verb (e.g. {@code "POST"}); used for status-code inference
     * @param responses  the target {@link ApiResponses} to populate
     * @param typeVarMap type-variable mappings for generic type resolution
     */
    private void buildDefaultResponse(Method method, String httpMethod, ApiResponses responses,
                                       Map<TypeVariable<?>, Type> typeVarMap) {
        String statusCode  = statusResolver.resolveCode(method, httpMethod, method.getGenericReturnType());
        String description = statusResolver.describeCode(statusCode);

        ApiResponse response = new ApiResponse().description(description);
        Optional.ofNullable(buildContentFromReturnType(method, typeVarMap))
                .ifPresent(response::setContent);

        responses.addApiResponse(statusCode, response);
        log.trace("Response → {} {}", statusCode, description);
    }

    /**
     * Builds a {@link Content} from the method's generic return type.
     *
     * <p>Returns {@code null} when the return type yields no schema (e.g. {@code void}).</p>
     *
     * @param method     the controller method; must not be {@code null}
     * @param typeVarMap type-variable mappings for generic type resolution
     * @return the populated {@link Content}, or {@code null} if no schema is available
     */
    @SuppressWarnings("java:S1168") // null is intentional: signals "no body" (e.g. void return type)
    private Content buildContentFromReturnType(Method method, Map<TypeVariable<?>, Type> typeVarMap) {
        Schema<?> schema = schemaProcessor.toSchema(method.getGenericReturnType(), typeVarMap);
        if (schema == null) return null;

        Content content = new Content();
        content.addMediaType(resolveProduces(method), new MediaType().schema(schema));
        return content;
    }

    /**
     * Resolves the response media type from the {@code produces} attribute of any annotation
     * present on the method (e.g. {@code @GetMapping(produces = "...")} or
     * {@code @RequestMapping(produces = "...")}).
     *
     * <p>Returns the first non-blank value found, or {@value #DEFAULT_MEDIA_TYPE} if none is declared.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the resolved media type; never {@code null}
     */
    private String resolveProduces(Method method) {
        return AnnotationUtils.resolveStringArrayAttribute(method, "produces", DEFAULT_MEDIA_TYPE);
    }
}
