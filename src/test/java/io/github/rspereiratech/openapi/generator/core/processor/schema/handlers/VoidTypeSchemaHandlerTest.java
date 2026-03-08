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
package io.github.rspereiratech.openapi.generator.core.processor.schema.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoidTypeSchemaHandlerTest {

    private VoidTypeSchemaHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VoidTypeSchemaHandler();
    }

    // ==========================================================================
    // supports()
    // ==========================================================================

    @Test
    void supports_primitiveVoid_returnsTrue() {
        assertTrue(handler.supports(void.class));
    }

    @Test
    void supports_boxedVoid_returnsTrue() {
        assertTrue(handler.supports(Void.class));
    }

    @Test
    void supports_string_returnsFalse() {
        assertFalse(handler.supports(String.class));
    }

    @Test
    void supports_integer_returnsFalse() {
        assertFalse(handler.supports(Integer.class));
    }

    @Test
    void supports_object_returnsFalse() {
        assertFalse(handler.supports(Object.class));
    }

    @Test
    void supports_list_returnsFalse() {
        assertFalse(handler.supports(List.class));
    }

    // ==========================================================================
    // resolve()
    // ==========================================================================

    @Test
    void resolve_primitiveVoid_returnsNull() {
        assertNull(handler.resolve(void.class, null));
    }

    @Test
    void resolve_boxedVoid_returnsNull() {
        assertNull(handler.resolve(Void.class, null));
    }
}
