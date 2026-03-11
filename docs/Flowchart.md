# Generation Pipeline Flowchart

This page describes the complete execution flow of `OpenApiGeneratorImpl.generate()` from start to finish.

---

## Full Pipeline

```
START
  │
  ▼
┌─────────────────────────────────┐
│        OpenApiModelBuilder      │
│  - info (title, version, etc.)  │
│  - servers + contextPath        │
│  - securitySchemes              │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│      DefaultClasspathScanner    │
│  Scan basePackages for classes  │
│  annotated with @RestController │
│  @Controller or custom annots.  │
└────────────────┬────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ controllers[]  │  (list of discovered Class<?> objects)
        └───────┬────────┘
                │
         ┌──────▼──────┐
         │  for each   │
         │ controller  │◄─────────────────────────┐
         └──────┬──────┘                          │
                │                                 │
                ▼                                 │
┌───────────────────────────────────┐             │
│       ControllerProcessorImpl     │             │
│  Walk full type hierarchy:        │             │
│  - resolve base path              │             │
│  - collect @Tag annotations       │             │
│  - collect mapped methods         │             │
└───────────────┬───────────────────┘             │
                │                                 │
                ▼                                 │
       ┌────────────────┐                         │
       │  methods[]     │                         │
       └───────┬────────┘                         │
               │                                  │
        ┌──────▼──────┐                           │
        │  for each   │                           │
        │   method    │◄───────────────────┐      │
        └──────┬──────┘                    │      │
               │                           │      │
               ▼                           │      │
┌──────────────────────────────────┐       │      │
│       OperationProcessorImpl     │       │      │
│  - HTTP method + path            │       │      │
│  - summary + description         │       │      │
│  - tags (controller + method)    │       │      │
└──────────────┬───────────────────┘       │      │
               │                           │      │
       ┌───────┴──────────────┐            │      │
       │                      │            │      │
       ▼                      ▼            │      │
┌─────────────────┐  ┌────────────────────┐│      │
│ParameterProc.   │  │  ResponseProcessor ││      │
│                 │  │                    ││      │
│ @PathVariable   │  │ @ApiResponse /     ││      │
│  → path param   │  │ @ApiResponses      ││      │
│                 │  │                    ││      │
│ @RequestParam   │  │ fallback:          ││      │
│  → query param  │  │ @ResponseStatus    ││      │
│                 │  │ or HTTP defaults   ││      │
│ @RequestHeader  │  │ (POST→201,         ││      │
│  → header param │  │  void→204,         ││      │
│                 │  │  others→200)       ││      │
│ @CookieValue    │  └────────┬───────────┘│      │
│  → cookie param │           │            │      │
│                 │           ▼            │      │
│ Pageable        │  ┌────────────────────┐│      │
│  → query param  │  │  SchemaProcessor   ││      │
│   ($ref Pageable│  │  (chain of resp.)  ││      │
└─────────────────┘  │                    ││      │
                     │  void/Void → null  ││      │
       ▼             │  Flux<T> → array   ││      │
┌─────────────────┐  │  Page<T> → paged  ││      │
│RequestBodyProc. │  │  Pageable → params ││      │
│                 │  │  other → Model     ││      │
│ @RequestBody    │  │         Converters ││      │
│  → request body │  └────────────────────┘│      │
│    schema ref   │                        │      │
└─────────────────┘                        │      │
               │                           │      │
               └───────────── next method ─┘      │
                                                   │
               └──────────────── next controller ──┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│               Post-Processors                │
│                                              │
│  1. SchemaRegistryMergePostProcessor         │
│     Merge collected schemas into             │
│     components/schemas                       │
│                                              │
│  2. PruneUnreferencedSchemasPostProcessor    │
│     Remove schemas not referenced            │
│     by any $ref in the model                 │
│                                              │
│  3. SortSpecPostProcessor                    │
│     Sort paths and responses alphabetically  │
│     (enabled when sortOutput=true)           │
│                                              │
│  4. UniqueOperationIdPostProcessor           │
│     Disambiguate duplicate operationIds      │
│     by appending numeric suffix (_1, _2...)  │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────┐
│           OpenApiWriter          │
│  WriterFactory selects:          │
│  - YamlWriter  (outputFormat=YAML│
│  - JsonWriter  (outputFormat=JSON│
│  Writes to outputFile            │
└──────────────────────────────────┘
                   │
                   ▼
                  END
```

---

## Schema Handler Chain (detail)

```
SchemaProcessor.resolve(type)
        │
        ▼
┌───────────────────────┐
│  VoidTypeSchemaHandler │
│  type == void/Void?   │──── YES ──► return null (no schema)
└───────────┬───────────┘
            │ NO
            ▼
┌────────────────────────┐
│  FluxTypeSchemaHandler  │
│  type is Flux<T>?      │──── YES ──► unwrap T, return array schema
└───────────┬────────────┘
            │ NO
            ▼
┌───────────────────────┐
│  PageTypeSchemaHandler │
│  type is Page<T>?     │──── YES ──► build Page{T} schema (11 props)
└───────────┬───────────┘             + SortObject + PageableObject
            │ NO                      component schemas
            ▼
┌─────────────────────────────┐
│  PageableTypeSchemaHandler   │
│  type is Pageable/PageRequest│── YES ──► register Pageable component
└─────────────┬───────────────┘           schema; return $ref to it
              │ NO
              ▼
┌──────────────────────────────────┐
│  ModelConvertersTypeSchemaHandler │
│  (catch-all)                     │──► delegate to Swagger
└──────────────────────────────────┘    ModelConverters
                                        register in schema registry
```

---

## Type Hierarchy Walk (detail)

```
walk(ConcreteController)
  │
  ├─► visit(ConcreteController)
  │     collect: @RequestMapping, @Tag, methods
  │
  ├─► walk(SuperClass)
  │     ├─► visit(SuperClass)
  │     │     collect: @RequestMapping, @Tag, methods
  │     │
  │     ├─► walk(SuperClass.superclass) ... until Object
  │     │
  │     └─► visit(SuperClass.interfaces[])
  │           collect: @Tag, methods
  │
  └─► visit(ConcreteController.interfaces[])
        ├─► visit(InterfaceA)
        │     collect: @Tag, methods
        │     └─► visit(InterfaceA.superinterfaces[])
        │
        └─► visit(InterfaceB)
              collect: @Tag, methods
              └─► visit(InterfaceB.superinterfaces[])

Result: UNION of all @Tag annotations + ALL methods from all levels
```
