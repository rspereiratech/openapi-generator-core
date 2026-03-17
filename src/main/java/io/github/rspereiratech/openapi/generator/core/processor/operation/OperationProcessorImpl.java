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
import io.github.rspereiratech.openapi.generator.core.processor.request.RequestBodyProcessor;
import io.github.rspereiratech.openapi.generator.core.processor.response.ResponseProcessor;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default {@link OperationProcessor} implementation.
 *
 * <p>Transforms a single controller {@link Method} into an OpenAPI {@link Operation}.
 *
 * <p>Enrichment priority:
 * <ol>
 *   <li>{@code @io.swagger.v3.oas.annotations.Operation} – explicit metadata.</li>
 *   <li>Javadoc-style fallbacks from the method name (camelCase → sentence).</li>
 * </ol>
 *
 * @author ruispereira
 */
@Slf4j
public class OperationProcessorImpl implements OperationProcessor {

    /** Converts Spring MVC parameters to OpenAPI {@link io.swagger.v3.oas.models.parameters.Parameter} objects. */
    private final ParameterProcessor   parameterProcessor;
    /** Converts {@code @RequestBody} parameters to an OpenAPI {@link io.swagger.v3.oas.models.parameters.RequestBody}. */
    private final RequestBodyProcessor requestBodyProcessor;
    /** Derives the OpenAPI {@link io.swagger.v3.oas.models.responses.ApiResponses} for each operation. */
    private final ResponseProcessor    responseProcessor;

    /**
     * Creates an instance wiring the three sub-processors used during operation building.
     *
     * @param parameterProcessor   processor for path/query/header parameters; must not be null
     * @param requestBodyProcessor processor for request bodies; must not be null
     * @param responseProcessor    processor for responses; must not be null
     * @throws NullPointerException if any argument is null
     */
    public OperationProcessorImpl(ParameterProcessor parameterProcessor,
                                  RequestBodyProcessor requestBodyProcessor,
                                  ResponseProcessor responseProcessor) {
        this.parameterProcessor   = Preconditions.checkNotNull(parameterProcessor,   "parameterProcessor must not be null");
        this.requestBodyProcessor = Preconditions.checkNotNull(requestBodyProcessor, "requestBodyProcessor must not be null");
        this.responseProcessor    = Preconditions.checkNotNull(responseProcessor,    "responseProcessor must not be null");
    }

    @Override
    public Operation buildOperation(Method method, String httpMethod, Collection<String> tags,
                                    Map<TypeVariable<?>, Type> typeVarMap, List<String> mappingHeaders) {
        Preconditions.checkNotNull(method,         "method must not be null");
        Preconditions.checkNotNull(httpMethod,     "httpMethod must not be null");
        Preconditions.checkNotNull(tags,           "tags must not be null");
        Preconditions.checkNotNull(typeVarMap,     "typeVarMap must not be null");
        Preconditions.checkNotNull(mappingHeaders, "mappingHeaders must not be null");

        log.debug("  Processing method: {} {}", httpMethod, method.getName());

        Operation operation = new Operation();
        tags.forEach(operation::addTagsItem);

        Optional<Annotation> swaggerOp = AnnotationUtils.findSwaggerAnnotation(method, "Operation");
        swaggerOp.ifPresent(ann -> applySwaggerOperation(operation, ann));
        applyDefaults(operation, method);

        List<io.swagger.v3.oas.models.parameters.Parameter> parameters =
                parameterProcessor.processParameters(method, typeVarMap, mappingHeaders);
        if (!parameters.isEmpty()) {
            swaggerOp.ifPresent(ann -> enrichParametersFromOperationAnnotation(parameters, ann));
            operation.setParameters(parameters);
        }

        requestBodyProcessor.processRequestBody(method, typeVarMap)
                .ifPresent(operation::setRequestBody);

        operation.setResponses(responseProcessor.processResponses(method, httpMethod, typeVarMap));

        if (isDeprecated(method)) operation.setDeprecated(true);

        return operation;
    }

    /**
     * Sets fallback {@code operationId} and {@code summary} if not already populated
     * by a {@code @Operation} annotation.
     */
    private void applyDefaults(Operation operation, Method method) {
        if (operation.getOperationId() == null || operation.getOperationId().isBlank()) {
            operation.setOperationId(method.getName());
        }
        if (operation.getSummary() == null || operation.getSummary().isBlank()) {
            operation.setSummary(methodNameToSentence(method.getName()));
        }
    }

    /**
     * Returns {@code true} if the method carries a {@link Deprecated} annotation,
     * including inherited and meta-annotated occurrences.
     */
    private static boolean isDeprecated(Method method) {
        return AnnotationUtils.getAllAnnotations(method).stream()
                .anyMatch(a -> a.annotationType() == Deprecated.class);
    }

    // ------------------------------------------------------------------
    // Swagger @Operation enrichment
    // ------------------------------------------------------------------

    /**
     * Applies scalar and collection attributes of a
     * {@code @io.swagger.v3.oas.annotations.Operation} annotation to the given {@link Operation}.
     */
    private void applySwaggerOperation(Operation operation, Annotation ann) {
        String summary     = AnnotationAttributeUtils.getStringAttribute(ann, "summary");
        String description = AnnotationAttributeUtils.getStringAttribute(ann, "description");
        String operationId = AnnotationAttributeUtils.getStringAttribute(ann, "operationId");
        boolean hidden     = AnnotationAttributeUtils.getBooleanAttribute(ann, "hidden", false);
        boolean deprecated = AnnotationAttributeUtils.getBooleanAttribute(ann, "deprecated", false);

        if (!summary.isBlank())     operation.setSummary(summary);
        if (!description.isBlank()) operation.setDescription(description);
        if (!operationId.isBlank()) operation.setOperationId(operationId);
        if (hidden)                 operation.addExtension("x-hidden", true);
        if (deprecated)             operation.setDeprecated(true);

        AnnotationAttributeUtils.getStringArrayValue(ann, "tags").stream()
                .filter(t -> !t.isBlank())
                .forEach(operation::addTagsItem);

        AnnotationAttributeUtils.getAnnotationAttribute(ann, "externalDocs")
                .ifPresent(extDoc -> applyExternalDocs(extDoc, operation));

        AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "servers")
                .forEach(serverAnn -> applyServer(serverAnn, operation));

        AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "security")
                .forEach(secAnn -> applySecurityRequirement(secAnn, operation));

        AnnotationAttributeUtils.getAnnotationArrayAttribute(ann, "extensions")
                .forEach(extAnn -> applyExtension(extAnn, operation));
    }

    /**
     * Converts an {@code @ExternalDocumentation} annotation attribute to an
     * OpenAPI {@link ExternalDocumentation} model and sets it on {@code operation},
     * when either {@code url} or {@code description} is non-blank.
     *
     * @param extDocAnn the annotation instance; must not be {@code null}
     * @param operation the operation to mutate; must not be {@code null}
     */
    private static void applyExternalDocs(Annotation extDocAnn, Operation operation) {
        String url         = AnnotationAttributeUtils.getStringAttribute(extDocAnn, "url");
        String description = AnnotationAttributeUtils.getStringAttribute(extDocAnn, "description");
        if (url.isBlank() && description.isBlank()) return;

        ExternalDocumentation model = new ExternalDocumentation();
        if (!url.isBlank())         model.setUrl(url);
        if (!description.isBlank()) model.setDescription(description);
        operation.setExternalDocs(model);
    }

    /**
     * Converts a {@code @Server} annotation attribute to an OpenAPI {@link Server} model
     * and adds it to {@code operation} via {@link Operation#addServersItem}, when
     * {@code url} is non-blank.
     *
     * @param serverAnn the annotation instance; must not be {@code null}
     * @param operation the operation to mutate; must not be {@code null}
     */
    private static void applyServer(Annotation serverAnn, Operation operation) {
        String url         = AnnotationAttributeUtils.getStringAttribute(serverAnn, "url");
        String description = AnnotationAttributeUtils.getStringAttribute(serverAnn, "description");
        if (url.isBlank()) return;

        Server server = new Server();
        server.setUrl(url);
        if (!description.isBlank()) server.setDescription(description);
        operation.addServersItem(server);
    }

    /**
     * Converts a {@code @SecurityRequirement} annotation attribute to an OpenAPI
     * {@link SecurityRequirement} model and adds it to {@code operation} via
     * {@link Operation#addSecurityItem}, when {@code name} is non-blank.
     *
     * @param secAnn    the annotation instance; must not be {@code null}
     * @param operation the operation to mutate; must not be {@code null}
     */
    private static void applySecurityRequirement(Annotation secAnn, Operation operation) {
        String name = AnnotationAttributeUtils.getStringAttribute(secAnn, "name");
        if (name.isBlank()) return;

        List<String> scopes = AnnotationAttributeUtils.getStringArrayValue(secAnn, "scopes");
        SecurityRequirement requirement = new SecurityRequirement();
        requirement.addList(name, scopes);
        operation.addSecurityItem(requirement);
    }

    /**
     * Converts an {@code @Extension} annotation attribute and its {@code @ExtensionProperty}
     * entries to individual {@link Operation#addExtension} calls.
     *
     * <p>The extension name is used as a key prefix when non-blank; each property
     * produces an {@code addExtension} call with the property {@code name} as key
     * (prefixed with {@code "x-"} when not already prefixed) and {@code value} as value.
     *
     * @param extAnn    the annotation instance; must not be {@code null}
     * @param operation the operation to mutate; must not be {@code null}
     */
    private static void applyExtension(Annotation extAnn, Operation operation) {
        String extensionName = AnnotationAttributeUtils.getStringAttribute(extAnn, "name");
        AnnotationAttributeUtils.getAnnotationArrayAttribute(extAnn, "properties")
                .forEach(propAnn -> {
                    String propName  = AnnotationAttributeUtils.getStringAttribute(propAnn, "name");
                    String propValue = AnnotationAttributeUtils.getStringAttribute(propAnn, "value");
                    if (propName.isBlank()) return;

                    String key = extensionName.isBlank() ? propName : extensionName + "." + propName;
                    if (!key.startsWith("x-")) key = "x-" + key;
                    operation.addExtension(key, propValue);
                });
    }

    // ------------------------------------------------------------------
    // @Operation.parameters[] enrichment
    // ------------------------------------------------------------------

    /**
     * Enriches already-built concrete parameters with {@code description} and {@code example}
     * values declared in {@code @Operation.parameters[]}.
     *
     * <p>This covers the pattern where Swagger enrichment is placed inside
     * {@code @Operation(parameters = { @Parameter(name = "tenantId", description = "...") })}
     * instead of directly on the Java method parameter. Only attributes not already set on the
     * concrete parameter are applied — the concrete annotation always wins.</p>
     *
     * <p>The {@code @Operation} annotation instance is resolved once by {@link #buildOperation}
     * and passed here directly to avoid a redundant type-hierarchy walk.</p>
     *
     * @param parameters the concrete parameters already built by {@link ParameterProcessor}
     * @param opAnn      the resolved {@code @Operation} annotation instance
     */
    private static void enrichParametersFromOperationAnnotation(
            List<io.swagger.v3.oas.models.parameters.Parameter> parameters, Annotation opAnn) {
        Map<String, Annotation> byName = AnnotationAttributeUtils
                .getAnnotationArrayAttribute(opAnn, "parameters")
                .stream()
                .collect(Collectors.toMap(
                        ann -> AnnotationAttributeUtils.getStringAttribute(ann, "name"),
                        ann -> ann,
                        (a, b) -> a));

        parameters.forEach(param -> {
            Annotation ann = byName.get(param.getName());
            if (ann == null) return;

            if (param.getDescription() == null) {
                String desc = AnnotationAttributeUtils.getStringAttribute(ann, "description");
                if (!desc.isBlank()) param.setDescription(desc);
            }
            if (param.getExample() == null) {
                String example = AnnotationAttributeUtils.getStringAttribute(ann, "example");
                if (!example.isBlank()) param.setExample(example);
            }
        });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Converts a camelCase method name to a human-readable sentence.
     * Example: {@code "getUserById"} → {@code "Get user by id"}.
     */
    static String methodNameToSentence(String name) {
        if (name == null || name.isBlank()) return name;
        String spaced = name.replaceAll("([A-Z])", " $1").toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).trim();
    }
}
