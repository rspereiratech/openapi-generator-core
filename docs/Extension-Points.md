# Extension Points

This page is aimed at developers who want to use `openapi-generator-core` as a library and extend it with custom behaviour. It covers the three main extension points: schema type handlers, post-processors, and annotation support.

---

## 1. Custom Schema Type Handler

Schema type handlers resolve Java `Type` objects to OpenAPI `Schema` objects. They form a chain of responsibility — each handler either handles the type or passes it to the next.

Add a custom handler when you need to control how a specific type is represented in the generated spec.

### Implement `TypeSchemaHandler`

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

### Wire into the chain

Extend `DefaultProcessorFactory` and override `createSchemaProcessor()`, inserting your handler before the catch-all `ModelConvertersTypeSchemaHandler`:

```java
public class MyProcessorFactory extends DefaultProcessorFactory {

    @Override
    public SchemaProcessor createSchemaProcessor() {
        return new SchemaProcessorImpl(List.of(
            new VoidTypeSchemaHandler(),
            new FluxTypeSchemaHandler(),
            new PageTypeSchemaHandler(),
            new PageableTypeSchemaHandler(),
            new MyTypeSchemaHandler(),          // inserted before catch-all
            new ModelConvertersTypeSchemaHandler()
        ));
    }
}
```

Pass your factory when constructing the generator:

```java
new OpenApiGeneratorImpl(new MyProcessorFactory()).generate(config, classLoader);
```

---

## 2. Custom Post-Processor

Post-processors run after all controllers have been processed and can modify the assembled `OpenAPI` model — adding, removing, or transforming any part of it.

### Implement `PostProcessor`

```java
public class MyPostProcessor implements PostProcessor {

    @Override
    public void process(OpenAPI openAPI) {
        // Example: add a custom extension to every operation
        if (openAPI.getPaths() == null) return;

        openAPI.getPaths().values().forEach(pathItem ->
            pathItem.readOperations().forEach(op ->
                op.addExtension("x-internal", false)
            )
        );
    }
}
```

### Register the post-processor

Extend `OpenApiGeneratorImpl` or pass the post-processor list directly if the implementation supports it:

```java
public class MyOpenApiGeneratorImpl extends OpenApiGeneratorImpl {

    @Override
    protected List<PostProcessor> postProcessors(OpenAPI openAPI, Map<String, Schema> schemaRegistry) {
        List<PostProcessor> processors = new ArrayList<>(super.postProcessors(openAPI, schemaRegistry));
        processors.add(new MyPostProcessor());
        return processors;
    }
}
```

---

## 3. Custom Controller Annotation

If your project uses a custom stereotype annotation that does **not** transitively meta-annotate `@RestController`, register it via `GeneratorConfig`:

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .controllerAnnotations(List.of("com.example.annotation.MyApiEndpoint"))
    .outputFile("docs/swagger/openapi.yaml")
    .title("My API")
    .version("1.0.0")
    .build();
```

If your annotation **does** transitively meta-annotate `@RestController` (via composed annotation), it is detected automatically — no configuration needed.

---

## 5. Custom Constraint Handler

`ValidationSchemaEnricher` uses a chain of `ConstraintHandler`s to map Jakarta Bean Validation annotations to OpenAPI schema properties. Add a custom handler to support annotations not covered by the default chain — for example, Hibernate Validator's `@Length`.

### Implement `ConstraintHandler`

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

### Wire into the enricher

Build a list that combines the built-in handlers with your custom one and pass it to `ValidationSchemaEnricher`:

```java
List<ConstraintHandler> handlers = new ArrayList<>();
handlers.addAll(ValidationSchemaEnricher.defaultHandlers()); // convenience method
handlers.add(new LengthConstraintHandler());

ValidationSchemaEnricher enricher = new ValidationSchemaEnricher(handlers);
```

Then supply the enricher to `ModelConvertersTypeSchemaHandler` and wire it via a custom `ProcessorFactory`:

```java
public class MyProcessorFactory extends DefaultProcessorFactory {

    @Override
    public SchemaProcessor createSchemaProcessor() {
        ValidationSchemaEnricher enricher = new ValidationSchemaEnricher(handlers);
        return new SchemaProcessorImpl(List.of(
            new VoidTypeSchemaHandler(),
            new FluxTypeSchemaHandler(),
            new PageTypeSchemaHandler(),
            new PageableTypeSchemaHandler(),
            new ModelConvertersTypeSchemaHandler(enricher)
        ));
    }
}
```

---

## 4. Custom HTTP Status Resolver

The `DefaultHttpStatusResolver` resolves HTTP status codes from `@ResponseStatus` or HTTP method defaults. To change this behaviour, implement `HttpStatusResolver`:

```java
public class MyHttpStatusResolver implements HttpStatusResolver {

    @Override
    public String resolve(Method method, String httpMethod) {
        // Custom logic — e.g. always use 200 for GET regardless of @ResponseStatus
        if ("GET".equals(httpMethod)) return "200";
        return new DefaultHttpStatusResolver().resolve(method, httpMethod);
    }
}
```

Wire it via a custom `ProcessorFactory` that passes your resolver to `ResponseProcessorImpl`.
