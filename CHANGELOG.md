# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- `AbstractConstraintHandler<A extends Annotation>` — generic base class for all `ConstraintHandler` implementations; provides a type-safe `supports()` check and encapsulates the unsafe annotation cast, eliminating the same boilerplate from all 14 concrete handlers
- `ParameterProcessorImpl`: parameters of an ignored type (e.g. `Locale`) are now included in the spec when the method carries `@Parameter(schema = @Schema(type = "…"))` — the explicit schema annotation overrides the ignore list
- `DefaultHttpStatusResolver`: status code `"default"` now resolves to the description `"default response"` instead of falling through to the `"Response"` fallback

### Fixed

- `ResponseProcessorImpl`: PUT and PATCH operations now correctly process explicit `@ApiResponse` annotations; previously a special case bypassed `processExplicitApiResponses()` for these HTTP methods
- `ResponseProcessorImpl`: blank `description` in an `@ApiResponse` annotation now falls back to the status-resolver description instead of emitting an empty string
- `DefaultHttpStatusResolver`: `"default"` is no longer incorrectly parsed as an integer, which previously caused a `NumberFormatException` and returned the generic `"Response"` fallback

### Changed

- All 14 `ConstraintHandler` implementations now extend `AbstractConstraintHandler<A>` — the redundant `supports()` override and unsafe `(A)` cast have been removed from every handler
- `AnnotationAttributeUtils`: internal reflection try-catch-log pattern extracted to the private `invokeAttribute()` helper; all six public read methods now delegate to it
- `ResponseProcessorImpl`: duplicated schema-hint reading logic extracted to a `SchemaComposition` private record and `readSchemaComposition()` static method
- `OperationProcessorImpl`: `applySwaggerOperation()` now uses `AnnotationAttributeUtils` helpers (`getStringArrayValue`, `getBooleanAttribute`) instead of inline raw reflection
- `ProcessorFactory`: removed three single-arg convenience overloads (`createParameterProcessor(SchemaProcessor)`, `createRequestBodyProcessor(SchemaProcessor)`, `createResponseProcessor(SchemaProcessor)`) — callers use the full-signature abstracts directly
- `ParameterProcessor`: removed 2-arg `processParameters(Method, Map)` middle overload; the 1-arg default delegates straight to the 3-arg signature
- `OperationProcessor`: removed 4-arg `buildOperation` middle overload; the 3-arg default delegates straight to the 5-arg signature
- `ClasspathScanner`: removed 2-arg `scan(List, ClassLoader)` convenience default; only the 3-arg abstract remains
- `ParameterProcessorImpl`, `RequestBodyProcessorImpl`, `ResponseProcessorImpl`: removed 1-arg convenience constructors; only the full-signature constructors remain
- `SortSpecPostProcessor`: replaced `Collections.sort(list)` with `list.sort(null)`; removed unused `java.util.Collections` import
- `PruneUnreferencedSchemasPostProcessor`: `Collectors.toSet()` replaced with `Collectors.toUnmodifiableSet()`

### Added

- `ValidationSchemaEnricher` (Chain of Responsibility) — propagates Jakarta Bean Validation constraints to OpenAPI schema properties after `ModelConverters` resolution; replaces the former static utility `BeanValidationConstraintApplier`
- `ConstraintHandler` interface — strategy element in the constraint enrichment chain; each implementation maps one Jakarta Bean Validation annotation to its OpenAPI schema equivalent
- Built-in `ConstraintHandler` implementations under `processor/schema/constraints/`: `MinConstraintHandler`, `MaxConstraintHandler`, `DecimalMinConstraintHandler`, `DecimalMaxConstraintHandler`, `PositiveConstraintHandler`, `PositiveOrZeroConstraintHandler`, `NegativeConstraintHandler`, `NegativeOrZeroConstraintHandler`, `SizeConstraintHandler`, `NotNullConstraintHandler`, `NotBlankConstraintHandler`, `NotEmptyConstraintHandler`, `PatternConstraintHandler`, `EmailConstraintHandler`
- `ValidationSchemaEnricher` accepts a custom `List<ConstraintHandler>` for extension (e.g. Hibernate Validator `@Length`) without modifying library code
- `AnnotationAttributeUtils`: added `getAnnotationAttribute`, `getAnnotationArrayAttribute`, `getClassAttribute` — centralise reflective annotation reading across all processors
- `SortSpecPostProcessor` — post-processor that sorts the `paths` block alphabetically by path string and sorts the `responses` map of every operation by HTTP status-code string; enabled by passing `sortOutput = true` to its constructor; always present in the pipeline — when disabled it is a no-op rather than being omitted
- `GeneratorConfig.sortOutput` boolean field (default `false`) — when `true`, controllers are sorted alphabetically by canonical class name before processing and `SortSpecPostProcessor` is activated, guaranteeing identical spec output across machines and builds
- `ProcessorFactory.createPostProcessors` extended with a `boolean sortOutput` parameter forwarded to `SortSpecPostProcessor`
- `ParameterProcessorImpl`: built-in `DEFAULT_IGNORED_PARAM_TYPES` set — silently skips `Locale`, `Principal`, `HttpServletRequest`, `HttpServletResponse`, `HttpSession`, `ServletRequest`, `ServletResponse`, `WebRequest`, `NativeWebRequest`, `BindingResult`, `Errors`, `Model`, `ModelMap`; mirrors SpringDoc default behaviour
- `ParameterProcessorImpl`: new constructor `ParameterProcessorImpl(SchemaProcessor, boolean ignoreDefaultParamTypes, Set<String> additionalIgnoredTypes)` for full control over the effective ignore set
- `GeneratorConfig`: `ignoreDefaultParamTypes` boolean field (default `true`) — when `false`, the built-in ignore list is disabled
- `GeneratorConfig`: `additionalIgnoredParamTypes` list — extra FQNs ignored on top of the defaults
- `ProcessorFactory`: new `createParameterProcessor(SchemaProcessor, boolean, Set<String>)` overload; existing single-argument signature preserved as a `default` method
- `ControllerProcessorImpl`: generates `in: header` parameters from the `headers` attribute of Spring MVC mapping annotations (e.g. `@PostMapping(headers = "x-dashboard-name=pcs")`); `key=value` expressions produce an enum-constrained string schema; when two methods map to the same path and HTTP method, the header-conditioned variant takes precedence
- `GeneratorConfig`: `defaultProducesMediaType` field (default `*/*`) — configures the fallback media type for response bodies when no `produces` attribute is declared; mirrors `springdoc.default-produces-media-type`
- `GeneratorConfig`: `defaultConsumesMediaType` field (default `application/json`) — configures the fallback media type for request bodies when no `consumes` attribute is declared; mirrors `springdoc.default-consumes-media-type`
- `ResponseProcessorImpl`: new constructors `ResponseProcessorImpl(SchemaProcessor, String defaultProducesMediaType)` and `ResponseProcessorImpl(SchemaProcessor, String, HttpStatusResolver)` — the default media type is now an instance property rather than a static constant
- `RequestBodyProcessorImpl`: new constructor `RequestBodyProcessorImpl(SchemaProcessor, String defaultConsumesMediaType)` — the default media type is now an instance property rather than a static constant
- `ProcessorFactory`: new abstract overloads `createResponseProcessor(SchemaProcessor, String)` and `createRequestBodyProcessor(SchemaProcessor, String)`; existing single-argument signatures preserved as `default` methods

### Fixed

- `ParameterProcessorImpl`: parameters annotated with `@Parameter(hidden = true)` are now omitted from the generated spec (aligns with SpringDoc behaviour); previously they were included with an `x-hidden: true` vendor extension
- `ValidationSchemaEnricher`: `@DecimalMin(inclusive = false)` and `@DecimalMax(inclusive = false)` now correctly set `exclusiveMinimum: true` / `exclusiveMaximum: true`; previously the `inclusive` flag was silently ignored
- `ValidationSchemaEnricher`: `@NotBlank` now sets `minLength: 1` in addition to `nullable: false`
- `ValidationSchemaEnricher`: `@NotEmpty` now sets `minLength: 1` (strings) or `minItems: 1` (collections/arrays) in addition to `nullable: false`
- `ValidationSchemaEnricher`: `@Size` on `Collection`, `Map`, and array types now maps to `minItems` / `maxItems` instead of `minLength` / `maxLength`
- `ResponseProcessorImpl`: `@ApiResponse` annotations declared inside `@Operation(responses = …)` are now discovered and registered (previously ignored)
- `ResponseProcessorImpl`: response code `"default"` is now normalised to `200 OK` instead of being treated as a literal string
- `ResponseProcessorImpl`: default media type changed from `application/json` to `*/*` to reflect the Spring MVC fall-through behaviour when no `produces` attribute is declared

---

## [1.0.0] — 2026-03-08

### Added

- `OpenApiGenerator` interface and `OpenApiGeneratorImpl` orchestrator
- `GeneratorConfig` — immutable configuration record with builder pattern
- `ServerConfig`, `SecuritySchemeConfig`, `OutputFormat` — configuration value types
- `DefaultClasspathScanner` — scans directories and JARs for Spring controller classes; supports custom controller annotation detection via recursive meta-annotation traversal
- `OpenApiModelBuilder` — initialises the OpenAPI model with `info`, `servers`, and `security schemes` blocks
- `ControllerProcessorImpl` — walks the full type hierarchy (superclasses + interfaces) to collect base paths, `@Tag` annotations, and mapped methods
- `OperationProcessorImpl` — converts controller methods to OpenAPI operations; enriches with `@Operation`, `@ApiResponse`, and `@Tag` annotations
- `ParameterProcessorImpl` — resolves `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue`, and `Pageable` parameters
- `RequestBodyProcessorImpl` — builds request body schema references from `@RequestBody`
- `ResponseProcessorImpl` — builds response entries from `@ApiResponse` / `@ApiResponses`, with fallback to `@ResponseStatus` and HTTP method defaults
- `DefaultHttpStatusResolver` — resolves HTTP status codes from `@ResponseStatus` or method-based defaults (`POST → 201`, `DELETE → 204`, others `→ 200`)
- `SchemaProcessorImpl` — chain-of-responsibility schema resolution
- `VoidTypeSchemaHandler` — handles `void` / `Void` return types
- `FluxTypeSchemaHandler` — unwraps `Flux<T>` to array schemas
- `PageTypeSchemaHandler` — resolves `Page<T>` to a structured paginated schema
- `PageableTypeSchemaHandler` — resolves `Pageable` / `PageRequest` to pagination query parameters
- `ModelConvertersTypeSchemaHandler` — catch-all handler delegating to Swagger `ModelConverters`
- `SchemaRegistryMergePostProcessor` — merges component schemas from the processing registry into the final model
- `PruneUnreferencedSchemasPostProcessor` — removes unreferenced schemas from `components/schemas`
- `UniqueOperationIdPostProcessor` — disambiguates duplicate operation IDs with a numeric suffix
- `AnnotationUtils` — recursive meta-annotation traversal; collects annotations across the full type hierarchy
- `AnnotationAttributeUtils` — reflective reading of annotation attribute values with alias resolution
- `PathUtils` — path joining and normalisation utilities
- `TypeUtils` — generic type resolution and unwrapping utilities
- `FileUtils` — output file creation helpers
- `WriterFactory`, `YamlWriter`, `JsonWriter` — YAML and JSON serialisation
- `DefaultProcessorFactory` — wires the full processor chain
