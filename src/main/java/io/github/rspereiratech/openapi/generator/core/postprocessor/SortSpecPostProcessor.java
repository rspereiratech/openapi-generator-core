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
package io.github.rspereiratech.openapi.generator.core.postprocessor;

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@link PostProcessor} that sorts the {@code paths} section, the {@code responses}
 * map of every operation, the {@code components/schemas} map and the properties of
 * every schema alphabetically, producing a deterministic spec regardless of controller
 * discovery order or JVM behaviour.
 *
 * <h3>What is sorted</h3>
 * <ul>
 *   <li><b>Paths</b> — top-level path strings sorted alphabetically
 *       (e.g. {@code /agents} before {@code /queues}).</li>
 *   <li><b>Responses</b> — HTTP status-code keys within each operation sorted
 *       lexicographically (e.g. {@code "200"} before {@code "404"}).</li>
 *   <li><b>Schemas</b> — keys of {@code components/schemas} (the DTOs) sorted
 *       alphabetically (e.g. {@code AgentDto} before {@code UserDto}).</li>
 *   <li><b>Schema properties</b> — the {@code properties} of every schema sorted
 *       alphabetically. ({@code required} needs nothing: swagger-core's
 *       {@code Schema.setRequired} already keeps it sorted.) This covers the schemas in
 *       {@code components} and the inline ones (parameters, request bodies, responses,
 *       headers), recursing into nested properties, {@code items},
 *       {@code additionalProperties}, {@code allOf}/{@code anyOf}/{@code oneOf} and
 *       {@code not}.</li>
 * </ul>
 *
 * <p>Without explicit sorting, these orderings depend on the sequence in which
 * controllers, methods and properties are returned by reflection — which is not
 * guaranteed to be stable across JVMs, operating systems, build environments, or
 * even two runs on the same JVM. Property order in particular comes from Jackson
 * introspection: for a class read through its getters (e.g. {@code java.util.Locale})
 * it follows the JVM's method order. This
 * processor ensures that every generation run produces the same output, making
 * spec diffs readable and CI comparisons reliable.
 *
 * <p>Sorting is enabled by passing {@code sortOutput = true} to the constructor.
 * When disabled, {@link #process(OpenAPI)} is a no-op, allowing the processor
 * to always be present in the pipeline without conditional inclusion logic in
 * the factory.
 *
 * <p>Null or empty collections are silently skipped when sorting is enabled.
 *
 * @author ruispereira
 * @see io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig#sortOutput()
 */
public class SortSpecPostProcessor implements PostProcessor {

    /** Whether sorting is active for this processor instance. */
    private final boolean sortOutput;

    /**
     * Creates a new processor.
     *
     * @param sortOutput {@code true} to sort paths, responses, component schemas and
     *                   schema properties alphabetically;
     *                   {@code false} to make {@link #process(OpenAPI)} a no-op
     */
    public SortSpecPostProcessor(boolean sortOutput) {
        this.sortOutput = sortOutput;
    }

    /**
     * Sorts the {@code paths} block, the {@code responses} map of every operation,
     * the {@code components/schemas} map and the properties of every schema when
     * {@code sortOutput} is {@code true}.
     * When {@code sortOutput} is {@code false} this method returns immediately
     * without modifying the model.
     *
     * @param openAPI the OpenAPI model to process; must not be {@code null}
     * @throws NullPointerException if {@code openAPI} is {@code null}
     */
    @Override
    public void process(OpenAPI openAPI) {
        Preconditions.checkNotNull(openAPI, "'openAPI' must not be null");
        if (!sortOutput) {
            return;
        }
        sortPaths(openAPI);
        sortResponses(openAPI);
        sortSchemas(openAPI);
        sortSchemaProperties(openAPI);
    }

    /**
     * Replaces the {@code paths} block with a new one whose entries are sorted
     * alphabetically by path string. All path items and their operations are
     * preserved unchanged.
     */
    private void sortPaths(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.isEmpty()) {
            return;
        }

        List<String> sorted = new ArrayList<>(paths.keySet());
        sorted.sort(null);

        Paths ordered = new Paths();
        sorted.forEach(path -> ordered.addPathItem(path, paths.get(path)));
        openAPI.setPaths(ordered);
    }

    /**
     * Replaces the {@code responses} map of every operation with a new one
     * whose entries are sorted lexicographically by HTTP status-code string.
     * All response objects are preserved unchanged.
     */
    private void sortResponses(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            return;
        }

        paths.values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(op -> op.getResponses() != null && !op.getResponses().isEmpty())
                .forEach(op -> {
                    ApiResponses responses = op.getResponses();
                    List<String> sorted = new ArrayList<>(responses.keySet());
                    sorted.sort(null);

                    ApiResponses ordered = new ApiResponses();
                    sorted.forEach(code -> ordered.addApiResponse(code, responses.get(code)));
                    op.setResponses(ordered);
                });
    }

    /**
     * Replaces the {@code components/schemas} map with a {@link TreeMap} keyed
     * alphabetically by schema name. Schema bodies are preserved unchanged.
     */
    private void sortSchemas(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null || schemas.isEmpty()) {
            return;
        }
        components.setSchemas(new TreeMap<>(schemas));
    }

    /**
     * Sorts the properties of every schema in the spec, wherever it appears: the
     * {@code components} section (schemas, parameters, request bodies, responses and
     * headers) and every path item and operation. Each schema is visited once, by
     * identity, so shared or self-referencing schemas are handled without looping.
     */
    private void sortSchemaProperties(OpenAPI openAPI) {
        final Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        final Components components = openAPI.getComponents();
        if (components != null) {
            if (components.getSchemas() != null) {
                components.getSchemas().values().forEach(schema -> sortProperties(schema, visited));
            }
            if (components.getParameters() != null) {
                components.getParameters().values().forEach(parameter -> sortParameter(parameter, visited));
            }
            if (components.getRequestBodies() != null) {
                components.getRequestBodies().values().forEach(body -> sortRequestBody(body, visited));
            }
            if (components.getResponses() != null) {
                components.getResponses().values().forEach(response -> sortResponse(response, visited));
            }
            if (components.getHeaders() != null) {
                components.getHeaders().values().forEach(header -> sortHeader(header, visited));
            }
        }

        final Paths paths = openAPI.getPaths();
        if (paths == null) {
            return;
        }
        paths.values().forEach(pathItem -> {
            if (pathItem.getParameters() != null) {
                pathItem.getParameters().forEach(parameter -> sortParameter(parameter, visited));
            }
            pathItem.readOperations().forEach(operation -> sortOperation(operation, visited));
        });
    }

    /**
     * Sorts the schemas of an operation's parameters, request body and responses.
     */
    private void sortOperation(Operation operation, Set<Schema<?>> visited) {
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> sortParameter(parameter, visited));
        }
        sortRequestBody(operation.getRequestBody(), visited);
        if (operation.getResponses() != null) {
            operation.getResponses().values().forEach(response -> sortResponse(response, visited));
        }
    }

    /**
     * Sorts the schema of a parameter, given either directly or through its content.
     */
    private void sortParameter(Parameter parameter, Set<Schema<?>> visited) {
        if (parameter == null) {
            return;
        }
        sortProperties(parameter.getSchema(), visited);
        sortContent(parameter.getContent(), visited);
    }

    /**
     * Sorts the schemas in a request body's content.
     */
    private void sortRequestBody(RequestBody requestBody, Set<Schema<?>> visited) {
        if (requestBody != null) {
            sortContent(requestBody.getContent(), visited);
        }
    }

    /**
     * Sorts the schemas in a response's content and headers.
     */
    private void sortResponse(ApiResponse response, Set<Schema<?>> visited) {
        if (response == null) {
            return;
        }
        sortContent(response.getContent(), visited);
        if (response.getHeaders() != null) {
            response.getHeaders().values().forEach(header -> sortHeader(header, visited));
        }
    }

    /**
     * Sorts the schema of a header, given either directly or through its content.
     */
    private void sortHeader(Header header, Set<Schema<?>> visited) {
        if (header == null) {
            return;
        }
        sortProperties(header.getSchema(), visited);
        sortContent(header.getContent(), visited);
    }

    /**
     * Sorts the schema of every media type in a content map.
     */
    private void sortContent(Content content, Set<Schema<?>> visited) {
        if (content == null) {
            return;
        }
        for (MediaType mediaType : content.values()) {
            if (mediaType != null) {
                sortProperties(mediaType.getSchema(), visited);
            }
        }
    }

    /**
     * Replaces a schema's {@code properties} with a {@link TreeMap} keyed alphabetically
     * by property name, then does the same for every schema nested in it: property schemas, {@code items}, a schema-valued
     * {@code additionalProperties}, the {@code allOf}/{@code anyOf}/{@code oneOf}
     * members and {@code not}. Property schemas themselves are preserved unchanged.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void sortProperties(Schema<?> schema, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }

        final Map<String, Schema> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            final Map<String, Schema> ordered = new TreeMap<>(properties);
            schema.setProperties(ordered);
            ordered.values().forEach(property -> sortProperties(property, visited));
        }

        sortProperties(schema.getItems(), visited);
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            sortProperties(additional, visited);
        }
        sortAll(schema.getAllOf(), visited);
        sortAll(schema.getAnyOf(), visited);
        sortAll(schema.getOneOf(), visited);
        sortProperties(schema.getNot(), visited);
    }

    /**
     * Sorts the properties of every schema in a composition list.
     */
    @SuppressWarnings("rawtypes")
    private void sortAll(List<Schema> schemas, Set<Schema<?>> visited) {
        if (schemas != null) {
            schemas.forEach(member -> sortProperties(member, visited));
        }
    }
}
