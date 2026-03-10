# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- `ParameterProcessorImpl`: built-in `DEFAULT_IGNORED_PARAM_TYPES` set — silently skips `Locale`, `Principal`, `HttpServletRequest`, `HttpServletResponse`, `HttpSession`, `ServletRequest`, `ServletResponse`, `WebRequest`, `NativeWebRequest`, `BindingResult`, `Errors`, `Model`, `ModelMap`; mirrors SpringDoc default behaviour
- `ParameterProcessorImpl`: new constructor `ParameterProcessorImpl(SchemaProcessor, boolean ignoreDefaultParamTypes, Set<String> additionalIgnoredTypes)` for full control over the effective ignore set
- `GeneratorConfig`: `ignoreDefaultParamTypes` boolean field (default `true`) — when `false`, the built-in ignore list is disabled
- `GeneratorConfig`: `additionalIgnoredParamTypes` list — extra FQNs ignored on top of the defaults
- `ProcessorFactory`: new `createParameterProcessor(SchemaProcessor, boolean, Set<String>)` overload; existing single-argument signature preserved as a `default` method
- `ControllerProcessorImpl`: generates `in: header` parameters from the `headers` attribute of Spring MVC mapping annotations (e.g. `@PostMapping(headers = "x-dashboard-name=pcs")`); `key=value` expressions produce an enum-constrained string schema; when two methods map to the same path and HTTP method, the header-conditioned variant takes precedence

---

## [1.1.0] — 2026-03-10

### Added

- `ValidationSchemaEnricher` (Chain of Responsibility) — propagates Jakarta Bean Validation constraints to OpenAPI schema properties after `ModelConverters` resolution; replaces the former static utility `BeanValidationConstraintApplier`
- `ConstraintHandler` interface — strategy element in the constraint enrichment chain; each implementation maps one Jakarta Bean Validation annotation to its OpenAPI schema equivalent
- Built-in `ConstraintHandler` implementations under `processor/schema/constraints/`: `MinConstraintHandler`, `MaxConstraintHandler`, `DecimalMinConstraintHandler`, `DecimalMaxConstraintHandler`, `PositiveConstraintHandler`, `PositiveOrZeroConstraintHandler`, `NegativeConstraintHandler`, `NegativeOrZeroConstraintHandler`, `SizeConstraintHandler`, `NotNullConstraintHandler`, `NotBlankConstraintHandler`, `NotEmptyConstraintHandler`, `PatternConstraintHandler`, `EmailConstraintHandler`
- `ValidationSchemaEnricher` accepts a custom `List<ConstraintHandler>` for extension (e.g. Hibernate Validator `@Length`) without modifying library code
- `AnnotationAttributeUtils`: added `getAnnotationAttribute`, `getAnnotationArrayAttribute`, `getClassAttribute` — centralise reflective annotation reading across all processors
- `SortSpecPostProcessor` — post-processor that sorts the `paths` block alphabetically by path string and sorts the `responses` map of every operation by HTTP status-code string; enabled by passing `sortOutput = true` to its constructor; always present in the pipeline — when disabled it is a no-op rather than being omitted
- `GeneratorConfig.sortOutput` boolean field (default `false`) — when `true`, controllers are sorted alphabetically by canonical class name before processing and `SortSpecPostProcessor` is activated, guaranteeing identical spec output across machines and builds
- `ProcessorFactory.createPostProcessors` extended with a `boolean sortOutput` parameter forwarded to `SortSpecPostProcessor`

### Fixed

- `ParameterProcessorImpl`: parameters annotated with `@Parameter(hidden = true)` are now omitted from the generated spec (aligns with SpringDoc behaviour); previously they were included with an `x-hidden: true` vendor extension
- `ValidationSchemaEnricher`: `@DecimalMin(inclusive = false)` and `@DecimalMax(inclusive = false)` now correctly set `exclusiveMinimum: true` / `exclusiveMaximum: true`; previously the `inclusive` flag was silently ignored
- `ValidationSchemaEnricher`: `@NotBlank` now sets `minLength: 1` in addition to `nullable: false`
- `ValidationSchemaEnricher`: `@NotEmpty` now sets `minLength: 1` (strings) or `minItems: 1` (collections/arrays) in addition to `nullable: false`
- `ValidationSchemaEnricher`: `@Size` on `Collection`, `Map`, and array types now maps to `minItems` / `maxItems` instead of `minLength` / `maxLength`

---

## [1.0.1] — 2026-03-09

### Fixed

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
