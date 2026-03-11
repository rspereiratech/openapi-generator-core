# Limitations

This page documents what `openapi-generator-core` does **not** support. Understanding these limitations avoids unexpected results and prevents unnecessary bug reports.

---

## Runtime-Only Behaviour

The generator works entirely from bytecode at build time. Anything that is determined at runtime cannot be captured:

- **`@ConditionalOnProperty` / `@ConditionalOnBean`** — conditionally registered beans are not evaluated. All controllers present in the compiled classes are scanned regardless of runtime conditions.
- **Programmatic route registration** — routes registered via `RouterFunction` (WebFlux functional style) or `RequestMappingHandlerMapping` at runtime are not detected.
- **Dynamic `@RequestMapping` values** — paths resolved from property placeholders (e.g. `@GetMapping("${my.path}")`) are not resolved. The literal placeholder string appears in the spec.

---

## WebFlux

- **Router functions** (`RouterFunction<ServerResponse>`) are not supported. Only annotation-based controllers (`@RestController`) are scanned.
- **`Mono<T>`** is not handled by a dedicated schema handler. It is passed to the catch-all `ModelConvertersTypeSchemaHandler`, which may not resolve it correctly in all cases.
- **`Flux<T>`** is supported — it is unwrapped to an array schema of `T`.

---

## Spring Features

- **`@RequestScope` / `@SessionScope`** — scope annotations have no effect on generation.
- **`@ExceptionHandler`** — exception handler methods are not included in the spec.
- **`@Validated` / `@Valid` constraints** — Bean Validation annotations (e.g. `@NotNull`, `@Size`) on method parameters are not reflected in the generated parameter schemas.
- **`@ModelAttribute`** — form-encoded request bodies via `@ModelAttribute` are not supported.
- **Interface default methods with `@RequestMapping`** — only methods reachable through the standard Java reflection hierarchy are collected. Interface default methods may not be detected in all cases.

---

## Schema Resolution

- **Circular references** — deeply nested circular type references may cause stack overflow during schema resolution via `ModelConverters`.
- **Generic types beyond one level** — `Page<List<OrderDto>>` or other multi-level generic nestings may not be resolved correctly.
- **`Map<K, V>` as response type** — map types are resolved as `additionalProperties` schemas, which may not always reflect the intended structure.
- **Polymorphism / inheritance in DTOs** — `@JsonSubTypes`, `@JsonTypeInfo`, and other Jackson polymorphism annotations are not explicitly handled. The generated schema reflects the declared Java type, not the full polymorphic hierarchy.

---

## Annotations

- **Custom `@Parameter` annotations** — only SpringDoc's `@Parameter` is recognised. Custom parameter annotations are ignored.
- **`@Hidden`** — SpringDoc's `@Hidden` annotation (to exclude operations from the spec) is not supported.
- **`@SecurityRequirement`** on individual methods — method-level security overrides are not supported. Security schemes are applied globally only.

---

## Output

- **Multiple output files** — the generator produces a single output file. Splitting the spec across multiple files is not supported.
- **OpenAPI versions other than 3.0** — only OpenAPI 3.0.x is produced. Swagger 2.0 output is not supported.
