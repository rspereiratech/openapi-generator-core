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
package io.github.rspereiratech.openapi.generator.core.processor.controller;

import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessor;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationUtils;
import io.github.rspereiratech.openapi.generator.core.utils.PathUtils;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link ControllerProcessor}.
 *
 * <p>Processes a single Spring MVC controller class and populates the
 * provided {@link OpenAPI} model with the paths and operations declared
 * by that controller.</p>
 *
 * <p>This implementation:
 * <ul>
 *     <li>Resolves the controller-level request mapping (if present)</li>
 *     <li>Iterates over handler methods</li>
 *     <li>Detects supported HTTP mapping annotations</li>
 *     <li>Delegates operation construction to {@link OperationProcessor}</li>
 *     <li>Merges generated operations into the {@link OpenAPI} paths section</li>
 * </ul>
 * </p>
 *
 * <p>Supported HTTP method annotations:
 * {@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping},
 * {@code @DeleteMapping}, {@code @PatchMapping}, {@code @RequestMapping}.</p>
 *
 * <p>This class is stateless and intended to be reused across multiple
 * controller-processing invocations within a single generation run.</p>
 *
 * @author ruispereira
 * @see ControllerProcessor
 * @see OperationProcessor
 */
@Slf4j
public class ControllerProcessorImpl implements ControllerProcessor {
    /** Strips common controller-class suffixes when deriving tag names. */
    private static final Pattern CONTROLLER_SUFFIX =
            Pattern.compile("(?i)(controller|api|resource|rest|endpoint)$");

    /** Maps simple annotation name → OpenAPI HTTP verb. */
    private static final Map<String, PathItem.HttpMethod> MAPPING_TO_HTTP = Map.of(
            "GetMapping",    PathItem.HttpMethod.GET,
            "PostMapping",   PathItem.HttpMethod.POST,
            "PutMapping",    PathItem.HttpMethod.PUT,
            "DeleteMapping", PathItem.HttpMethod.DELETE,
            "PatchMapping",  PathItem.HttpMethod.PATCH
    );

    /**
     * Processor responsible for building {@link io.swagger.v3.oas.models.Operation}
     * instances from controller handler methods.
     *
     * <p>Delegated to for each detected HTTP-mapped method during controller processing.</p>
     */
    private final OperationProcessor operationProcessor;

    /**
     * Creates a new {@code ControllerProcessorImpl}.
     *
     * @param operationProcessor the processor used to build each OpenAPI operation;
     *                           must not be {@code null}
     * @throws NullPointerException if {@code operationProcessor} is {@code null}
     */
    public ControllerProcessorImpl(OperationProcessor operationProcessor) {
        this.operationProcessor = Preconditions.checkNotNull(operationProcessor, "operationProcessor must not be null");
    }

    @Override
    public void process(Class<?> controllerClass, OpenAPI openAPI) {
        Preconditions.checkNotNull(controllerClass, "controllerClass must not be null");
        Preconditions.checkNotNull(openAPI,         "openAPI must not be null");
        log.info("Processing controller: {}", controllerClass.getSimpleName());

        String basePath = resolveBasePath(controllerClass);
        Collection<String> tags = resolveTags(controllerClass, openAPI);
        Map<TypeVariable<?>, Type> typeVarMap = TypeUtils.buildTypeVariableMap(controllerClass);

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new Paths());
        }

        Arrays.stream(controllerClass.getMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> !m.isSynthetic() && !m.isBridge())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .forEach(m -> processMethod(m, basePath, tags, openAPI, typeVarMap));
    }

    // ------------------------------------------------------------------
    // Method-level processing
    // ------------------------------------------------------------------

    /**
     * Processes a single controller method and, if it declares a supported
     * Spring MVC mapping annotation, generates and registers the corresponding
     * OpenAPI {@link Operation}.
     *
     * <p>The method performs the following steps:
     * <ol>
     *     <li>Collects all effective annotations (including meta-annotations).</li>
     *     <li>Checks for specific HTTP mapping annotations
     *         ({@code @GetMapping}, {@code @PostMapping}, etc.).</li>
     *     <li>If found, resolves the sub-path and HTTP verb, builds the operation,
     *         and adds it to the {@link OpenAPI} model.</li>
     *     <li>If no specific mapping is found, falls back to {@code @RequestMapping},
     *         resolving one or more HTTP methods (defaulting to {@code GET} if none specified).</li>
     * </ol>
     *
     * <p>If the method does not declare any supported mapping annotation,
     * it is ignored.</p>
     *
     * @param method      the controller method to analyse; must not be {@code null}
     * @param basePath    the base path declared at controller level (may be empty but not {@code null})
     * @param tags        tags inherited from the owning controller; must not be {@code null}
     * @param openAPI     the target OpenAPI model to update; must not be {@code null}
     * @param typeVarMap  type-variable → concrete-type mappings used to resolve generics;
     *                    may be empty but not {@code null}
     */
    private void processMethod(Method method, String basePath,
                                Collection<String> tags, OpenAPI openAPI,
                                Map<TypeVariable<?>, Type> typeVarMap) {

        List<Annotation> effectiveAnnotations = AnnotationUtils.getAllAnnotations(method);

        // Single pass: find the first specific mapping annotation via direct map lookup
        for (Annotation ann : effectiveAnnotations) {
            PathItem.HttpMethod httpMethod = MAPPING_TO_HTTP.get(ann.annotationType().getSimpleName());
            if (httpMethod != null) {
                String fullPath = PathUtils.joinPaths(basePath, AnnotationAttributeUtils.extractPath(ann));
                List<String> mappingHeaders = AnnotationAttributeUtils.getStringArrayValue(ann, "headers");
                addOperation(openAPI, fullPath, httpMethod,
                        operationProcessor.buildOperation(method, httpMethod.name(), tags, typeVarMap, mappingHeaders),
                        mappingHeaders);
                return;
            }
        }

        // Fall back to generic @RequestMapping
        findBySimpleName(effectiveAnnotations, "RequestMapping").ifPresent(ann -> {
            String fullPath = PathUtils.joinPaths(basePath, AnnotationAttributeUtils.extractPath(ann));
            List<String> mappingHeaders = AnnotationAttributeUtils.getStringArrayValue(ann, "headers");
            List<PathItem.HttpMethod> httpMethods = resolveHttpMethods(ann);
            if (httpMethods.isEmpty()) httpMethods = List.of(PathItem.HttpMethod.GET);
            httpMethods.forEach(httpMethod -> addOperation(openAPI, fullPath, httpMethod,
                    operationProcessor.buildOperation(method, httpMethod.name(), tags, typeVarMap, mappingHeaders),
                    mappingHeaders));
        });
    }

    // ------------------------------------------------------------------
    // Helpers – path & tag resolution
    // ------------------------------------------------------------------

    /**
     * Resolves the base path for a controller class from its {@code @RequestMapping} annotation.
     *
     * @param controllerClass the controller class to inspect
     * @return the normalised base path, or an empty string if no {@code @RequestMapping} is present
     */
    private String resolveBasePath(Class<?> controllerClass) {
        return findBySimpleName(AnnotationUtils.getAllAnnotations(controllerClass), "RequestMapping")
                .map(ann -> PathUtils.normalisePath(AnnotationAttributeUtils.extractPath(ann)))
                .orElse("");
    }

    /**
     * Resolves the OpenAPI tags for a controller class.
     *
     * <p>Collects all {@code @Tag} annotations (including inherited and meta-annotated ones),
     * registers each unique non-blank tag name in the {@link OpenAPI} model, and returns them
     * in declaration order. If no {@code @Tag} annotations are found, falls back to a tag
     * derived from the controller's simple class name via {@link #deriveTagName}.</p>
     *
     * @param controllerClass the controller class to inspect
     * @param openAPI         the OpenAPI model to register discovered tags into
     * @return the ordered, deduplicated list of tag names for this controller; never empty
     */
    private Collection<String> resolveTags(Class<?> controllerClass, OpenAPI openAPI) {
        Set<String> tags = new LinkedHashSet<>();

        for (Annotation tagAnn : AnnotationUtils.collectAllBySimpleName(controllerClass, "Tag")) {
            String name = AnnotationAttributeUtils.getStringAttribute(tagAnn, "name");
            if (!name.isBlank() && tags.add(name)) {
                registerTag(openAPI, name, AnnotationAttributeUtils.getStringAttribute(tagAnn, "description"));
            }
        }

        if (tags.isEmpty()) {
            String tagName = deriveTagName(controllerClass.getSimpleName());
            registerTag(openAPI, tagName, "");
            tags.add(tagName);
        }

        return tags;
    }

    /**
     * Derives a camelCase OpenAPI tag name from a controller class simple name.
     *
     * <p>Strips common suffixes ({@code Controller}, {@code Api}, {@code Resource},
     * {@code Rest}, {@code Endpoint}, case-insensitive) and lower-cases the first character.
     * If stripping the suffix leaves an empty string, {@code simpleName} is returned unchanged.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "TenantController"} → {@code "tenant"}</li>
     *   <li>{@code "AgentApi"}        → {@code "agent"}</li>
     *   <li>{@code "Api"}             → {@code "Api"} (suffix-only input, returned as-is)</li>
     * </ul>
     * </p>
     *
     * @param simpleName the controller class simple name
     * @return the derived tag name; never {@code null}
     */
    private static String deriveTagName(String simpleName) {
        String name = CONTROLLER_SUFFIX.matcher(simpleName).replaceAll("").trim();
        return name.isEmpty() ? simpleName : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Adds a tag to the {@link OpenAPI} model if no tag with the same name is already registered.
     *
     * @param openAPI     the OpenAPI model to update
     * @param name        the tag name
     * @param description the tag description
     */
    private void registerTag(OpenAPI openAPI, String name, String description) {
        List<Tag> existing = openAPI.getTags();
        if (existing == null || existing.stream().noneMatch(t -> name.equals(t.getName()))) {
            openAPI.addTagsItem(new Tag().name(name).description(description));
        }
    }

    // ------------------------------------------------------------------
    // Helpers – HTTP method
    // ------------------------------------------------------------------

    /**
     * Resolves the HTTP methods declared on a {@code @RequestMapping} annotation.
     *
     * <p>Reads the {@code method} attribute via reflection and maps each
     * {@link org.springframework.web.bind.annotation.RequestMethod} value to the
     * corresponding {@link PathItem.HttpMethod}. Values unrecognised by
     * {@link PathItem.HttpMethod} are skipped with a warning.</p>
     *
     * @param requestMappingAnn the {@code @RequestMapping} annotation instance; must not be {@code null}
     * @return the resolved HTTP methods; empty if the attribute could not be read or is absent
     */
    private List<PathItem.HttpMethod> resolveHttpMethods(Annotation requestMappingAnn) {
        try {
            Object[] methodEnums = (Object[]) requestMappingAnn.annotationType()
                    .getDeclaredMethod("method").invoke(requestMappingAnn);
            return Arrays.stream(methodEnums)
                    .map(m -> {
                        try {
                            return PathItem.HttpMethod.valueOf(m.toString().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("Unrecognised HTTP method '{}' in @RequestMapping — skipping", m);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Could not read 'method' attribute from @RequestMapping: {}", e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Path item management
    // ------------------------------------------------------------------

    /**
     * Adds the given {@link Operation} to the {@link OpenAPI} model under the
     * specified path and HTTP method.
     *
     * <p>If a {@link PathItem} for the path already exists, the operation is added to it;
     * otherwise, a new {@link PathItem} is created.</p>
     *
     * <p>When multiple Spring MVC methods map to the same path and HTTP method via different
     * header conditions (e.g. a generic {@code search()} and a header-conditioned
     * {@code searchV2(headers = "x-dashboard-name=pcs")}), the more specific variant
     * (with mapping headers) takes precedence. A headerless operation will not overwrite
     * an existing operation that already carries header parameters derived from mapping headers.</p>
     *
     * @param openAPI        the OpenAPI model to update; must not be {@code null}
     * @param path           the URL path (combined base + sub-path); must not be {@code null}
     * @param httpMethod     the HTTP method for the operation; must not be {@code null}
     * @param operation      the OpenAPI operation to add; must not be {@code null}
     * @param mappingHeaders header expressions extracted from the mapping annotation; may be empty
     */
    private void addOperation(OpenAPI openAPI, String path,
                               PathItem.HttpMethod httpMethod, Operation operation,
                               List<String> mappingHeaders) {
        log.debug("  {} {}", httpMethod, path);
        PathItem pathItem = openAPI.getPaths().getOrDefault(path, new PathItem());

        if (mappingHeaders.isEmpty() && hasHeaderMappingParams(pathItem, httpMethod)) {
            log.debug("  Skipping {} {} (no mapping headers) — more specific variant already registered", httpMethod, path);
            return;
        }

        pathItem.operation(httpMethod, operation);
        openAPI.getPaths().addPathItem(path, pathItem);
    }

    /**
     * Returns {@code true} if the existing operation at the given {@code httpMethod} on
     * {@code pathItem} already carries at least one {@code in: header} parameter, indicating
     * it was generated from a header-conditioned mapping annotation.
     *
     * @param pathItem   the path item to inspect; must not be {@code null}
     * @param httpMethod the HTTP method to check; must not be {@code null}
     * @return {@code true} if a header parameter is present on the existing operation
     */
    private static boolean hasHeaderMappingParams(PathItem pathItem, PathItem.HttpMethod httpMethod) {
        Operation existing = pathItem.readOperationsMap().get(httpMethod);
        if (existing == null || existing.getParameters() == null) return false;
        return existing.getParameters().stream()
                .anyMatch(p -> "header".equals(p.getIn()));
    }

    // ------------------------------------------------------------------
    // Annotation lookup
    // ------------------------------------------------------------------

    /**
     * Finds the first annotation in the given list whose type is directly or
     * transitively meta-annotated with an annotation matching the supplied
     * simple name.
     *
     * <p>This allows detection of both direct Spring mapping annotations
     * (e.g. {@code @GetMapping}) and composed annotations that are
     * meta-annotated with them.</p>
     *
     * @param annotations the annotations to inspect; must not be {@code null}
     * @param simpleName  the simple name of the target annotation
     *                    (e.g. {@code "GetMapping"}, {@code "RequestMapping"});
     *                    must not be {@code null}
     * @return an {@link Optional} containing the first matching annotation,
     *         or {@link Optional#empty()} if none match
     */
    private Optional<Annotation> findBySimpleName(List<Annotation> annotations, String simpleName) {
        return annotations.stream()
                .filter(ann -> AnnotationUtils.isMetaAnnotated(ann.annotationType(), simpleName))
                .findFirst();
    }
}
