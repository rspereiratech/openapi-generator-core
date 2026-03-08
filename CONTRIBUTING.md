# Contributing

Thank you for your interest in contributing to `openapi-generator-core`.

This module contains all the generation logic. The most common types of contributions are adding a new schema type handler, adding a new post-processor, or extending annotation support.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building and Testing](#building-and-testing)
- [Adding a New Schema Type Handler](#adding-a-new-schema-type-handler)
- [Adding a New Post-Processor](#adding-a-new-post-processor)
- [Extending Annotation Support](#extending-annotation-support)
- [Code Style](#code-style)
- [Submitting a Pull Request](#submitting-a-pull-request)

---

## Prerequisites

- Java 21+
- Maven 3.9+
- `openapi-generator-parent` installed locally

---

## Building and Testing

```bash
mvn install -f ../openapi-generator-parent/pom.xml
mvn test
```

---

## Adding a New Schema Type Handler

Schema type handlers implement the `TypeSchemaHandler` interface and form a chain of responsibility. Each handler either handles the given type or delegates to the next handler in the chain.

### 1. Implement `TypeSchemaHandler`

```java
public class MyTypeSchemaHandler implements TypeSchemaHandler {

    private TypeSchemaHandler next;

    @Override
    public void setNext(TypeSchemaHandler next) {
        this.next = next;
    }

    @Override
    public Schema<?> handle(Type type, ModelConverters converters) {
        if (/* this handler can handle the type */) {
            // build and return the schema
        }
        return next != null ? next.handle(type, converters) : null;
    }
}
```

### 2. Register the handler in `DefaultProcessorFactory`

Add your handler to the chain in `DefaultProcessorFactory.createSchemaProcessor()`, before the catch-all `ModelConvertersTypeSchemaHandler`:

```java
TypeSchemaHandler myHandler = new MyTypeSchemaHandler();
myHandler.setNext(modelConvertersHandler);
// insert your handler at the appropriate position in the chain
```

### 3. Write tests

Add a test class under `src/test/java/.../processor/schema/handlers/` following the pattern of the existing handler tests.

---

## Adding a New Post-Processor

Post-processors implement the `PostProcessor` interface and run after all controllers have been processed.

### 1. Implement `PostProcessor`

```java
public class MyPostProcessor implements PostProcessor {

    @Override
    public void process(OpenAPI openAPI) {
        // modify the OpenAPI model
    }
}
```

### 2. Register in `OpenApiGeneratorImpl`

Add your post-processor to the list in `OpenApiGeneratorImpl`:

```java
List<PostProcessor> postProcessors = List.of(
    new SchemaRegistryMergePostProcessor(registry),
    new PruneUnreferencedSchemasPostProcessor(),
    new UniqueOperationIdPostProcessor(),
    new MyPostProcessor()   // add here
);
```

### 3. Write tests

Add a test class under `src/test/java/.../postprocessor/`.

---

## Extending Annotation Support

Annotation resolution is handled by `AnnotationUtils` and `AnnotationAttributeUtils`.

- To support a new annotation on controller methods, extend `OperationProcessorImpl`.
- To support a new parameter annotation, extend `ParameterProcessorImpl`.
- To support a new controller stereotype annotation, no code change is needed — the scanner already handles it via `controllerAnnotations` configuration or automatic meta-annotation traversal.

Always write a test that verifies the annotation is correctly resolved in the generated spec.

---

## Code Style

- Follow the existing package structure — one interface and one `*Impl` implementation per processor.
- Chain-of-responsibility handlers go under `processor/schema/handlers/`.
- Post-processors go under `postprocessor/`.
- Utilities go under `utils/`.
- All public classes and methods must have Javadoc.
- Tests use JUnit 5 with `@DisplayName` and `@Nested` grouped by method or scenario.

---

## Submitting a Pull Request

1. Fork the repository and create a branch from `master`.
2. Implement your changes with tests.
3. Ensure `mvn test` passes.
4. Update `CHANGELOG.md` under a new version entry.
5. Open a pull request with a clear description of what was added and why.
