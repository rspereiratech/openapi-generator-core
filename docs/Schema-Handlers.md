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

Generates a structured paginated schema named `Page{T}` (e.g. `PageOrderDto`) and registers it in `components/schemas`. The property order matches SpringDoc's default output:

| Field | Type | Description |
|---|---|---|
| `totalElements` | `integer` (int64) | Total number of items across all pages |
| `totalPages` | `integer` (int32) | Total number of pages |
| `size` | `integer` (int32) | Number of items per page |
| `content` | `array` of `T` | Items in the current page |
| `number` | `integer` (int32) | Current page number (0-based) |
| `first` | `boolean` | Whether this is the first page |
| `last` | `boolean` | Whether this is the last page |
| `sort` | `array` of `$ref: SortObject` | Sort criteria applied to the page |
| `numberOfElements` | `integer` (int32) | Number of elements on this page |
| `pageable` | `$ref: PageableObject` | Pagination metadata |
| `empty` | `boolean` | Whether the page has no content |

In addition, two helper component schemas are registered if not already present:

**`SortObject`** properties: `direction`, `nullHandling`, `ascending`, `property`, `ignoreCase`

**`PageableObject`** properties: `offset`, `sort` (array of `$ref: SortObject`), `paged`, `pageNumber`, `pageSize`, `unpaged`

---

### `PageableTypeSchemaHandler`

**Handles:** `Pageable`, `PageRequest` (Spring Data)

Registers a `Pageable` component schema in `components/schemas` (if not already present) and returns a `$ref` pointing to it. The component schema has three properties:

| Property | Type | Constraint |
|---|---|---|
| `page` | `integer` (int32) | minimum 0 |
| `size` | `integer` (int32) | minimum 1 |
| `sort` | `array` of `string` | — |

When used as a method parameter (via `ParameterProcessorImpl`), the result is a single `query` parameter with a `$ref` to `Pageable`, which keeps the spec concise while preserving full schema metadata.

---

### `ModelConvertersTypeSchemaHandler`

**Handles:** All other types (catch-all)

Delegates to Swagger's `ModelConverters`, which resolves Java types to OpenAPI schemas via reflection. This handles all standard DTO classes, enums, collections, maps, and primitives.

After resolution, `ValidationSchemaEnricher` is invoked to propagate Jakarta Bean Validation constraints (e.g. `@Min`, `@Size`, `@NotBlank`) from field annotations to the corresponding schema properties. See [Architecture](Architecture.md#validationschemaenricher) for details.

Schemas generated here are registered in the shared schema registry and later merged into `components/schemas` by the `SchemaRegistryMergePostProcessor`.

---

## Adding a New Handler

See [CONTRIBUTING.md](../CONTRIBUTING.md#adding-a-new-schema-type-handler) for step-by-step instructions.
