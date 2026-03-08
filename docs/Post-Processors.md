# Post-Processors

Post-processors run after all controllers have been processed and the full `OpenAPI` model has been assembled. They clean up, merge, and finalise the model before it is serialised.

---

## Execution Order

```
SchemaRegistryMergePostProcessor
            │
            ▼
PruneUnreferencedSchemasPostProcessor
            │
            ▼
UniqueOperationIdPostProcessor
```

The order matters — schemas must be merged before they can be pruned, and operation IDs must be deduplicated last.

---

## Post-Processors

### `SchemaRegistryMergePostProcessor`

During controller and operation processing, `ModelConverters` registers resolved DTO schemas in a shared in-memory registry. This post-processor merges all those registered schemas into the `components/schemas` section of the final `OpenAPI` model.

**Why it runs first:** The pruning step needs all schemas to be present in `components/schemas` before it can check which ones are referenced.

---

### `PruneUnreferencedSchemasPostProcessor`

Scans all `$ref` values across the entire `OpenAPI` model (paths, request bodies, responses, and nested schemas) and builds a set of referenced schema names. Any schema in `components/schemas` that is not in that set is removed.

**Why:** `ModelConverters` sometimes registers more schemas than are actually used (e.g. intermediate types encountered during reflection). This step keeps the generated spec clean.

---

### `UniqueOperationIdPostProcessor`

Ensures every operation in the spec has a unique `operationId`. When the same method name appears in multiple controllers or via inheritance, duplicate IDs are disambiguated by appending a numeric suffix:

```
getById   →  getById
getById   →  getById_1
getById   →  getById_2
```

This strategy mirrors SpringDoc's own disambiguation approach, ensuring compatibility with clients that rely on operation IDs for code generation.

---

## Adding a New Post-Processor

See [CONTRIBUTING.md](../CONTRIBUTING.md#adding-a-new-post-processor) for step-by-step instructions.
