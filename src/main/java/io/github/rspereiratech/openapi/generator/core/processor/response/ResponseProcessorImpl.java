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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default {@link ResponseProcessor} implementation.
 *
 * <p>Builds {@link ApiResponses} for a controller method.
 *
 * <p>Content resolution rules (applied per {@code @ApiResponse}):
 * <ol>
 *   <li><b>Explicit content declared</b> ({@code @Content} with schema hints) → used as-is.</li>
 *   <li><b>2xx without content</b> (no {@code content} attribute, or empty {@code @Content}) →
 *       schema inferred from the method's return type.</li>
 *   <li><b>4xx/5xx without content</b> → no response body.</li>
 *   <li><b>No {@code @ApiResponse} at all</b> → status code resolved via
 *       {@link io.github.rspereiratech.openapi.generator.core.processor.response.resolver.HttpStatusResolver};
 *       schema inferred from return type (void return produces 200 with no body).</li>
 * </ol>
 *
 * <p>HTTP status-code resolution is delegated to an {@link HttpStatusResolver} strategy,
 * which defaults to {@link DefaultHttpStatusResolver}.
 *
 * @author ruispereira
 */
@Slf4j
public class ResponseProcessorImpl implements ResponseProcessor {
    /** Default media type for response bodies — mirrors {@code springdoc.default-produces-media-type}. */
    private static final String FALLBACK_PRODUCES_MEDIA_TYPE = "*/*";

    /** Shared {@link ObjectMapper} instance; avoids allocating a new one per {@link #parseJsonValue} call. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Shared {@link SchemaProcessor} used to derive response schemas from method return types. */
    private final SchemaProcessor    schemaProcessor;
    /** Strategy for resolving HTTP status codes and their descriptions. */
    private final HttpStatusResolver statusResolver;
    /** Default media type used when no {@code produces} or {@code @Content(mediaType)} is declared. */
    private final String             defaultProducesMediaType;

    /**
     * Creates an instance with a configurable default produces media type and the default
     * {@link DefaultHttpStatusResolver}.
     *
     * @param schemaProcessor        the shared schema processor; must not be {@code null}
     * @param defaultProducesMediaType the default media type when no {@code produces} is declared;
     *                               {@code null} or blank falls back to {@value #FALLBACK_PRODUCES_MEDIA_TYPE}
     * @throws NullPointerException if {@code schemaProcessor} is {@code null}
     */
    public ResponseProcessorImpl(SchemaProcessor schemaProcessor, String defaultProducesMediaType) {
        this(schemaProcessor, defaultProducesMediaType, new DefaultHttpStatusResolver());
    }

    /**
     * Creates an instance with a custom {@link HttpStatusResolver} and configurable default
     * produces media type.
     *
     * @param schemaProcessor        the shared schema processor; must not be {@code null}
     * @param defaultProducesMediaType the default media type when no {@code produces} is declared;
     *                               {@code null} or blank falls back to {@value #FALLBACK_PRODUCES_MEDIA_TYPE}
     * @param statusResolver         strategy for status-code resolution and description; must not be {@code null}
     * @throws NullPointerException if {@code schemaProcessor} or {@code statusResolver} is {@code null}
     */
    public ResponseProcessorImpl(SchemaProcessor schemaProcessor, String defaultProducesMediaType,
                                  HttpStatusResolver statusResolver) {
        this.schemaProcessor         = Preconditions.checkNotNull(schemaProcessor, "'schemaProcessor' must not be null");
        this.statusResolver          = Preconditions.checkNotNull(statusResolver,  "'statusResolver' must not be null");
        this.defaultProducesMediaType = (defaultProducesMediaType == null || defaultProducesMediaType.isBlank())
                ? FALLBACK_PRODUCES_MEDIA_TYPE : defaultProducesMediaType;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code method}, {@code httpMethod}, or {@code typeVarMap} is {@code null}
     */
    @Override
    public ApiResponses processResponses(Method method, String httpMethod,
                                          Map<TypeVariable<?>, Type> typeVarMap) {
        Preconditions.checkNotNull(method,     "'method' must not be null");
        Preconditions.checkNotNull(httpMethod, "'httpMethod' must not be null");
        Preconditions.checkNotNull(typeVarMap, "'typeVarMap' must not be null");

        ApiResponses responses = new ApiResponses();

        boolean hasExplicit = processExplicitApiResponses(method, responses, typeVarMap);
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
        if (responseCode.isBlank()) responseCode = "default";

        String description = AnnotationAttributeUtils.getStringAttribute(ann, "description");
        if (description.isBlank()) description = statusResolver.describeCode(responseCode);

        ApiResponse response = new ApiResponse().description(description);

        Optional.ofNullable(resolveContent(ann, responseCode, method, typeVarMap))
                .ifPresent(response::setContent);

        responses.addApiResponse(responseCode, response);
    }

    /**
     * Resolves the {@link Content} for a Swagger {@code @ApiResponse} annotation,
     * applying the content-inference rules based on the response code:
     *
     * <ul>
     *   <li>If the annotation carries {@code @Content} with schema hints → used as-is (Rule 1).</li>
     *   <li>If the annotation has no {@code content} attribute, or carries an empty {@code @Content},
     *       and the response code is 2xx → schema is inferred from the method's return type (Rule 2).</li>
     *   <li>If the response code is 4xx/5xx and no schema hints are present → no body (Rule 3).</li>
     * </ul>
     *
     * @param ann          the Swagger {@code @ApiResponse} annotation; must not be {@code null}
     * @param responseCode the HTTP response code string (e.g. {@code "200"}, {@code "404"})
     * @param method       the controller method; used for return-type inference
     * @param typeVarMap   type-variable mappings for generic type resolution
     * @return the resolved {@link Content}, or {@code null} if no body applies
     */
    private Content resolveContent(Annotation ann, String responseCode, Method method,
                                    Map<TypeVariable<?>, Type> typeVarMap) {
        List<Annotation> contentArr = AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "content");

        if (contentArr.isEmpty()) {
            // No content attribute declared — Rule 2 (2xx) / Rule 3 (4xx/5xx)
            return is2xx(responseCode) ? buildContentFromReturnType(method, typeVarMap) : null;
        }

        Content fromAnnotation = buildContentFromAnnotation(contentArr.getFirst(), method, typeVarMap);
        // Empty @Content on 2xx — Rule 2: infer schema from return type
        if (fromAnnotation == null && is2xx(responseCode)) {
            return buildContentFromReturnType(method, typeVarMap);
        }
        return fromAnnotation;
    }

    /**
     * Returns {@code true} when {@code responseCode} represents a 2xx HTTP status.
     * Non-numeric values (e.g. {@code "default"}) return {@code false}.
     *
     * @param responseCode the HTTP response code string; must not be {@code null}
     * @return {@code true} if the code is in the range [200, 300)
     */
    private static boolean is2xx(String responseCode) {
        try {
            int code = Integer.parseInt(responseCode);
            return code >= 200 && code < 300;
        } catch (NumberFormatException e) {
            return false;
        }
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
        if (mediaType.isBlank()) mediaType = defaultProducesMediaType;

        Schema<?> schema = AnnotationAttributeUtils.getAnnotationAttribute(contentAnn, "schema")
                .map(this::schemaFromAnnotation)
                .orElse(null);
        if (schema == null) schema = arraySchemaFromAnnotation(contentAnn);
        if (schema == null) schema = composedSchemaFromAnnotation(contentAnn);
        if (schema == null && !hasSchemaHints(contentAnn)) return null; // empty @Content → no body
        if (schema == null) schema = schemaProcessor.toSchema(method.getGenericReturnType(), typeVarMap);
        if (schema == null) return null;

        MediaType mediaTypeObj = new MediaType().schema(schema);
        applyExamples(contentAnn, mediaTypeObj);

        Content content = new Content();
        content.addMediaType(mediaType, mediaTypeObj);
        return content;
    }

    /**
     * Reads the {@code examples} attribute from a Swagger {@code @Content} annotation and
     * applies them to the given {@link MediaType}.
     *
     * <p>Each {@code @ExampleObject} in the array is read; entries whose {@code name} is blank
     * are silently skipped. Non-blank fields ({@code summary}, {@code description}, {@code value},
     * {@code externalValue}) are set on the resulting {@link Example} model object.</p>
     *
     * @param contentAnn   the Swagger {@code @Content} annotation; must not be {@code null}
     * @param mediaTypeObj the {@link MediaType} to enrich; must not be {@code null}
     */
    private static void applyExamples(Annotation contentAnn, MediaType mediaTypeObj) {
        List<Annotation> examplesArr = AnnotationAttributeUtils.getAnnotationArrayAttribute(contentAnn, "examples");
        for (Annotation exampleAnn : examplesArr) {
            String name = AnnotationAttributeUtils.getStringAttribute(exampleAnn, "name");
            if (name.isBlank()) continue;

            Example example = new Example();
            String summary       = AnnotationAttributeUtils.getStringAttribute(exampleAnn, "summary");
            String description   = AnnotationAttributeUtils.getStringAttribute(exampleAnn, "description");
            String value         = AnnotationAttributeUtils.getStringAttribute(exampleAnn, "value");
            String externalValue = AnnotationAttributeUtils.getStringAttribute(exampleAnn, "externalValue");

            if (!summary.isBlank())       example.setSummary(summary);
            if (!description.isBlank())   example.setDescription(description);
            if (!value.isBlank())         example.setValue(parseJsonValue(value));
            if (!externalValue.isBlank()) example.setExternalValue(externalValue);

            mediaTypeObj.addExamples(name, example);
        }
    }

    /**
     * Attempts to parse {@code raw} as a JSON value using Jackson.
     *
     * <p>When {@code raw} is valid JSON (object, array, or scalar), the resulting
     * {@link JsonNode} is returned so that the YAML serializer emits it as structured
     * YAML rather than as a plain quoted string.  If parsing fails the raw string is
     * returned unchanged as a fallback.</p>
     *
     * @param raw the raw string value from an {@code @ExampleObject}; must not be {@code null}
     * @return a {@link JsonNode} if {@code raw} is valid JSON, otherwise the original string
     */
    private static Object parseJsonValue(String raw) {
        try {
            return OBJECT_MAPPER.readValue(raw, JsonNode.class);
        } catch (Exception e) {
            log.debug("Example value is not valid JSON, using raw string: {}", e.getMessage());
            return raw;
        }
    }

    /**
     * Builds a composed {@link Schema} from the {@code oneOf}, {@code allOf}, {@code anyOf},
     * or {@code type} attributes declared on the {@code @Schema} annotation inside a
     * Swagger {@code @Content} annotation.
     *
     * <p>Each class listed in {@code oneOf}/{@code allOf}/{@code anyOf} is resolved via
     * {@link SchemaProcessor}; {@link Void} entries and unresolvable types are silently
     * skipped.  When {@code type} is non-blank it is set on the resulting schema.
     * Returns {@code null} when none of the attributes yield any content (e.g., all
     * class arrays are empty and {@code type} is blank).</p>
     *
     * @param contentAnn the Swagger {@code @Content} annotation; must not be {@code null}
     * @return a composed {@link Schema}, or {@code null}
     */
    @SuppressWarnings("java:S1168")
    private Schema<?> composedSchemaFromAnnotation(Annotation contentAnn) {
        return AnnotationAttributeUtils.getAnnotationAttribute(contentAnn, "schema")
                .flatMap(ResponseProcessorImpl::readSchemaComposition)
                .flatMap(this::buildComposedSchema)
                .orElse(null);
    }

    /**
     * Builds an OpenAPI composed schema ({@code oneOf}/{@code allOf}/{@code anyOf}/{@code type})
     * from a resolved {@link SchemaComposition}.
     *
     * <p>Returns {@link Optional#empty()} when all composition arrays are empty and {@code type}
     * is blank — i.e., when there is nothing to compose.
     *
     * @param sc the resolved schema composition; must not be {@code null}
     * @return an {@link Optional} wrapping the composed schema, or empty if nothing to compose
     */
    private Optional<Schema<?>> buildComposedSchema(SchemaComposition sc) {
        var oneOf = toSchemaList(sc.oneOf());
        var allOf = toSchemaList(sc.allOf());
        var anyOf = toSchemaList(sc.anyOf());

        if (oneOf.isEmpty() && allOf.isEmpty() && anyOf.isEmpty() && sc.type().isBlank()) {
            return Optional.empty();
        }

        Schema<?> composed = new Schema<>();
        if (!oneOf.isEmpty()) composed.setOneOf(oneOf);
        if (!allOf.isEmpty()) composed.setAllOf(allOf);
        if (!anyOf.isEmpty()) composed.setAnyOf(anyOf);
        if (!sc.type().isBlank()) composed.setType(sc.type());
        return Optional.of(composed);
    }

    /**
     * Maps an array of classes to resolved raw {@link Schema} objects, skipping {@link Void}
     * and unresolvable types.
     *
     * <p>Returns {@code List<Schema>} — the raw type — to match the signature expected by
     * {@link Schema#setOneOf}, {@link Schema#setAllOf}, and {@link Schema#setAnyOf} in the
     * Swagger Core API, avoiding any cast at the call site.
     *
     * @param classes the array of implementation classes to resolve
     * @return list of resolved schemas; never {@code null}, may be empty
     */
    @SuppressWarnings("rawtypes") // raw Schema matches the Swagger Core setOneOf/setAllOf/setAnyOf API
    private List<Schema> toSchemaList(Class<?>[] classes) {
        return Arrays.stream(classes)
                .filter(c -> c != Void.class)
                .map(schemaProcessor::toSchema)
                .filter(Objects::nonNull)
                .map(s -> (Schema) s)
                .toList();
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
                .flatMap(ResponseProcessorImpl::readSchemaComposition)
                .map(sc -> sc.oneOf().length > 0 || sc.allOf().length > 0
                        || sc.anyOf().length > 0 || !sc.type().isBlank())
                .orElse(false);
    }

    /**
     * Reads the composition attributes ({@code oneOf}, {@code allOf}, {@code anyOf}, {@code type})
     * from a Swagger {@code @Schema} annotation via reflection.
     *
     * <p>Returns {@link Optional#empty()} if any reflection step fails (e.g. the annotation type
     * does not declare the expected attributes).</p>
     *
     * @param schemaAnn a Swagger {@code @Schema} annotation instance; must not be {@code null}
     * @return the composition attributes, or empty if unreadable
     */
    private static Optional<SchemaComposition> readSchemaComposition(Annotation schemaAnn) {
        try {
            Class<?>[] oneOf = (Class<?>[]) schemaAnn.annotationType().getMethod("oneOf").invoke(schemaAnn);
            Class<?>[] allOf = (Class<?>[]) schemaAnn.annotationType().getMethod("allOf").invoke(schemaAnn);
            Class<?>[] anyOf = (Class<?>[]) schemaAnn.annotationType().getMethod("anyOf").invoke(schemaAnn);
            String     type  = (String)    schemaAnn.annotationType().getMethod("type").invoke(schemaAnn);
            return Optional.of(new SchemaComposition(oneOf, allOf, anyOf, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Holds the composition-related attributes of a Swagger {@code @Schema} annotation,
     * read once and shared between {@link #composedSchemaFromAnnotation} and
     * {@link #hasSchemaHints}.
     *
     * @param oneOf the classes listed in {@code @Schema(oneOf = {...})}
     * @param allOf the classes listed in {@code @Schema(allOf = {...})}
     * @param anyOf the classes listed in {@code @Schema(anyOf = {...})}
     * @param type  the {@code type} string (e.g. {@code "object"}, {@code "string"})
     */
    private record SchemaComposition(Class<?>[] oneOf, Class<?>[] allOf, Class<?>[] anyOf, String type) {}

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
     * <p>Returns the first non-blank value found, or {@link #defaultProducesMediaType} if none
     * is declared.</p>
     *
     * @param method the controller method to inspect; must not be {@code null}
     * @return the resolved media type; never {@code null}
     */
    private String resolveProduces(Method method) {
        return AnnotationUtils.resolveStringArrayAttribute(method, "produces", defaultProducesMediaType);
    }
}
