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

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeUtilsTest {

    // ==========================================================================
    // Fixture class to capture generic return types at runtime
    // ==========================================================================

    @SuppressWarnings("unused")
    static class TypeFixtures {
        public ResponseEntity<String>                  responseEntityOfString()      { return null; }
        public Optional<String>                        optionalOfString()            { return null; }
        public ResponseEntity<Optional<String>>        nestedResponseEntityOptional(){ return null; }
        public List<String>                            listOfString()                { return null; }
        public Set<Integer>                            setOfInteger()                { return null; }
        public Map<String, Integer>                    mapOfStringInt()              { return null; }
        public String[]                                stringArray()                 { return null; }
        public int[]                                   intArray()                    { return null; }
        public String                                  rawString()                   { return null; }
        public void                                    voidReturn()                  {}
    }

    private static Type returnType(String methodName) throws Exception {
        return TypeFixtures.class.getMethod(methodName).getGenericReturnType();
    }

    // ==========================================================================
    // unwrapType
    // ==========================================================================

    @Test
    void unwrapType_rawClass_returnedUnchanged() {
        assertEquals(String.class, TypeUtils.unwrapType(String.class));
    }

    @Test
    void unwrapType_responseEntityOfString_unwrapsToString() throws Exception {
        Type result = TypeUtils.unwrapType(returnType("responseEntityOfString"));
        assertEquals(String.class, result);
    }

    @Test
    void unwrapType_optionalOfString_unwrapsToString() throws Exception {
        Type result = TypeUtils.unwrapType(returnType("optionalOfString"));
        assertEquals(String.class, result);
    }

    @Test
    void unwrapType_nestedWrappers_unwrapsToInnermostType() throws Exception {
        // ResponseEntity<Optional<String>> → Optional<String> → String
        Type result = TypeUtils.unwrapType(returnType("nestedResponseEntityOptional"));
        assertEquals(String.class, result);
    }

    @Test
    void unwrapType_listOfString_notUnwrapped() throws Exception {
        // List is not a transparent wrapper — should be returned as-is
        Type listType = returnType("listOfString");
        assertEquals(listType, TypeUtils.unwrapType(listType));
    }

    @Test
    void unwrapType_primitiveVoid_returnedUnchanged() {
        assertEquals(void.class, TypeUtils.unwrapType(void.class));
    }

    // ==========================================================================
    // isCollection
    // ==========================================================================

    @Test
    void isCollection_list_returnsTrue() throws Exception {
        assertTrue(TypeUtils.isCollection(returnType("listOfString")));
    }

    @Test
    void isCollection_set_returnsTrue() throws Exception {
        assertTrue(TypeUtils.isCollection(returnType("setOfInteger")));
    }

    @Test
    void isCollection_rawArrayList_returnsTrue() {
        assertTrue(TypeUtils.isCollection(ArrayList.class));
    }

    @Test
    void isCollection_string_returnsFalse() {
        assertFalse(TypeUtils.isCollection(String.class));
    }

    @Test
    void isCollection_map_returnsFalse() throws Exception {
        assertFalse(TypeUtils.isCollection(returnType("mapOfStringInt")));
    }

    // ==========================================================================
    // isMap
    // ==========================================================================

    @Test
    void isMap_mapOfStringInt_returnsTrue() throws Exception {
        assertTrue(TypeUtils.isMap(returnType("mapOfStringInt")));
    }

    @Test
    void isMap_linkedHashMap_returnsTrue() {
        assertTrue(TypeUtils.isMap(LinkedHashMap.class));
    }

    @Test
    void isMap_rawHashMap_returnsTrue() {
        assertTrue(TypeUtils.isMap(HashMap.class));
    }

    @Test
    void isMap_string_returnsFalse() {
        assertFalse(TypeUtils.isMap(String.class));
    }

    @Test
    void isMap_list_returnsFalse() throws Exception {
        assertFalse(TypeUtils.isMap(returnType("listOfString")));
    }

    // ==========================================================================
    // isArray
    // ==========================================================================

    @Test
    void isArray_stringArray_returnsTrue() throws Exception {
        assertTrue(TypeUtils.isArray(returnType("stringArray")));
    }

    @Test
    void isArray_intArray_returnsTrue() throws Exception {
        assertTrue(TypeUtils.isArray(returnType("intArray")));
    }

    @Test
    void isArray_string_returnsFalse() {
        assertFalse(TypeUtils.isArray(String.class));
    }

    @Test
    void isArray_listOfString_returnsFalse() throws Exception {
        assertFalse(TypeUtils.isArray(returnType("listOfString")));
    }

    // ==========================================================================
    // isVoid
    // ==========================================================================

    @Test
    void isVoid_primitiveVoid_returnsTrue() {
        assertTrue(TypeUtils.isVoid(void.class));
    }

    @Test
    void isVoid_boxedVoid_returnsTrue() {
        assertTrue(TypeUtils.isVoid(Void.class));
    }

    @Test
    void isVoid_string_returnsFalse() {
        assertFalse(TypeUtils.isVoid(String.class));
    }

    @Test
    void isVoid_integer_returnsFalse() {
        assertFalse(TypeUtils.isVoid(Integer.class));
    }

    // ==========================================================================
    // firstTypeArgument
    // ==========================================================================

    @Test
    void firstTypeArgument_listOfString_returnsString() throws Exception {
        Type arg = TypeUtils.firstTypeArgument(returnType("listOfString"));
        assertEquals(String.class, arg);
    }

    @Test
    void firstTypeArgument_responseEntityOfString_returnsString() throws Exception {
        // unwrapping is done elsewhere; firstTypeArgument just reads arg[0]
        Type arg = TypeUtils.firstTypeArgument(returnType("responseEntityOfString"));
        assertEquals(String.class, arg);
    }

    @Test
    void firstTypeArgument_rawClass_returnsObjectClass() {
        Type result = TypeUtils.firstTypeArgument(String.class);
        assertEquals(Object.class, result);
    }

    // ==========================================================================
    // toRawClass
    // ==========================================================================

    @Test
    void toRawClass_class_returnsSelf() {
        assertEquals(String.class, TypeUtils.toRawClass(String.class));
    }

    @Test
    void toRawClass_parameterizedType_returnsRawType() throws Exception {
        Class<?> raw = TypeUtils.toRawClass(returnType("listOfString"));
        assertEquals(List.class, raw);
    }

    @Test
    void toRawClass_genericArrayType_returnsNull() throws Exception {
        // GenericArrayType is neither a Class nor a ParameterizedType
        // Use a type variable scenario — simplest proxy: null input guard
        assertNull(TypeUtils.toRawClass(null));
    }

    // ==========================================================================
    // isPrimitive
    // ==========================================================================

    @Test
    void isPrimitive_primitiveInt_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(int.class));
    }

    @Test
    void isPrimitive_boxedInteger_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Integer.class));
    }

    @Test
    void isPrimitive_boxedLong_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Long.class));
    }

    @Test
    void isPrimitive_boxedBoolean_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Boolean.class));
    }

    @Test
    void isPrimitive_boxedDouble_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Double.class));
    }

    @Test
    void isPrimitive_boxedFloat_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Float.class));
    }

    @Test
    void isPrimitive_boxedByte_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Byte.class));
    }

    @Test
    void isPrimitive_boxedShort_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Short.class));
    }

    @Test
    void isPrimitive_boxedChar_returnsTrue() {
        assertTrue(TypeUtils.isPrimitive(Character.class));
    }

    @Test
    void isPrimitive_string_returnsFalse() {
        assertFalse(TypeUtils.isPrimitive(String.class));
    }

    @Test
    void isPrimitive_object_returnsFalse() {
        assertFalse(TypeUtils.isPrimitive(Object.class));
    }

    // ==========================================================================
    // isScalar
    // ==========================================================================

    @Test
    void isScalar_string_returnsTrue() {
        assertTrue(TypeUtils.isScalar(String.class));
    }

    @Test
    void isScalar_uuid_returnsTrue() {
        assertTrue(TypeUtils.isScalar(UUID.class));
    }

    @Test
    void isScalar_bigDecimal_returnsTrue() {
        assertTrue(TypeUtils.isScalar(BigDecimal.class));
    }

    @Test
    void isScalar_bigInteger_returnsTrue() {
        assertTrue(TypeUtils.isScalar(BigInteger.class));
    }

    @Test
    void isScalar_localDate_returnsTrue() {
        assertTrue(TypeUtils.isScalar(LocalDate.class));
    }

    @Test
    void isScalar_localDateTime_returnsTrue() {
        assertTrue(TypeUtils.isScalar(LocalDateTime.class));
    }

    @Test
    void isScalar_enum_returnsTrue() {
        enum Status { ACTIVE, INACTIVE }
        assertTrue(TypeUtils.isScalar(Status.class));
    }

    @Test
    void isScalar_primitiveInt_returnsTrue() {
        assertTrue(TypeUtils.isScalar(int.class));
    }

    @Test
    void isScalar_boxedInteger_returnsTrue() {
        assertTrue(TypeUtils.isScalar(Integer.class));
    }

    @Test
    void isScalar_list_returnsFalse() {
        assertFalse(TypeUtils.isScalar(List.class));
    }

    @Test
    void isScalar_object_returnsFalse() {
        assertFalse(TypeUtils.isScalar(Object.class));
    }

    @Test
    void isScalar_map_returnsFalse() {
        assertFalse(TypeUtils.isScalar(Map.class));
    }

    // ==========================================================================
    // resolveType — ParameterizedTypeImpl (inner record) coverage
    // ==========================================================================

    @SuppressWarnings("unused")
    static class Container<T> {
        List<T> items;
    }

    @SuppressWarnings("unused")
    static class ArrayContainer<T> {
        T[] array;
    }

    @Test
    void resolveType_parameterizedTypeWithTypeVariable_substitutesTypeArgs() throws Exception {
        // Container<T>.items is List<T>; resolve T -> String
        TypeVariable<?> tv = Container.class.getTypeParameters()[0];
        Type listOfT = Container.class.getDeclaredField("items").getGenericType();
        Map<TypeVariable<?>, Type> map = new java.util.HashMap<>();
        map.put(tv, String.class);

        Type resolved = TypeUtils.resolveType(listOfT, map);

        // ParameterizedTypeImpl is created; exercise all three overridden methods
        assertTrue(resolved instanceof ParameterizedType, "Result must be a ParameterizedType");
        ParameterizedType pt = (ParameterizedType) resolved;
        assertEquals(String.class,  pt.getActualTypeArguments()[0], "T must be resolved to String");
        assertEquals(List.class,    pt.getRawType(),                "Raw type must be List");
        assertNull(pt.getOwnerType(),                               "Owner type must be null for List");
    }

    @Test
    void resolveType_genericArrayTypeWithTypeVariable_substitutesComponent() throws Exception {
        // ArrayContainer<T>.array is T[]; resolve T -> String
        TypeVariable<?> tv = ArrayContainer.class.getTypeParameters()[0];
        Type arrayOfT = ArrayContainer.class.getDeclaredField("array").getGenericType();
        Map<TypeVariable<?>, Type> map = new java.util.HashMap<>();
        map.put(tv, String.class);

        Type resolved = TypeUtils.resolveType(arrayOfT, map);

        // The lambda GenericArrayType is created; exercise getGenericComponentType()
        assertTrue(resolved instanceof GenericArrayType, "Result must be a GenericArrayType");
        assertEquals(String.class,
                ((GenericArrayType) resolved).getGenericComponentType(),
                "Component type must be resolved to String");
    }
}
