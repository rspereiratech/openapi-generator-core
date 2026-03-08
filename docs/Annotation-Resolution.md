# Annotation Resolution

One of the core capabilities of this library is resolving annotations across the full Java type hierarchy, including composed (meta-annotated) annotations. This page explains how it works.

---

## Recursive Meta-Annotation Traversal

Java's `Class.getAnnotation(Class)` only finds annotations **directly present** on a class. It does not walk the annotation graph.

This library uses `AnnotationUtils` to perform recursive traversal. Given a class `C` and an annotation type `A`, it:

1. Checks if `A` is directly present on `C`.
2. If not, iterates over all annotations on `C` and recursively searches their annotation graphs.
3. Stops when `A` is found or all paths are exhausted.

### Example

```java
@RestController   // meta-annotation
@RequestMapping
@Tag(name = "")
public @interface CustomRestController { ... }

@CustomRestController(value = "/api/v1/orders", name = "orders")
public class OrderController { ... }
```

`OrderController` does not have `@RestController` directly, but `AnnotationUtils.findAnnotation(OrderController.class, RestController.class)` returns the annotation because it walks the annotation graph of `@CustomRestController`.

---

## Full Hierarchy Collection

For collecting annotations like `@Tag` that may appear at multiple levels, `AnnotationUtils` provides methods that walk the **full type hierarchy**:

```
ConcreteController
  └─ extends AbstractBase
       └─ implements InterfaceA   ← @Tag("Tag A")
  └─ implements InterfaceB        ← @Tag("Tag B")
       └─ extends InterfaceC      ← @Tag("Tag A")  (same interface reached via two paths)
```

All `@Tag` annotations from all reachable types are collected and unioned. The same interface reached via two different inheritance paths does **not** cause deduplication of the tags from the second path — all tags are preserved.

This is the mechanism that enables the multi-tag scenario in `AgentController` (see the [samples repository](https://github.com/rspereiratech/openapi-generator-samples)).

---

## Annotation Attribute Resolution

`AnnotationAttributeUtils` reads attribute values from annotations reflectively. It supports **attribute aliasing**: when a composed annotation's attribute maps to an attribute of a meta-annotation, the value is read from the correct level.

### Example

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping        // meta-annotation
public @interface CustomRestController {
    String[] value() default {};  // aliased to @RequestMapping.value()
}
```

When `AnnotationAttributeUtils` reads the `value` attribute of `@CustomRestController`, it resolves it to the `@RequestMapping.value()` binding, so the base path is correctly extracted even though it is declared on the composed annotation.

---

## Type Hierarchy Walk

`ControllerProcessorImpl` uses `AnnotationUtils` to walk the full type hierarchy of each controller class. The walk visits:

1. The concrete class itself
2. Each superclass, recursively up to `Object`
3. All interfaces implemented by each class in the chain, recursively

This guarantees that operations and tags declared anywhere in the hierarchy — abstract base classes, interfaces, interface superinterfaces — are all collected.

```
walk(ConcreteController)
  ├─ visit(ConcreteController)
  ├─ walk(AbstractBase)               ← superclass
  │    ├─ visit(AbstractBase)
  │    ├─ walk(Object)                ← stops here
  │    └─ visit(InterfaceA)           ← interface of superclass
  └─ visit(InterfaceB)                ← direct interface
       └─ visit(InterfaceC)           ← superinterface
```
