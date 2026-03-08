/*
 *   ___                   _   ___ ___
 *  / _ \ _ __  ___ _ _   /_\ | _ \_ _|
 * | (_) | '_ \/ -_) ' \ / _ \|  _/| |
 *  \___/| .__/\___|_||_/_/ \_\_| |___|   Generator
 *       |_|
 *
 * MIT License - Copyright (c) 2026 Rui Pereira
 * See LICENSE in the project root for full license information.
 */
package io.github.rspereiratech.openapi.generator.core.utils;

import lombok.NoArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utility methods for working with {@link java.lang.reflect.Type} instances.
 *
 * <p>This class provides helpers for two primary concerns:
 * <ol>
 *   <li><b>Unwrapping</b> – stripping transparent wrapper types such as
 *       {@code ResponseEntity<T>}, {@code Optional<T>}, and {@code Mono<T>}
 *       to reach the actual body type that should appear in the OpenAPI schema.</li>
 *   <li><b>Classification</b> – determining whether a type is a collection,
 *       a map, an array, a primitive/scalar, or {@code void}.</li>
 * </ol>
 *
 * <p>All methods are stateless and thread-safe.  The class cannot be instantiated.
  *
 * @author ruispereira
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TypeUtils {
    /** Well-known wrapper types whose inner generic argument is the real body type. */
    private static final java.util.Set<String> TRANSPARENT_WRAPPERS = java.util.Set.of(
            "org.springframework.http.ResponseEntity",
            "org.springframework.http.HttpEntity",
            "java.util.Optional",
            "reactor.core.publisher.Mono",
            "reactor.core.publisher.Flux"  // Flux is treated as an array in the response processor
    );

    /**
     * Unwraps {@code ResponseEntity<T>}, {@code Optional<T>}, {@code Mono<T>}, etc.
     * and returns the innermost meaningful type.
     *
     * @param type the raw return type from a controller method
     * @return the unwrapped type, or the original if no unwrapping applies
     */
    public static Type unwrapType(Type type) {
        if (!(type instanceof ParameterizedType pt)) return type;

        Type rawType = pt.getRawType();
        if (!(rawType instanceof Class<?> rawClass)) return type;

        if (TRANSPARENT_WRAPPERS.contains(rawClass.getName())) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                return unwrapType(args[0]); // recurse for nested wrappers
            }
        }
        return type;
    }

    /**
     * Returns {@code true} if the raw erasure of {@code type} is a {@link Collection} subtype
     * (e.g. {@code List}, {@code Set}, {@code Queue}).
     *
     * @param type the type to test; may be a raw class or a parameterised type
     * @return {@code true} if the type is assignable to {@link Collection}
     */
    public static boolean isCollection(Type type) {
        Class<?> raw = toRawClass(type);
        return raw != null && Collection.class.isAssignableFrom(raw);
    }

    /**
     * Returns {@code true} if the raw erasure of {@code type} is a {@link Map} subtype
     * (e.g. {@code HashMap}, {@code LinkedHashMap}).
     *
     * @param type the type to test; may be a raw class or a parameterised type
     * @return {@code true} if the type is assignable to {@link Map}
     */
    public static boolean isMap(Type type) {
        Class<?> raw = toRawClass(type);
        return raw != null && Map.class.isAssignableFrom(raw);
    }

    /**
     * Returns {@code true} if the type represents a Java array (including multi-dimensional arrays).
     *
     * @param type the type to test
     * @return {@code true} if {@code type} is a {@link Class} and {@link Class#isArray()} is true
     */
    public static boolean isArray(Type type) {
        return type instanceof Class<?> c && c.isArray();
    }

    /**
     * Returns {@code true} for types that should produce no response body schema,
     * i.e. the primitive {@code void} or the boxed {@link Void} wrapper.
     *
     * @param type the type to test
     * @return {@code true} if the type represents an absence of a value
     */
    public static boolean isVoid(Type type) {
        return type == void.class || type == Void.class;
    }

    /**
     * Extracts the first generic type argument from a parameterised type.
     * Returns {@code Object.class} if none is found.
     */
    public static Type firstTypeArgument(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) return args[0];
        }
        return Object.class;
    }

    /**
     * Returns the raw {@link Class} for a given {@link Type},
     * or {@code null} if it cannot be determined.
     */
    public static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) return raw;
        return null;
    }

    // ------------------------------------------------------------------
    // Type variable resolution
    // ------------------------------------------------------------------

    /**
     * Builds a map from every {@link TypeVariable} in the type hierarchy of
     * {@code concreteClass} to its concrete {@link Type}.
     *
     * <p>Example: for {@code TenantRestControllerImpl extends
     * GenericVertexRestControllerImpl<TenantDTO, String, Tenant>} the map will
     * contain entries for the type variables declared on
     * {@code GenericVertexRestControllerImpl} ({@code T→TenantDTO},
     * {@code I→String}, {@code D→Tenant}) and for those declared on any
     * further super-types or interfaces.
     *
     * @param concreteClass the concrete (non-generic) class to analyse
     * @return a mutable mapping that is safe to pass by reference
     */
    public static Map<TypeVariable<?>, Type> buildTypeVariableMap(Class<?> concreteClass) {
        Map<TypeVariable<?>, Type> map = new LinkedHashMap<>();
        walkHierarchy(concreteClass, map);
        return map;
    }

    /**
     * Substitutes any {@link TypeVariable}s in {@code type} using {@code map},
     * recursing into {@link ParameterizedType} arguments and
     * {@link GenericArrayType} components.
     *
     * @param type the type to resolve; may be {@code null}
     * @param map  the type-variable → concrete-type mapping
     * @return the resolved type, or the original if no substitution was needed
     */
    public static Type resolveType(Type type, Map<TypeVariable<?>, Type> map) {
        if (map.isEmpty()) return type;

        if (type instanceof TypeVariable<?> tv) {
            return map.getOrDefault(tv, tv);
        }

        if (type instanceof ParameterizedType pt) {
            Type[] original = pt.getActualTypeArguments();
            Type[] resolved = new Type[original.length];
            boolean changed = false;
            for (int i = 0; i < original.length; i++) {
                resolved[i] = resolveType(original[i], map);
                if (resolved[i] != original[i]) changed = true;
            }
            if (!changed) return type;
            return new ParameterizedTypeImpl(pt.getRawType(), resolved, pt.getOwnerType());
        }

        if (type instanceof GenericArrayType gat) {
            Type component = resolveType(gat.getGenericComponentType(), map);
            if (component == gat.getGenericComponentType()) return type;
            return (GenericArrayType) () -> component;
        }

        return type;
    }

    // -- type-variable map builder internals --

    /**
     * Recursively walks the class hierarchy (superclass and interfaces) of the given class,
     * collecting type variable bindings into {@code map}.
     *
     * @param clazz the class to inspect; stops when {@code null} or {@link Object}
     * @param map   accumulator mapping each {@link TypeVariable} to its resolved {@link Type}
     */
    private static void walkHierarchy(Class<?> clazz, Map<TypeVariable<?>, Type> map) {
        if (clazz == null || clazz == Object.class) return;

        Stream.concat(
                Stream.ofNullable(clazz.getGenericSuperclass()),
                Arrays.stream(clazz.getGenericInterfaces()))
            .forEach(type -> collectTypeVars(type, map));
    }

    /**
     * Extracts type variable bindings from a single generic type and recurses up its hierarchy.
     * <p>
     * If {@code genericType} is a {@link ParameterizedType}, maps each of the raw class's
     * {@link TypeVariable}s to the corresponding actual type argument (resolved against
     * already-known bindings). Then recurses via {@link #walkHierarchy} to collect bindings
     * from the raw class's own superclass and interfaces.
     *
     * @param genericType the generic supertype or interface to inspect
     * @param map         accumulator mapping each {@link TypeVariable} to its resolved {@link Type};
     *                    existing entries are never overwritten ({@code putIfAbsent} semantics)
     */
    private static void collectTypeVars(Type genericType, Map<TypeVariable<?>, Type> map) {
        if (!(genericType instanceof ParameterizedType pt)) return;
        if (!(pt.getRawType() instanceof Class<?> rawClass)) return;

        TypeVariable<?>[] typeParams = rawClass.getTypeParameters();
        Type[] actualArgs = pt.getActualTypeArguments();

        // Resolve each arg in case it is itself a TypeVariable already in the map.
        IntStream.range(0, Math.min(typeParams.length, actualArgs.length))
            .forEach(i -> map.putIfAbsent(typeParams[i], resolveType(actualArgs[i], map)));

        walkHierarchy(rawClass, map);
    }

    /**
     * Minimal {@link ParameterizedType} implementation used to reconstruct generic types
     * after type-variable resolution.
     *
     * @param rawType              the raw (erased) class, e.g. {@code List.class}
     * @param actualTypeArguments  the resolved type arguments, e.g. {@code [String.class]}
     * @param ownerType            the enclosing type, or {@code null} for top-level classes
     */
    private record ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type ownerType)
            implements ParameterizedType {
        @Override public Type[] getActualTypeArguments() { return actualTypeArguments; }
        @Override public Type  getRawType()               { return rawType; }
        @Override public Type  getOwnerType()             { return ownerType; }
    }

    // ------------------------------------------------------------------
    // Type hierarchy traversal
    // ------------------------------------------------------------------

    /**
     * Returns all ancestor types of {@code clazz} — abstract superclasses and interfaces
     * (including super-interfaces) — in priority order:
     * <ol>
     *   <li>Superclass chain, most specific first (e.g. {@code AbstractBase} before
     *       {@code AbstractRoot}), with each superclass's interfaces interleaved.</li>
     *   <li>Interfaces declared directly on {@code clazz} (and their super-interfaces).</li>
     * </ol>
     * {@code Object} is always excluded. Each type appears at most once.
     *
     * @param clazz the class whose ancestors to collect
     * @return an ordered, deduplicated list of ancestor types
     */
    public static List<Class<?>> getAncestorTypes(Class<?> clazz) {
        List<Class<?>> result = new ArrayList<>();
        Set<Class<?>> seen = new LinkedHashSet<>();

        // Superclass chain (abstract base controllers, etc.)
        for (Class<?> sc = clazz.getSuperclass(); sc != null && sc != Object.class; sc = sc.getSuperclass()) {
            if (seen.add(sc)) {
                result.add(sc);
                collectInterfaces(sc, seen, result);
            }
        }

        // Direct interfaces of the original class (+ their super-interfaces)
        collectInterfaces(clazz, seen, result);

        return result;
    }

    /**
     * Finds the corresponding method in {@code ancestor} for a given concrete {@code method}.
     * <p>
     * Two strategies are tried in order:
     * <ol>
     *   <li><b>Exact match</b> – {@code ancestor.getMethod(name, paramTypes)}.
     *       Works for non-generic ancestors and interfaces.</li>
     *   <li><b>Erased match</b> – scans {@code ancestor}'s public methods for a
     *       non-bridge, non-synthetic method with the same name and parameter count.
     *       Needed when the ancestor is a generic class (e.g. {@code AbstractCrudApi<T, ID>})
     *       because type arguments are erased to {@code Object} in bytecode, so the concrete
     *       parameter type ({@code Long}) differs from the erased one ({@code Object}).</li>
     * </ol>
     *
     * @param ancestor the superclass or interface to search in
     * @param method   the concrete method to find a counterpart for
     * @return the matching ancestor method, or {@code null} if not found
     */
    public static Method findMethodInAncestor(Class<?> ancestor, Method method) {
        // Exact signature (works for non-generic and interface cases)
        try {
            return ancestor.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ignored) {}

        // Erased-type fallback (handles AbstractFoo<T,ID> patterns)
        return Arrays.stream(ancestor.getMethods())
                .filter(c -> !c.isBridge() && !c.isSynthetic())
                .filter(c -> c.getName().equals(method.getName()))
                .filter(c -> c.getParameterCount() == method.getParameterCount())
                .findFirst()
                .orElse(null);
    }

    /**
     * Recursively collects the interfaces of {@code clazz} into {@code result},
     * including super-interfaces at every level.
     * <p>
     * {@code seen} acts as a visited set to prevent duplicates and cycles
     * (e.g. two classes implementing the same interface).
     *
     * @param clazz  the class or interface whose interfaces to collect
     * @param seen   visited set; an interface is only added if not already present
     * @param result accumulator receiving interfaces in breadth-first declaration order
     */
    private static void collectInterfaces(Class<?> clazz,
                                          Set<Class<?>> seen,
                                          List<Class<?>> result) {
        Arrays.stream(clazz.getInterfaces())
                .filter(seen::add)
                .forEach(iface -> {
                    result.add(iface);
                    collectInterfaces(iface, seen, result);
                });
    }

    /**
     * Returns {@code true} if the class is a Java primitive type or its boxed wrapper.
     *
     * <p>Covered types: {@code boolean}/{@code Boolean}, {@code byte}/{@code Byte},
     * {@code short}/{@code Short}, {@code int}/{@code Integer}, {@code long}/{@code Long},
     * {@code float}/{@code Float}, {@code double}/{@code Double},
     * {@code char}/{@code Character}.
     *
     * @param clazz the class to test; must not be {@code null}
     * @return {@code true} if {@code clazz} is a primitive or its wrapper
     */
    public static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == Boolean.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == Character.class;
    }

    /**
     * Returns {@code true} for types that map directly to a scalar OpenAPI schema
     * (i.e. no {@code $ref} is needed).
     *
     * <p>Scalar types include:
     * <ul>
     *   <li>All primitives and their wrappers (see {@link #isPrimitive(Class)})</li>
     *   <li>{@code String} / {@code CharSequence}</li>
     *   <li>Types that implement {@link java.time.temporal.Temporal}
     *       (e.g. {@code LocalDate}, {@code Instant})</li>
     *   <li>{@link java.util.Date} and its subtypes</li>
     *   <li>{@link java.util.UUID}</li>
     *   <li>{@link java.math.BigDecimal} and {@link java.math.BigInteger}</li>
     *   <li>Enum types</li>
     * </ul>
     *
     * @param clazz the class to test; must not be {@code null}
     * @return {@code true} if the class can be represented as a scalar OpenAPI type
     */
    public static boolean isScalar(Class<?> clazz) {
        return isPrimitive(clazz)
                || clazz == String.class
                || clazz == CharSequence.class
                || java.time.temporal.Temporal.class.isAssignableFrom(clazz)
                || java.util.Date.class.isAssignableFrom(clazz)
                || java.util.UUID.class == clazz
                || java.math.BigDecimal.class == clazz
                || java.math.BigInteger.class == clazz
                || clazz.isEnum();
    }
}
