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
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
        enrichFromSwaggerOperation(operation, method);
        applyDefaults(operation, method);

        List<io.swagger.v3.oas.models.parameters.Parameter> parameters =
                parameterProcessor.processParameters(method, typeVarMap, mappingHeaders);
        if (!parameters.isEmpty()) {
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
     * Enriches an {@link Operation} with attributes from a
     * {@code @io.swagger.v3.oas.annotations.Operation} annotation, if present on the method.
     *
     * @param operation the operation to enrich
     * @param method    the controller method to inspect
     */
    private void enrichFromSwaggerOperation(Operation operation, Method method) {
        AnnotationUtils.getAllAnnotations(method).stream()
                .filter(ann -> AnnotationUtils.isSwaggerAnnotation(ann, "Operation"))
                .findFirst()
                .ifPresent(ann -> applySwaggerOperation(operation, ann));
    }

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
