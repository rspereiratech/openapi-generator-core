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
| `Pageable` / `PageRequest` | Single `query` parameter with a `$ref` to the `Pageable` schema |

---

### `ResponseProcessorImpl` + `DefaultHttpStatusResolver`

Builds the `responses` map for an operation.

`@ApiResponse` annotations embedded in `@Operation(responses = …)` are also discovered and merged. Response content is resolved according to four inference rules — see [Response Processing](Response-Processing.md) for the full decision table.

**Content-inference rules (summary):**

| Situation | Content resolution |
|---|---|
| `@ApiResponse` with explicit `content` carrying a schema hint | Use exactly what was declared |
| `@ApiResponse` 2xx — `content` absent or empty `@Content` | Infer schema from the method's return type |
| `@ApiResponse` 4xx / 5xx — `content` absent or empty `@Content` | No response body |
| No `@ApiResponse` at all (non-void return) | Default status + return-type schema |
| No `@ApiResponse` at all (void / `Void` return) | `204 No Content`, no body |

When no `@ApiResponse` is present the status code is resolved from `@ResponseStatus` or HTTP method defaults:

| Condition | Default Status |
|---|---|
| `POST` method | `201 Created` |
| Void return type | `204 No Content` |
| All others | `200 OK` |

---

### `SchemaProcessorImpl`

Resolves Java `Type` objects to OpenAPI `Schema` objects using a **chain of responsibility**. See [Schema Handlers](Schema-Handlers.md) for the full chain breakdown.

---

### `ValidationSchemaEnricher`

After `ModelConverters` resolves a DTO class, the raw schema does not always carry constraint metadata (e.g. `minimum`, `maxLength`). `ValidationSchemaEnricher` fills this gap by reflectively reading Jakarta Bean Validation annotations from class fields and applying them to the already-resolved schema properties.

It uses its own **chain of responsibility** of `ConstraintHandler`s — one per annotation type. The default chain covers all standard constraints. A custom chain can be supplied for extension (e.g. Hibernate Validator `@Length`):

```java
new ValidationSchemaEnricher(List.of(
    new MinConstraintHandler(),
    new LengthConstraintHandler(),   // custom
    // ...
));
```

Constraint traversal walks the full superclass hierarchy and resolves `@JsonProperty` names to match the schema property keys produced by `ModelConverters`.

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
│       ├── ValidationSchemaEnricher.java
│       ├── handlers/
│       │   ├── TypeSchemaHandler.java
│       │   ├── VoidTypeSchemaHandler.java
│       │   ├── FluxTypeSchemaHandler.java
│       │   ├── PageTypeSchemaHandler.java
│       │   ├── PageableTypeSchemaHandler.java
│       │   └── ModelConvertersTypeSchemaHandler.java
│       └── constraints/
│           ├── ConstraintHandler.java
│           ├── AbstractConstraintHandler.java
│           ├── MinConstraintHandler.java
│           ├── MaxConstraintHandler.java
│           ├── DecimalMinConstraintHandler.java
│           ├── DecimalMaxConstraintHandler.java
│           ├── PositiveConstraintHandler.java
│           ├── PositiveOrZeroConstraintHandler.java
│           ├── NegativeConstraintHandler.java
│           ├── NegativeOrZeroConstraintHandler.java
│           ├── SizeConstraintHandler.java
│           ├── NotNullConstraintHandler.java
│           ├── NotBlankConstraintHandler.java
│           ├── NotEmptyConstraintHandler.java
│           ├── PatternConstraintHandler.java
│           └── EmailConstraintHandler.java
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
