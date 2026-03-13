# Response Processing

This page describes how `ResponseProcessorImpl` converts Spring MVC annotations into OpenAPI `responses` objects, with particular focus on the **content-inference rules** that determine when a response body schema is emitted and what schema is used.

---

## Overview

For each operation, the processor:

1. Collects all `@ApiResponse` annotations (from `@ApiResponses`, from `@Operation(responses = …)`, and from a bare `@ApiResponse` on the method).
2. For each annotation, builds an OpenAPI `ApiResponse` object — including its `content` map, if any.
3. If no `@ApiResponse` is present at all, generates a single default response whose status code and content are fully inferred.

---

## Content-Inference Rules

The four rules below govern how the `content` map is populated. They are applied per individual `@ApiResponse` annotation.

### Rule 1 — Explicit `@Content` with schema hints → use as declared

If the `@ApiResponse` carries a `content` attribute that references a schema (via `schema = @Schema(…)`, `array = @ArraySchema(…)`, or `mediaType`), the content is used exactly as declared — no inference is applied.

```java
@ApiResponse(
    responseCode = "200",
    description  = "Success",
    content      = @Content(
        mediaType = "application/json",
        schema    = @Schema(implementation = UserDto.class)
    )
)
```

Generated:
```yaml
'200':
  description: Success
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/UserDto'
```

---

### Rule 2 — `@ApiResponse` 2xx, `content` absent or empty `@Content` → infer from return type

When a 2xx response has no `content` attribute **or** has an explicit empty `@Content` (no `mediaType`, no `schema`, no `array`), the processor falls back to building the content from the method's return type.

This means you can declare the description and status code in the annotation without having to duplicate the schema:

```java
@ApiResponse(responseCode = "200", description = "Entity updated successfully")
@PutMapping("/{id}")
public UserDto updateUser(@PathVariable String id, @RequestBody UpdateUserRequest req) { … }
```

Generated (schema inferred from `UserDto`):
```yaml
'200':
  description: Entity updated successfully
  content:
    '*/*':
      schema:
        $ref: '#/components/schemas/UserDto'
```

The same applies when `content = @Content` is written explicitly but left empty:

```java
@ApiResponse(
    responseCode = "200",
    description  = "OK",
    content      = @Content   // empty — triggers inference
)
```

---

### Rule 3 — `@ApiResponse` 4xx / 5xx, `content` absent or empty `@Content` → no body

Error responses typically do not carry a schema. When a non-2xx response has no `content` attribute or an empty `@Content`, no content map is emitted.

```java
@ApiResponse(responseCode = "404", description = "Not Found")
@ApiResponse(responseCode = "400", description = "Invalid request data")
```

Generated:
```yaml
'404':
  description: Not Found
'400':
  description: Invalid request data
```

---

### Rule 4 — No `@ApiResponse` at all → fully inferred default

If the method carries **no** `@ApiResponse` annotation (neither directly, nor inside `@ApiResponses`, nor inside `@Operation`), the processor generates a single default response:

| Return type | Generated response |
|---|---|
| `void` / `Void` | `200 OK` — no body |
| `ResponseEntity<Void>` | `200 OK` — no body |
| Any other type | Status code from `@ResponseStatus` or HTTP method default + content inferred from return type |

HTTP method defaults when no `@ResponseStatus` is present:

| HTTP method | Default status |
|---|---|
| `POST` | `201 Created` |
| Any other | `200 OK` |

```java
// No @ApiResponse — Rule 4 applies
@GetMapping("/{id}")
public UserDto getUser(@PathVariable String id) { … }
```

Generated:
```yaml
'200':
  description: OK
  content:
    '*/*':
      schema:
        $ref: '#/components/schemas/UserDto'
```

---

## Decision Table

| `@ApiResponse` present? | Status class | `content` attribute | Result |
|---|---|---|---|
| Yes | any | explicit, non-empty | Use declared content (Rule 1) |
| Yes | 2xx | absent | Infer from return type (Rule 2) |
| Yes | 2xx | present, empty `@Content` | Infer from return type (Rule 2) |
| Yes | 4xx / 5xx | absent | No body (Rule 3) |
| Yes | 4xx / 5xx | present, empty `@Content` | No body (Rule 3) |
| No | — | — | Infer status + content (Rule 4) |

---

## Default Media Types

The media type used in inferred content maps is configurable:

| Config field | Default | Description |
|---|---|---|
| `GeneratorConfig.defaultProducesMediaType` | `*/*` | Media type for response bodies when no `produces` is declared |

Override via `GeneratorConfig.builder().defaultProducesMediaType("application/json")` or the `<defaultProducesMediaType>` Maven plugin parameter.

---

## `ResponseEntity<T>` Unwrapping

`ResponseEntity<T>` is a transparent wrapper — the schema is resolved from `T`, not from `ResponseEntity` itself. This is handled by `TypeUtils.unwrapType()` before schema resolution.

---

## Related

- [Architecture](Architecture.md) — high-level pipeline overview
- [Schema Handlers](Schema-Handlers.md) — how Java types are resolved to OpenAPI schemas
- [Configuration](Configuration.md) — `defaultProducesMediaType` and other response-related config fields
