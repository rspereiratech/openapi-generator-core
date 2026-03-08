# openapi-generator-core

[![CI](https://github.com/rspereiratech/openapi-generator-core/actions/workflows/ci.yml/badge.svg)](https://github.com/rspereiratech/openapi-generator-core/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?logo=spring&logoColor=white)](https://spring.io)
[![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0-green?logo=openapiinitiative)](https://swagger.io/specification/)
![REST API](https://img.shields.io/badge/REST-API-blue)

Core library that generates an OpenAPI 3.0 specification from compiled Spring MVC controllers — **no running server required**.

This module contains all the logic: classpath scanning, annotation resolution, type hierarchy traversal, schema processing, and YAML/JSON serialisation. It is consumed by [`openapi-generator-maven-plugin`](https://github.com/rspereiratech/openapi-generator-maven-plugin), which provides the Maven lifecycle integration.

---

## Table of Contents

- [Quickstart](#quickstart)
- [How It Works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Building](#building)
- [Programmatic Usage](#programmatic-usage)
- [Architecture Overview](#architecture-overview)
- [Key Components](#key-components)
- [Supported Annotations](#supported-annotations)
- [Schema Type Handlers](#schema-type-handlers)
- [Post-Processors](#post-processors)
- [License](#license)

---

## Quickstart

This example shows how to go from a Spring MVC controller to a generated `openapi.yaml` in three steps.

### 1. Add the parent POM

```xml
<parent>
  <groupId>io.github.rspereiratech</groupId>
  <artifactId>openapi-generator-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</parent>
```

### 2. Add the dependency

```xml
<dependency>
  <groupId>io.github.rspereiratech</groupId>
  <artifactId>openapi-generator-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3. Call the generator

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.yaml")
    .title("My API")
    .version("1.0.0")
    .build();

new OpenApiGeneratorImpl().generate(config, Thread.currentThread().getContextClassLoader());
```

That's it — `docs/swagger/openapi.yaml` is generated from your compiled controllers.

> For Maven lifecycle integration (automatic generation on build), use the [`openapi-generator-maven-plugin`](https://github.com/rspereiratech/openapi-generator-maven-plugin) instead.

---

## How It Works

The generation pipeline executes entirely from bytecode — no Spring context is started and no HTTP server is needed.

```
ClasspathScanner          → discovers controller classes in configured packages
OpenApiModelBuilder       → initialises the OpenAPI model (info, servers, security schemes)
ControllerProcessor       → for each controller: resolves base path, tags, and methods
  OperationProcessor      → converts each method to an OpenAPI operation
    ParameterProcessor    → builds path, query, header, and cookie parameters
    RequestBodyProcessor  → builds the request body schema reference
    ResponseProcessor     → builds response codes and response schema references
      SchemaProcessor     → resolves Java types to OpenAPI schemas (chain of responsibility)
PostProcessors            → clean up and finalise the assembled model
OpenApiWriter             → serialises to YAML or JSON
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| `openapi-generator-parent` | latest (must be installed locally) |

---

## Building

```bash
# Install the parent POM first
mvn install -f ../openapi-generator-parent/pom.xml

# Build and install this project
mvn install
```

---

## Programmatic Usage

The library can be used directly without the Maven plugin — useful for custom tooling, CI scripts, or testing.

### Minimal example

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.yaml")
    .title("My API")
    .version("1.0.0")
    .build();

ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

new OpenApiGeneratorImpl().generate(config, classLoader);
```

### With servers and security

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.yaml")
    .title("My API")
    .version("1.0.0")
    .server(ServerConfig.of("https://api.example.com", "Production"))
    .server(ServerConfig.of("http://localhost:8080", "Local"))
    .contextPath("my-api")
    .securityScheme(new SecuritySchemeConfig(
        "bearerAuth", "http", "bearer", "JWT", null, null, null, null
    ))
    .build();

new OpenApiGeneratorImpl().generate(config, classLoader);
```

### With custom controller annotation

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.yaml")
    .controllerAnnotations(List.of("com.example.annotation.MyApiEndpoint"))
    .title("My API")
    .version("1.0.0")
    .build();

new OpenApiGeneratorImpl().generate(config, classLoader);
```

### JSON output

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.json")
    .outputFormat(OutputFormat.JSON)
    .title("My API")
    .version("1.0.0")
    .build();

new OpenApiGeneratorImpl().generate(config, classLoader);
```

> **Note:** The `ClassLoader` passed to `generate()` must be able to load the controller classes and their dependencies. When calling from within the same application, `Thread.currentThread().getContextClassLoader()` is usually correct. When scanning an external project (as the Maven plugin does), a `URLClassLoader` built from the project's compiled classpath must be used.

See [`docs/Configuration.md`](docs/Configuration.md) for the full `GeneratorConfig` field reference.

---

## Architecture Overview

See [`docs/Architecture.md`](docs/Architecture.md) for the full pipeline breakdown, component responsibilities, and design decisions.

---

## Key Components

| Component | Description |
|---|---|
| `OpenApiGeneratorImpl` | Main orchestrator — runs the full pipeline |
| `DefaultClasspathScanner` | Scans directories and JARs for controller classes |
| `ControllerProcessorImpl` | Walks the type hierarchy to collect tags, base paths, and methods |
| `OperationProcessorImpl` | Maps methods to OpenAPI operations using Spring MVC and SpringDoc annotations |
| `ParameterProcessorImpl` | Resolves `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue`, `Pageable` |
| `SchemaProcessorImpl` | Chain-of-responsibility for resolving Java types to OpenAPI schemas |
| `AnnotationUtils` | Recursive meta-annotation traversal across the full type hierarchy |

---

## Supported Annotations

### Routing
`@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`

### Documentation (SpringDoc / Swagger)
`@Operation`, `@ApiResponse`, `@ApiResponses`, `@Parameter`, `@RequestBody`, `@Tag`

### Parameters
`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue`, `@RequestBody`

### Response Status
`@ResponseStatus`

---

## Schema Type Handlers

The `SchemaProcessor` uses a chain of responsibility. Each handler either handles the type or delegates to the next:

| Handler | Types handled |
|---|---|
| `VoidTypeSchemaHandler` | `void`, `Void` |
| `FluxTypeSchemaHandler` | `Flux<T>` (Project Reactor) |
| `PageTypeSchemaHandler` | `Page<T>` (Spring Data) |
| `PageableTypeSchemaHandler` | `Pageable`, `PageRequest` |
| `ModelConvertersTypeSchemaHandler` | All other types via Swagger `ModelConverters` |

See [`docs/Schema-Handlers.md`](docs/Schema-Handlers.md) for details on each handler.

---

## Post-Processors

After all controllers are processed, three post-processors run in sequence:

| Post-Processor | Description |
|---|---|
| `SchemaRegistryMergePostProcessor` | Merges component schemas collected during processing into the final model |
| `PruneUnreferencedSchemasPostProcessor` | Removes schemas from `components/schemas` that are not referenced anywhere |
| `UniqueOperationIdPostProcessor` | Disambiguates duplicate operation IDs using a numeric suffix strategy |

See [`docs/Post-Processors.md`](docs/Post-Processors.md) for details.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
