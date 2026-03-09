# Architecture

This page describes the full internal architecture of `openapi-generator-core` — how the pipeline is structured, what each component does, and how they interact.

---

## High-Level Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                      OpenApiGeneratorImpl                       │
│                                                                 │
│  1. OpenApiModelBuilder    →  Initialise OpenAPI model          │
│  2. DefaultClasspathScanner →  Discover controller classes      │
│  3. ControllerProcessor    →  For each controller:              │
│       OperationProcessor   →    For each method:                │
│         ParameterProcessor →      Build parameters              │
│         RequestBodyProcessor →    Build request body            │
│         ResponseProcessor  →      Build responses               │
│           SchemaProcessor  →        Resolve types to schemas    │
│  4. PostProcessors         →  Clean up and finalise model       │
│  5. OpenApiWriter          →  Write YAML or JSON                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Responsibilities

### `OpenApiGeneratorImpl`

The main orchestrator. Coordinates all stages in sequence:

1. Builds the initial `OpenAPI` model (info, servers, security) via `OpenApiModelBuilder`.
2. Calls `ClasspathScanner` to discover controller classes.
3. For each controller, delegates to `ControllerProcessor`.
4. Runs all `PostProcessor` instances in order.
5. Delegates to `OpenApiWriter` to serialise the result.

---

### `OpenApiModelBuilder`

Builds the static parts of the OpenAPI model that do not depend on controller scanning:

- `info` block: title, description, version, contact, license
- `servers` block: list of server environments with optional context path
- `components/securitySchemes` + root `security` list

---

### `DefaultClasspathScanner`

Scans the configured base packages for controller classes. It accepts a `URLClassLoader` and iterates over all `.class` files found in directories and JARs on the classpath.

A class is considered a controller if it is annotated (directly or via recursive meta-annotation traversal) with:
- `@RestController`
- `@Controller`
- Any class name listed in `GeneratorConfig.controllerAnnotations()`

---

### `ControllerProcessorImpl`

Processes a single controller class. Walks the **full type hierarchy** — concrete class, superclasses, and interfaces — to collect:

- Base request mapping path (`@RequestMapping`)
- `@Tag` annotations (from all hierarchy levels, unioned)
- All methods annotated with HTTP method mappings

The multi-level walk ensures that operations and tags declared on abstract base classes or interfaces are correctly inherited.

```
ConcreteController
  └─ extends AbstractBase         ← @Tag, @RequestMapping, methods
       └─ implements InterfaceA   ← @Tag, methods
       └─ implements InterfaceB   ← @Tag, methods
            └─ extends InterfaceC ← @Tag
```

---

### `OperationProcessorImpl`

Converts a single controller method into an OpenAPI `Operation`. Collects:

- HTTP method and path from routing annotations
- Summary and description from `@Operation`
- Tags from `@Tag` (merged with controller-level tags)
- Responses from `@ApiResponse` / `@ApiResponses`
- Parameters via `ParameterProcessor`
- Request body via `RequestBodyProcessor`

---

### `ParameterProcessorImpl`

Builds the `parameters` list for an operation. Handles:

| Spring Annotation | OpenAPI `in` |
|---|---|
| `@PathVariable` | `path` |
| `@RequestParam` | `query` |
| `@RequestHeader` | `header` |
| `@CookieValue` | `cookie` |
| `Pageable` / `PageRequest` | Expands to `page` + `size` query parameters |

---

### `ResponseProcessorImpl` + `DefaultHttpStatusResolver`

Builds the `responses` map for an operation.

If `@ApiResponse` / `@ApiResponses` are present they are used directly. `@ApiResponse` annotations embedded in `@Operation(responses = …)` are also discovered and merged. Otherwise, the status code is resolved from `@ResponseStatus` or from HTTP method defaults:

| HTTP Method | Default Status |
|---|---|
| `POST` | `201 Created` |
| `DELETE` | `204 No Content` |
| All others | `200 OK` |

---

### `SchemaProcessorImpl`

Resolves Java `Type` objects to OpenAPI `Schema` objects using a **chain of responsibility**. See [Schema Handlers](Schema-Handlers.md) for the full chain breakdown.

---

### Post-Processors

Run after all controllers have been processed. See [Post-Processors](Post-Processors.md) for details.

---

### `AnnotationUtils`

Provides two key capabilities:

1. **Recursive meta-annotation search** — finds an annotation on a class even if it is only present as a meta-annotation (e.g. `@CustomRestController` which is meta-annotated with `@RestController`).

2. **Full hierarchy collection** — collects all instances of an annotation across the full type hierarchy (class + superclasses + interfaces), enabling multi-tag and multi-mapping scenarios.

---

### `AnnotationAttributeUtils`

Reads annotation attribute values reflectively, resolving aliased attributes (e.g. `@CustomRestController.value()` aliased to `@RequestMapping.value()`).

---

## Package Structure

```
core/
├── OpenApiGenerator.java              # Public interface
├── OpenApiGeneratorImpl.java          # Orchestrator
├── builder/
│   └── OpenApiModelBuilder.java       # Info / servers / security init
├── config/
│   ├── GeneratorConfig.java           # Immutable config record
│   ├── OutputFormat.java              # YAML | JSON
│   ├── SecuritySchemeConfig.java
│   └── ServerConfig.java
├── scanner/
│   ├── ClasspathScanner.java          # Interface
│   └── DefaultClasspathScanner.java   # Scans dirs + JARs
├── processor/
│   ├── ProcessorFactory.java          # Interface
│   ├── DefaultProcessorFactory.java   # Wires the full chain
│   ├── controller/
│   │   ├── ControllerProcessor.java
│   │   └── ControllerProcessorImpl.java
│   ├── operation/
│   │   ├── OperationProcessor.java
│   │   └── OperationProcessorImpl.java
│   ├── parameter/
│   │   ├── ParameterProcessor.java
│   │   └── ParameterProcessorImpl.java
│   ├── request/
│   │   ├── RequestBodyProcessor.java
│   │   └── RequestBodyProcessorImpl.java
│   ├── response/
│   │   ├── ResponseProcessor.java
│   │   ├── ResponseProcessorImpl.java
│   │   └── resolver/
│   │       ├── HttpStatusResolver.java
│   │       └── DefaultHttpStatusResolver.java
│   └── schema/
│       ├── SchemaProcessor.java
│       ├── SchemaProcessorImpl.java
│       └── handlers/
│           ├── TypeSchemaHandler.java
│           ├── VoidTypeSchemaHandler.java
│           ├── FluxTypeSchemaHandler.java
│           ├── PageTypeSchemaHandler.java
│           ├── PageableTypeSchemaHandler.java
│           └── ModelConvertersTypeSchemaHandler.java
├── postprocessor/
│   ├── PostProcessor.java
│   ├── SchemaRegistryMergePostProcessor.java
│   ├── PruneUnreferencedSchemasPostProcessor.java
│   └── UniqueOperationIdPostProcessor.java
├── utils/
│   ├── AnnotationUtils.java
│   ├── AnnotationAttributeUtils.java
│   ├── PathUtils.java
│   ├── TypeUtils.java
│   └── FileUtils.java
└── writer/
    ├── OpenApiWriter.java
    ├── WriterFactory.java
    ├── YamlWriter.java
    └── JsonWriter.java
```
