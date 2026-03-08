# Extension Points

This page is aimed at developers who want to use `openapi-generator-core` as a library and extend it with custom behaviour. It covers the three main extension points: schema type handlers, post-processors, and annotation support.

---

## 1. Custom Schema Type Handler

Schema type handlers resolve Java `Type` objects to OpenAPI `Schema` objects. They form a chain of responsibility — each handler either handles the type or passes it to the next.

Add a custom handler when you need to control how a specific type is represented in the generated spec.

### Implement `TypeSchemaHandler`

```java
public class MyTypeSchemaHandler implements TypeSchemaHandler {

    private TypeSchemaHandler next;

    @Override
    public void setNext(TypeSchemaHandler next) {
        this.next = next;
    }

    @Override
    public Schema<?> handle(Type type, ModelConverters converters) {
        if (!isMyType(type)) {
            return next != null ? next.handle(type, converters) : null;
        }

        // Build and return the schema for this type
        Schema<?> schema = new Schema<>();
        schema.setType("string");
        schema.setFormat("my-format");
        return schema;
    }

    private boolean isMyType(Type type) {
        if (!(type instanceof Class<?> clazz)) return false;
        return MySpecialType.class.isAssignableFrom(clazz);
    }
}
```

### Wire into the chain

Extend `DefaultProcessorFactory` and override `createSchemaProcessor()`, inserting your handler before the catch-all `ModelConvertersTypeSchemaHandler`:

```java
public class MyProcessorFactory extends DefaultProcessorFactory {

    @Override
    public SchemaProcessor createSchemaProcessor() {
        TypeSchemaHandler modelConverters = new ModelConvertersTypeSchemaHandler();
        TypeSchemaHandler pageable     = new PageableTypeSchemaHandler();
        TypeSchemaHandler page         = new PageTypeSchemaHandler();
        TypeSchemaHandler flux         = new FluxTypeSchemaHandler();
        TypeSchemaHandler voidHandler  = new VoidTypeSchemaHandler();
        TypeSchemaHandler myHandler    = new MyTypeSchemaHandler();

        // Build the chain: void → flux → page → pageable → my → modelConverters
        pageable.setNext(myHandler);
        myHandler.setNext(modelConverters);
        page.setNext(pageable);
        flux.setNext(page);
        voidHandler.setNext(flux);

        return new SchemaProcessorImpl(voidHandler);
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
