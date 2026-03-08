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

/**
 * Utility methods for normalising and joining URL path segments.
 *
 * <p>All methods are stateless and thread-safe. The class cannot be instantiated.
 *
 * @author ruispereira
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PathUtils {

    /**
     * Normalises a URL path segment by ensuring a leading {@code /} and removing any
     * trailing {@code /}.
     * <p>
     * Examples: {@code "users/"} → {@code "/users"}, {@code "/api/"} → {@code "/api"},
     * {@code null} / blank → {@code ""}.
     *
     * @param segment the raw path segment to normalise
     * @return the normalised segment, or {@code ""} if {@code segment} is null or blank
     */
    public static String normalisePath(String segment) {
        if (segment == null || segment.isBlank()) return "";
        String s = segment.trim();
        s = s.startsWith("/") ? s : "/" + s;
        s = s.length() > 1 && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        return s;
    }

    /**
     * Joins a base path and a sub-path into a single normalised URL path,
     * avoiding duplicate slashes.
     * <p>
     * Both segments are normalised via {@link #normalisePath(String)} before joining.
     * Examples: {@code ("/api", "/users")} → {@code "/api/users"},
     * {@code (null, "/users")} → {@code "/users"},
     * {@code (null, null)} → {@code "/"}.
     *
     * @param base the base path segment (e.g. {@code "/api"})
     * @param sub  the sub-path segment to append (e.g. {@code "/users"})
     * @return the joined path, never {@code null}; {@code "/"} if both are blank
     */
    public static String joinPaths(String base, String sub) {
        String b = normalisePath(base);
        String s = normalisePath(sub);
        if (b.isEmpty()) return s.isEmpty() ? "/" : s;
        if (s.isEmpty()) return b;
        return b.endsWith("/") ? b + s.substring(1) : b + s;
    }
}
