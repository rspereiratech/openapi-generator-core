package org.springframework.data.domain;

/**
 * Minimal stub of Spring Data's Pageable — exists only so that
 * ParameterProcessorImpl.isPageable() recognises the type by name
 * without requiring spring-data-commons on the compile/test classpath.
 */
public interface Pageable {}
