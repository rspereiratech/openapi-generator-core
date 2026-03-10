# Configuration

`GeneratorConfig` is an immutable record built via a fluent builder. It is the single object passed from the Maven plugin (or any other caller) to the core generation pipeline.

---

## Building a Config

```java
GeneratorConfig config = GeneratorConfig.builder()
    .basePackages(List.of("com.example.controller"))
    .outputFile("docs/swagger/openapi.yaml")
    .title("My API")
    .version("1.0.0")
    .build();
```

---

## Fields

### `basePackages`

`List<String>` — **required**

Packages to scan for controller classes. All subpackages are included automatically.

```java
.basePackages(List.of("com.example.controller", "com.example.api"))
```

---

### `outputFile`

`String` — default: `docs/swagger/openapi.yaml`

Absolute or relative path where the generated file is written. The parent directories are created if they do not exist.

```java
.outputFile("target/openapi/spec.yaml")
```

---

### `title`, `description`, `version`

`String` — written into the `info` block.

```java
.title("Order Service API")
.description("Manages customer orders")
.version("2.0.0")
```

---

### `contactName`, `contactEmail`, `contactUrl`

`String` — written into `info.contact`. All optional.

```java
.contactName("API Team")
.contactEmail("api@example.com")
.contactUrl("https://developer.example.com")
```

---

### `licenseName`, `licenseUrl`

`String` — written into `info.license`. Both optional.

```java
.licenseName("Apache 2.0")
.licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
```

---

### `servers`

`List<ServerConfig>` — server environments.

```java
.server(ServerConfig.of("https://api.example.com", "Production"))
.server(ServerConfig.of("http://localhost:8080", "Local"))
```

If no servers are configured, a default server with URL `/` is used.

---

### `contextPath`

`String` — optional. Appended to every server URL as a path segment.

```java
.contextPath("my-api")
// https://api.example.com  →  https://api.example.com/my-api/
```

---

### `securitySchemes`

`List<SecuritySchemeConfig>` — security schemes added to `components/securitySchemes` and the root `security` list.

```java
.securityScheme(new SecuritySchemeConfig(
    "bearerAuth", "http", "bearer", "JWT", null, null, null, null
))
```

`SecuritySchemeConfig` fields: `name`, `type`, `scheme`, `bearerFormat`, `description`, `in`, `parameterName`, `openIdConnectUrl`.

---

### `controllerAnnotations`

`List<String>` — fully-qualified class names of additional annotations to treat as controller stereotypes.

```java
.controllerAnnotations(List.of("com.example.annotation.MyApiEndpoint"))
```

Annotations that already transitively meta-annotate `@RestController` are detected automatically without this setting.

---

### `outputFormat`

`OutputFormat` — `YAML` (default) or `JSON`.

```java
.outputFormat(OutputFormat.JSON)
```

---

### `sortOutput`

`boolean` — default: `false`

When `true`, enables deterministic output:

- Controllers are sorted alphabetically by canonical class name before processing.
- The `paths` block is sorted alphabetically after all controllers are processed.

This guarantees that the generated spec is byte-for-byte identical across machines and builds, regardless of filesystem or JVM ordering. Useful for version-controlled spec files where noise-free diffs matter.

```java
.sortOutput(true)
```

---

### `ignoreDefaultParamTypes`

`boolean` — default: `true`

When `true`, the following framework-injected parameter types are silently skipped and never appear as OpenAPI parameters:

| Type | Package |
|---|---|
| `Locale` | `java.util` |
| `Principal` | `java.security` |
| `HttpServletRequest` | `jakarta.servlet.http` |
| `HttpServletResponse` | `jakarta.servlet.http` |
| `HttpSession` | `jakarta.servlet.http` |
| `ServletRequest` | `jakarta.servlet` |
| `ServletResponse` | `jakarta.servlet` |
| `WebRequest` | `org.springframework.web.context.request` |
| `NativeWebRequest` | `org.springframework.web.context.request` |
| `BindingResult` | `org.springframework.validation` |
| `Errors` | `org.springframework.validation` |
| `Model` | `org.springframework.ui` |
| `ModelMap` | `org.springframework.ui` |

Set to `false` to disable this behaviour and include all parameter types as-is:

```java
.ignoreDefaultParamTypes(false)
```

---

### `additionalIgnoredParamTypes`

`List<String>` — default: _(empty)_

Fully-qualified class names of extra parameter types to ignore, applied on top of the built-in defaults (when `ignoreDefaultParamTypes` is `true`). Useful for project-specific types that should never be exposed as OpenAPI parameters.

```java
.additionalIgnoredParamType("com.example.security.TenantContext")
.additionalIgnoredParamType("com.example.audit.AuditContext")
// or replace the entire list
.additionalIgnoredParamTypes(List.of("com.example.security.TenantContext"))
```

---

### `defaultProducesMediaType`

`String` — default: `*/*`

The fallback media type used in response `content` blocks when no `produces` attribute is declared on the mapping annotation and no `@Content(mediaType = ...)` is specified.

Mirrors `springdoc.default-produces-media-type`. Explicit `produces` values always take precedence.

```java
.defaultProducesMediaType("application/json")
```

---

### `defaultConsumesMediaType`

`String` — default: `application/json`

The fallback media type used in request body `content` blocks when no `consumes` attribute is declared on the mapping annotation and no `@Content(mediaType = ...)` is specified.

Mirrors `springdoc.default-consumes-media-type`. Explicit `consumes` values always take precedence.

```java
.defaultConsumesMediaType("application/xml")
```
