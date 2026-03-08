# Schema Handlers

The `SchemaProcessorImpl` resolves Java `Type` objects to OpenAPI `Schema` objects using a **chain of responsibility**. Each handler in the chain checks whether it can handle the given type. If it can, it returns a schema. If not, it passes the type to the next handler.

---

## Handler Chain

```
VoidTypeSchemaHandler
        │
        ▼
FluxTypeSchemaHandler
        │
        ▼
PageTypeSchemaHandler
        │
        ▼
PageableTypeSchemaHandler
        │
        ▼
ModelConvertersTypeSchemaHandler   ← catch-all
```

---

## Handlers

### `VoidTypeSchemaHandler`

**Handles:** `void`, `Void`

Returns `null` — void return types produce no response schema in the spec.

---

### `FluxTypeSchemaHandler`

**Handles:** `Flux<T>` (Project Reactor)

Unwraps the type argument `T` and returns an `array` schema whose `items` reference the resolved schema for `T`.

```
Flux<OrderDto>  →  { type: array, items: { $ref: '#/components/schemas/OrderDto' } }
```

---

### `PageTypeSchemaHandler`

**Handles:** `Page<T>` (Spring Data)

Generates a structured paginated schema with the following fields:

| Field | Type | Description |
|---|---|---|
| `content` | `array` of `T` | Items in the current page |
| `page` | `integer` | Current page number (0-based) |
| `size` | `integer` | Number of items per page |
| `totalElements` | `integer` (int64) | Total number of items across all pages |
| `totalPages` | `integer` | Total number of pages |
| `last` | `boolean` | Whether this is the last page |

The schema is named `Page{T}` (e.g. `PageOrderDto`) and registered in `components/schemas`.

---

### `PageableTypeSchemaHandler`

**Handles:** `Pageable`, `PageRequest` (Spring Data)

Instead of generating a schema, this handler expands `Pageable` parameters into two explicit query parameters:

| Parameter | Type | Description |
|---|---|---|
| `page` | `integer` | Page number (0-based) |
| `size` | `integer` | Page size |

This avoids exposing the `Pageable` interface as a complex schema object.

---

### `ModelConvertersTypeSchemaHandler`

**Handles:** All other types (catch-all)

Delegates to Swagger's `ModelConverters`, which resolves Java types to OpenAPI schemas via reflection. This handles all standard DTO classes, enums, collections, maps, and primitives.

Schemas generated here are registered in the shared schema registry and later merged into `components/schemas` by the `SchemaRegistryMergePostProcessor`.

---

## Adding a New Handler

See [CONTRIBUTING.md](../CONTRIBUTING.md#adding-a-new-schema-type-handler) for step-by-step instructions.
