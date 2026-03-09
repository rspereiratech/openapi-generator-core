# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
