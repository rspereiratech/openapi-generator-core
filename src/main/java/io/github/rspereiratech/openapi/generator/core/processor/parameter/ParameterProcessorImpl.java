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

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Default {@link ParameterProcessor} implementation.
 *
 * <p>Builds OpenAPI {@link Parameter} objects from Spring MVC method parameters.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@code @PathVariable}  → {@code in: path}</li>
 *   <li>{@code @RequestParam}  → {@code in: query}</li>
 *   <li>{@code @RequestHeader} → {@code in: header}</li>
 *   <li>{@code @CookieValue}   → {@code in: cookie}</li>
 *   <li>{@code @io.swagger.v3.oas.annotations.Parameter} – enriches any of the above</li>
 * </ul>
 *
 * <p>{@code @RequestBody} parameters are intentionally excluded from this
 * processor; they are handled by
 * {@link io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor}.
 *
 * @author ruispereira
 */
@Slf4j
public class ParameterProcessorImpl implements ParameterProcessor {
    /**
     * Mapping between supported parameter annotation simple names
     * and their corresponding OpenAPI "in" locations.
     */
    private static final Map<String, String> ANNOTATION_TO_LOCATION = Map.of(
            "PathVariable",  "path",
            "RequestParam",  "query",
            "RequestHeader", "header",
            "CookieValue",   "cookie"
    );

    /**
     * Built-in set of framework-injected parameter types that must never appear
     * as OpenAPI parameters. Mirrors the types ignored by SpringDoc by default.
     */
    static final Set<String> DEFAULT_IGNORED_PARAM_TYPES = Set.of(
            "java.util.Locale",
            "java.security.Principal",
            "jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpSession",
            "jakarta.servlet.ServletRequest",
            "jakarta.servlet.ServletResponse",
            "org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.validation.BindingResult",
            "org.springframework.validation.Errors",
            "org.springframework.ui.Model",
            "org.springframework.ui.ModelMap"
    );

    /**
     * Effective set of parameter type FQNs to skip during processing.
     * Built from the default list (when enabled) and any additional entries.
     */
    private final Set<String> ignoredParamTypes;

    /**
     * Shared {@link SchemaProcessor} used to generate OpenAPI schemas
     * for method parameters.
     */
    private final SchemaProcessor schemaProcessor;

    /**
     * Creates a new {@code ParameterProcessorImpl} with full control over which
     * parameter types are ignored.
     *
     * @param schemaProcessor          the shared schema processor; must not be {@code null}
     * @param ignoreDefaultParamTypes  when {@code true}, the built-in {@link #DEFAULT_IGNORED_PARAM_TYPES}
     *                                 are added to the effective ignore set
     * @param additionalIgnoredTypes   extra FQNs to ignore on top of the defaults; must not be {@code null}
     * @throws NullPointerException if {@code schemaProcessor} or {@code additionalIgnoredTypes} is {@code null}
     */
    public ParameterProcessorImpl(SchemaProcessor schemaProcessor,
                                   boolean ignoreDefaultParamTypes,
                                   Set<String> additionalIgnoredTypes) {
        this.schemaProcessor = Preconditions.checkNotNull(schemaProcessor, "schemaProcessor must not be null");
        Preconditions.checkNotNull(additionalIgnoredTypes, "additionalIgnoredTypes must not be null");
        if (ignoreDefaultParamTypes) {
            var combined = new HashSet<>(DEFAULT_IGNORED_PARAM_TYPES);
            combined.addAll(additionalIgnoredTypes);
            this.ignoredParamTypes = Set.copyOf(combined);
        } else {
            this.ignoredParamTypes = Set.copyOf(additionalIgnoredTypes);
        }
    }

    @Override
    public List<Parameter> processParameters(Method method,
                                             Map<TypeVariable<?>, Type> typeVarMap,
                                             List<String> mappingHeaders) {
        Preconditions.checkNotNull(method,         "method must not be null");
        Preconditions.checkNotNull(typeVarMap,     "typeVarMap must not be null");
        Preconditions.checkNotNull(mappingHeaders, "mappingHeaders must not be null");

        java.lang.reflect.Parameter[] methodParams = method.getParameters();

        Stream<Parameter> methodParamStream = IntStream.range(0, methodParams.length)
                .mapToObj(i -> {
                    java.lang.reflect.Parameter param = methodParams[i];
                    Annotation[] annotations = AnnotationUtils.getAllParameterAnnotations(method, i);
                    return isPageable(param.getType())
                            ? buildPageableParameter(param, i, annotations)
                            : processParameter(param, annotations, typeVarMap);
                })
                .flatMap(Optional::stream);

        Stream<Parameter> mappingHeaderStream = mappingHeaders.stream()
                .filter(h -> !h.isBlank() && !h.startsWith("!"))
                .map(ParameterProcessorImpl::buildMappingHeaderParameter);

        return Stream.concat(methodParamStream, mappingHeaderStream).toList();
    }

    // ------------------------------------------------------------------

    /**
     * Builds an OpenAPI header {@link Parameter} from a Spring MVC header expression.
     *
     * <p>Supports two expression formats:
     * <ul>
     *   <li>{@code "X-Header=value"} — the header must equal {@code value};
     *       the schema is a {@code string} with a single-element {@code enum}.</li>
     *   <li>{@code "X-Header"} — the header must be present;
     *       the schema is an unconstrained {@code string}.</li>
     * </ul>
     * Negative expressions (e.g. {@code "!X-Header"}) are filtered out before reaching
     * this method and should never be passed here.</p>
     *
     * @param headerExpression a Spring MVC header expression; must not be blank or start with {@code !}
     * @return the populated {@link Parameter} with {@code in: header} and {@code required: true}
     */
    @SuppressWarnings("unchecked")
    private static Parameter buildMappingHeaderParameter(String headerExpression) {
        int eqIdx = headerExpression.indexOf('=');

        String name;
        String enumValue = null;

        if (eqIdx >= 0) {
            name      = headerExpression.substring(0, eqIdx).trim();
            enumValue = headerExpression.substring(eqIdx + 1).trim();
        } else {
            name = headerExpression.trim();
        }

        Schema<String> schema = new Schema<>();
        schema.setType("string");
        if (enumValue != null) {
            schema.setEnum(List.of(enumValue));
        }

        Parameter parameter = new HeaderParameter();
        parameter.setIn("header");
        parameter.setName(name);
        parameter.setRequired(true);
        parameter.setSchema(schema);

        log.trace("  Mapping header [{} = {}] → in:header required:true", name, enumValue);
        return parameter;
    }

    /**
     * Attempts to build an OpenAPI {@link Parameter} from a single method parameter.
     *
     * <p>Returns empty if the parameter is annotated with {@code @RequestBody} (handled
     * separately by {@link io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor})
     * or carries no recognised location annotation.</p>
     */
    private Optional<Parameter> processParameter(java.lang.reflect.Parameter param,
                                                  Annotation[] annotations,
                                                  Map<TypeVariable<?>, Type> typeVarMap) {
        if (Arrays.stream(annotations)
                .anyMatch(ann -> "RequestBody".equals(ann.annotationType().getSimpleName()))) {
            return Optional.empty();
        }

        if (isHiddenSwaggerAnnotation(annotations)) return Optional.empty();

        Optional<Schema<?>> schemaOverride = Optional.empty();
        if (ignoredParamTypes.contains(param.getType().getName())) {
            schemaOverride = extractExplicitParameterSchema(annotations);
            if (schemaOverride.isEmpty()) {
                log.trace("Skipping ignored param type: {}", param.getType().getName());
                return Optional.empty();
            }
            log.trace("Ignored param type {} overridden via explicit @Parameter(schema=...)", param.getType().getName());
        }

        final Optional<Schema<?>> finalSchemaOverride = schemaOverride;
        return Arrays.stream(annotations)
                .filter(ann -> ANNOTATION_TO_LOCATION.containsKey(ann.annotationType().getSimpleName()))
                .findFirst()
                .map(ann -> buildParameter(param, ann,
                        ANNOTATION_TO_LOCATION.get(ann.annotationType().getSimpleName()),
                        annotations, typeVarMap, finalSchemaOverride));
    }

    /**
     * Extracts an explicit OpenAPI {@link Schema} from a {@code @Parameter(schema = @Schema(...))}
     * annotation, if present and carrying a non-default {@code type} or {@code implementation}.
     *
     * <p>Returns {@link Optional#empty()} if no {@code @Parameter} annotation is found, or if
     * the nested {@code @Schema} carries only default values (blank {@code type} and
     * {@code Void.class} implementation).</p>
     *
     * @param annotations the effective annotations on the method parameter
     * @return an {@link Optional} containing the explicit schema, or empty if none is defined
     */
    private Optional<Schema<?>> extractExplicitParameterSchema(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "Parameter"))
                .findFirst()
                .flatMap(ann -> AnnotationAttributeUtils.getAnnotationAttribute(ann, "schema"))
                .flatMap(schemaAnn -> {
                    String type = AnnotationAttributeUtils.getStringAttribute(schemaAnn, "type");
                    if (!type.isBlank()) {
                        Schema<?> schema = new Schema<>();
                        schema.setType(type);
                        String format = AnnotationAttributeUtils.getStringAttribute(schemaAnn, "format");
                        if (!format.isBlank()) schema.setFormat(format);
                        String example = AnnotationAttributeUtils.getStringAttribute(schemaAnn, "example");
                        if (!example.isBlank()) schema.setExample(example);
                        return Optional.of(schema);
                    }
                    return AnnotationAttributeUtils.getClassAttribute(schemaAnn, "implementation")
                            .filter(c -> c != Void.class && c != void.class)
                            .map(schemaProcessor::toSchema);
                });
    }

    /**
     * Builds a single OpenAPI {@link Parameter} for a Spring MVC method parameter.
     *
     * @param param             the reflection parameter
     * @param mappingAnnotation the annotation that determined the location (e.g. {@code @PathVariable})
     * @param location          the OpenAPI {@code in} value ("path", "query", "header", "cookie")
     * @param allAnnotations    all effective annotations on the parameter (used for Swagger enrichment)
     * @param typeVarMap        type-variable mappings for generic type resolution
     * @return the populated {@link Parameter}
     */
    private Parameter buildParameter(java.lang.reflect.Parameter param,
                                     Annotation mappingAnnotation,
                                     String location,
                                     Annotation[] allAnnotations,
                                     Map<TypeVariable<?>, Type> typeVarMap,
                                     Optional<Schema<?>> schemaOverride) {
        boolean isPath = "path".equals(location);

        Parameter parameter = isPath ? new PathParameter() : new QueryParameter();
        parameter.setIn(location);
        parameter.setName(resolveName(param, mappingAnnotation));
        parameter.setRequired(isPath || AnnotationAttributeUtils.getBooleanAttribute(mappingAnnotation, "required", false));
        parameter.setSchema(schemaOverride.orElseGet(
                () -> schemaProcessor.toSchema(TypeUtils.resolveType(param.getParameterizedType(), typeVarMap))));

        enrichFromSwaggerAnnotation(parameter, allAnnotations);

        log.trace("  Parameter [{} {}] → in:{} required:{}", param.getType().getSimpleName(),
                parameter.getName(), location, parameter.getRequired());
        return parameter;
    }

    /**
     * Resolves the OpenAPI parameter name from the mapping annotation, falling back to the
     * reflection parameter name or the type simple name.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@code value} attribute of the mapping annotation (e.g. {@code @PathVariable("id")})</li>
     *   <li>{@code name} attribute of the mapping annotation</li>
     *   <li>Reflection parameter name (available when compiled with {@code -parameters})</li>
     *   <li>Type simple name as last resort</li>
     * </ol>
     * </p>
     *
     * @param param             the reflection parameter
     * @param mappingAnnotation the Spring MVC mapping annotation (e.g. {@code @PathVariable})
     * @return the resolved parameter name; never blank
     */
    private static String resolveName(java.lang.reflect.Parameter param, Annotation mappingAnnotation) {
        return Stream.of(
                        AnnotationAttributeUtils.getStringAttribute(mappingAnnotation, "value"),
                        AnnotationAttributeUtils.getStringAttribute(mappingAnnotation, "name"),
                        param.isNamePresent() ? param.getName() : param.getType().getSimpleName()
                )
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElseGet(param.getType()::getSimpleName);
    }

    /**
     * Returns {@code true} if {@code type} is Spring Data's {@code Pageable} or {@code PageRequest}.
     *
     * <p>The check uses the fully-qualified class name to remain compatible across
     * classloader boundaries (the type is loaded from the project's URLClassLoader).</p>
     *
     * @param type the parameter type to test; must not be {@code null}
     * @return {@code true} if the type represents a pageable abstraction
     */
    private static boolean isPageable(Class<?> type) {
        String name = type.getName();
        return "org.springframework.data.domain.Pageable".equals(name)
            || "org.springframework.data.domain.PageRequest".equals(name);
    }

    /**
     * Builds a single {@code Pageable} query parameter backed by the
     * {@code #/components/schemas/Pageable} component schema.
     *
     * <p>Registers the {@code Pageable} component schema as a side-effect via
     * {@link SchemaProcessor#toSchema(Type)} before constructing the parameter.</p>
     */
    private Optional<Parameter> buildPageableParameter(java.lang.reflect.Parameter param, int index, Annotation[] annotations) {

        if (isHiddenSwaggerAnnotation(annotations)) return Optional.empty();

        schemaProcessor.toSchema(param.getType());

        String name = param.isNamePresent() ? param.getName() : "arg" + index;

        Parameter parameter = new QueryParameter()
                .name(name)
                .in("query")
                .required(true)
                .schema(new Schema<>().$ref("#/components/schemas/Pageable"));

        enrichFromSwaggerAnnotation(parameter, annotations);

        return Optional.of(parameter);
    }

    /**
     * Returns {@code true} if the parameter carries a {@code @io.swagger.v3.oas.annotations.Parameter}
     * annotation with {@code hidden = true}.
     */
    private static boolean isHiddenSwaggerAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "Parameter"))
                .findFirst()
                .map(ann -> AnnotationAttributeUtils.getBooleanAttribute(ann, "hidden", false))
                .orElse(false);
    }

    /**
     * Enriches a {@link Parameter} with attributes from a
     * {@code @io.swagger.v3.oas.annotations.Parameter} annotation, if present in {@code annotations}.
     *
     * <p>Applies {@code description}, {@code example}, and {@code hidden} (as {@code x-hidden} extension).
     * Only the first matching annotation is used; unset or blank attributes are ignored.</p>
     *
     * @param parameter   the OpenAPI parameter to enrich; must not be {@code null}
     * @param annotations the effective annotations on the method parameter
     */
    private static void enrichFromSwaggerAnnotation(Parameter parameter, Annotation[] annotations) {
        Arrays.stream(annotations)
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "Parameter"))
                .findFirst()
                .ifPresent(ann -> {
                    String description = AnnotationAttributeUtils.getStringAttribute(ann, "description");
                    String example     = AnnotationAttributeUtils.getStringAttribute(ann, "example");
                    boolean hidden     = AnnotationAttributeUtils.getBooleanAttribute(ann, "hidden", false);

                    if (!description.isBlank()) parameter.setDescription(description);
                    if (!example.isBlank())     parameter.setExample(example);
                });
    }

}
