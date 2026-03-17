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
package org.springframework.data.domain;

/**
 * Minimal stub of Spring Data's Pageable — exists only so that
 * ParameterProcessorImpl.isPageable() recognises the type by name
 * without requiring spring-data-commons on the compile/test classpath.
 */
public interface Pageable {}
