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

- Java 17+
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

Schema type handlers implement the `TypeSchemaHandler` interface and form a chain of responsibility. Each handler either handles the given type or delegates to the next.

### 1. Implement `TypeSchemaHandler`

```java
public class MyTypeSchemaHandler implements TypeSchemaHandler {

    @Override
    public boolean supports(Type type) {
        if (!(type instanceof Class<?> clazz)) return false;
        return MySpecialType.class.isAssignableFrom(clazz);
    }

    @Override
    public Schema<?> resolve(Type type, SchemaProcessor schemaProcessor) {
        Schema<?> schema = new Schema<>();
        schema.setType("string");
        schema.setFormat("my-format");
        return schema;
    }
}
```

### 2. Register the handler in `DefaultProcessorFactory`

Add your handler to the list in `DefaultProcessorFactory.createSchemaProcessor()`, before the catch-all `ModelConvertersTypeSchemaHandler`:

```java
return new SchemaProcessorImpl(List.of(
    new VoidTypeSchemaHandler(),
    new FluxTypeSchemaHandler(),
    new PageTypeSchemaHandler(),
    new PageableTypeSchemaHandler(),
    new MyTypeSchemaHandler(),          // inserted before catch-all
    new ModelConvertersTypeSchemaHandler()
));
```

### 3. Write tests

Add a test class under `src/test/java/.../processor/schema/handlers/` following the pattern of the existing handler tests.

---

## Adding a New Constraint Handler

Constraint handlers implement the `ConstraintHandler` interface and are used by `ValidationSchemaEnricher` to map Jakarta Bean Validation annotations to OpenAPI schema properties.

Add a new handler when you need to support an annotation not covered by the default chain (e.g. Hibernate Validator `@Length`, `@Range`).

### 1. Implement `ConstraintHandler`

```java
public class LengthConstraintHandler implements ConstraintHandler {

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Length;
    }

    @Override
    public void apply(Annotation annotation, Type fieldType, Schema<?> property) {
        Length length = (Length) annotation;
        if (length.min() > 0)                property.setMinLength(length.min());
        if (length.max() < Integer.MAX_VALUE) property.setMaxLength(length.max());
    }
}
```

### 2. Wire it into `ValidationSchemaEnricher`

Pass the handler via a custom `ProcessorFactory` (see [Extension-Points.md](docs/Extension-Points.md#4-custom-constraint-handler)).

### 3. Write tests

Add a test class under `src/test/java/.../processor/schema/constraints/` following the pattern of `ConstraintHandlersTest`.

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

### 2. Register in `DefaultProcessorFactory`

Add your post-processor to the list in `DefaultProcessorFactory.createPostProcessors()`:

```java
@Override
public List<PostProcessor> createPostProcessors(SchemaProcessor schemaProcessor, boolean sortOutput) {
    return List.of(
        new SchemaRegistryMergePostProcessor(schemaProcessor),
        new PruneUnreferencedSchemasPostProcessor(),
        new SortSpecPostProcessor(sortOutput),
        new UniqueOperationIdPostProcessor(),
        new MyPostProcessor()   // add here
    );
}
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
- Schema type handlers go under `processor/schema/handlers/`.
- Constraint handlers go under `processor/schema/constraints/`.
- Post-processors go under `postprocessor/`.
- Utilities go under `utils/`.
- All public classes and methods must have Javadoc.
- Tests use JUnit 5. Test methods are grouped by scenario using `// ===` section-separator comments rather than `@Nested` classes.

---

## Submitting a Pull Request

1. Fork the repository and create a branch from `master`.
2. Implement your changes with tests.
3. Ensure `mvn test` passes.
4. Open a pull request with a clear description of what was added and why.
