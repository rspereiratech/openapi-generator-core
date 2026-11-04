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

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SortSpecPostProcessor}.
 *
 * <p>Verifies that paths, response-code keys, component schemas and schema
 * properties are sorted alphabetically when the processor is enabled, and that the model is left
 * unchanged when disabled.
 */
class SortSpecPostProcessorTest {

    private final SortSpecPostProcessor enabled  = new SortSpecPostProcessor(true);
    private final SortSpecPostProcessor disabled = new SortSpecPostProcessor(false);

    // ==========================================================================
    // Path sorting — enabled
    // ==========================================================================

    @Test
    void process_unsortedPaths_sortedAlphabetically() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        enabled.process(openAPI);

        assertEquals(List.of("/a/first", "/m/middle", "/z/last"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_alreadySortedPaths_remainsSorted() {
        OpenAPI openAPI = openApiWith("/a/first", "/b/second", "/c/third");

        enabled.process(openAPI);

        assertEquals(List.of("/a/first", "/b/second", "/c/third"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_singlePath_noChange() {
        OpenAPI openAPI = openApiWith("/only");

        enabled.process(openAPI);

        assertEquals(List.of("/only"), List.copyOf(openAPI.getPaths().keySet()));
    }

    // ==========================================================================
    // Response sorting — enabled
    // ==========================================================================

    @Test
    void process_unsortedResponses_sortedByStatusCode() {
        Operation op = operationWithResponses("500", "200", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        enabled.process(openAPI);

        assertEquals(List.of("200", "404", "500"),
                List.copyOf(op.getResponses().keySet()));
    }

    @Test
    void process_alreadySortedResponses_remainsSorted() {
        Operation op = operationWithResponses("200", "201", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        enabled.process(openAPI);

        assertEquals(List.of("200", "201", "404"),
                List.copyOf(op.getResponses().keySet()));
    }

    @Test
    void process_multipleOperations_allResponsesSorted() {
        Operation get  = operationWithResponses("404", "200");
        Operation post = operationWithResponses("500", "201", "400");
        PathItem item = new PathItem().get(get).post(post);
        OpenAPI openAPI = new OpenAPI();
        Paths paths = new Paths();
        paths.addPathItem("/test", item);
        openAPI.setPaths(paths);

        enabled.process(openAPI);

        assertEquals(List.of("200", "404"), List.copyOf(get.getResponses().keySet()));
        assertEquals(List.of("201", "400", "500"), List.copyOf(post.getResponses().keySet()));
    }

    @Test
    void process_operationWithNullResponses_doesNotThrow() {
        Operation op = new Operation();  // no responses set
        OpenAPI openAPI = openApiWithOperation("/test", op);
        enabled.process(openAPI);
    }

    // ==========================================================================
    // Schema sorting — enabled
    // ==========================================================================

    @Test
    void process_unsortedSchemas_sortedAlphabetically() {
        OpenAPI openAPI = openApiWithSchemas("UserDto", "AgentDto", "OrderDto");

        enabled.process(openAPI);

        assertEquals(List.of("AgentDto", "OrderDto", "UserDto"),
                List.copyOf(openAPI.getComponents().getSchemas().keySet()));
    }

    @Test
    void process_alreadySortedSchemas_remainsSorted() {
        OpenAPI openAPI = openApiWithSchemas("AgentDto", "OrderDto", "UserDto");

        enabled.process(openAPI);

        assertEquals(List.of("AgentDto", "OrderDto", "UserDto"),
                List.copyOf(openAPI.getComponents().getSchemas().keySet()));
    }

    @Test
    void process_nullComponents_doesNotThrow() {
        OpenAPI openAPI = openApiWith("/test");  // components left null
        enabled.process(openAPI);
        assertNull(openAPI.getComponents());
    }

    @Test
    void process_componentsWithoutSchemas_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI().components(new Components());
        enabled.process(openAPI);
        assertNull(openAPI.getComponents().getSchemas());
    }

    // ==========================================================================
    // Schema property sorting — enabled
    // ==========================================================================

    @Test
    void process_componentSchemaProperties_sortedAlphabetically() {
        Schema<?> agent = objectSchema("name", "id", "email");
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("AgentDto", agent));

        enabled.process(openAPI);

        assertEquals(List.of("email", "id", "name"), propertyNames(agent));
    }

    @Test
    void process_nestedPropertySchemas_sortedRecursively() {
        Schema<?> address = objectSchema("street", "city");
        Schema<?> agent = objectSchema("name", "address");
        agent.getProperties().put("address", address);
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("AgentDto", agent));

        enabled.process(openAPI);

        assertEquals(List.of("address", "name"), propertyNames(agent));
        assertEquals(List.of("city", "street"), propertyNames(address));
    }

    @Test
    void process_inlineQueryParameterArrayItems_sortedAlphabetically() {
        // A Set<java.util.Locale> query parameter: the item properties come from Locale's getters,
        // in whatever order the JVM returns them.
        Schema<?> locale = objectSchema("script", "language", "country", "displayName");
        Operation op = new Operation()
                .addParametersItem(new Parameter().name("languageIds").in("query")
                        .schema(new ArraySchema().items(locale)))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse()));
        OpenAPI openAPI = openApiWithOperation("/translations", op);

        enabled.process(openAPI);

        assertEquals(List.of("country", "displayName", "language", "script"), propertyNames(locale));
    }

    @Test
    void process_inlineRequestBodySchema_sortedAlphabetically() {
        Schema<?> body = objectSchema("name", "id");
        Operation op = new Operation()
                .requestBody(new RequestBody().content(jsonContent(body)))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse()));
        OpenAPI openAPI = openApiWithOperation("/agents", op);

        enabled.process(openAPI);

        assertEquals(List.of("id", "name"), propertyNames(body));
    }

    @Test
    void process_inlineResponseContentAndHeaderSchemas_sortedAlphabetically() {
        Schema<?> content = objectSchema("total", "items");
        Schema<?> header = objectSchema("remaining", "limit");
        ApiResponse response = new ApiResponse()
                .content(jsonContent(content))
                .headers(new LinkedHashMap<>(Map.of("X-Rate", new Header().schema(header))));
        Operation op = new Operation().responses(new ApiResponses().addApiResponse("200", response));
        OpenAPI openAPI = openApiWithOperation("/agents", op);

        enabled.process(openAPI);

        assertEquals(List.of("items", "total"), propertyNames(content));
        assertEquals(List.of("limit", "remaining"), propertyNames(header));
    }

    @Test
    void process_compositionAdditionalPropertiesAndNot_sortedRecursively() {
        Schema<?> allOfMember = objectSchema("b", "a");
        Schema<?> oneOfMember = objectSchema("y", "x");
        Schema<?> anyOfMember = objectSchema("q", "p");
        Schema<?> mapValue = objectSchema("v", "u");
        Schema<?> negated = objectSchema("n", "m");
        Schema<?> root = new Schema<>()
                .allOf(List.of(allOfMember))
                .oneOf(List.of(oneOfMember))
                .anyOf(List.of(anyOfMember))
                .additionalProperties(mapValue)
                .not(negated);
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("Root", root));

        enabled.process(openAPI);

        assertEquals(List.of("a", "b"), propertyNames(allOfMember));
        assertEquals(List.of("x", "y"), propertyNames(oneOfMember));
        assertEquals(List.of("p", "q"), propertyNames(anyOfMember));
        assertEquals(List.of("u", "v"), propertyNames(mapValue));
        assertEquals(List.of("m", "n"), propertyNames(negated));
    }

    @Test
    void process_selfReferencingSchema_doesNotLoop() {
        Schema<?> node = objectSchema("value", "children");
        node.getProperties().put("children", new ArraySchema().items(node));
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("Node", node));

        enabled.process(openAPI);

        assertEquals(List.of("children", "value"), propertyNames(node));
    }

    @Test
    void process_reusableComponentParametersBodiesResponsesAndHeaders_sorted() {
        Schema<?> parameter = objectSchema("b", "a");
        Schema<?> body = objectSchema("d", "c");
        Schema<?> response = objectSchema("f", "e");
        Schema<?> header = objectSchema("h", "g");
        Components components = new Components()
                .addParameters("Filter", new Parameter().name("filter").in("query").schema(parameter))
                .addRequestBodies("Agent", new RequestBody().content(jsonContent(body)))
                .addResponses("Agents", new ApiResponse().content(jsonContent(response)))
                .addHeaders("X-Page", new Header().schema(header));
        OpenAPI openAPI = new OpenAPI().components(components);

        enabled.process(openAPI);

        assertEquals(List.of("a", "b"), propertyNames(parameter));
        assertEquals(List.of("c", "d"), propertyNames(body));
        assertEquals(List.of("e", "f"), propertyNames(response));
        assertEquals(List.of("g", "h"), propertyNames(header));
    }

    @Test
    void process_pathLevelParameterSchema_sortedAlphabetically() {
        Schema<?> parameter = objectSchema("b", "a");
        PathItem item = new PathItem()
                .addParametersItem(new Parameter().name("filter").in("query").schema(parameter))
                .get(new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse())));
        OpenAPI openAPI = new OpenAPI().paths(new Paths().addPathItem("/agents", item));

        enabled.process(openAPI);

        assertEquals(List.of("a", "b"), propertyNames(parameter));
    }

    // ==========================================================================
    // Sorting disabled
    // ==========================================================================

    @Test
    void process_sortOutputFalse_schemaPropertyOrderUnchanged() {
        Schema<?> agent = objectSchema("name", "id", "email");
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("AgentDto", agent));

        disabled.process(openAPI);

        assertEquals(List.of("name", "id", "email"), propertyNames(agent));
    }

    @Test
    void process_sortOutputFalse_pathOrderUnchanged() {
        OpenAPI openAPI = openApiWith("/z/last", "/a/first", "/m/middle");

        disabled.process(openAPI);

        assertEquals(List.of("/z/last", "/a/first", "/m/middle"),
                List.copyOf(openAPI.getPaths().keySet()));
    }

    @Test
    void process_sortOutputFalse_responseOrderUnchanged() {
        Operation op = operationWithResponses("500", "200", "404");
        OpenAPI openAPI = openApiWithOperation("/test", op);

        disabled.process(openAPI);

        assertEquals(List.of("500", "200", "404"),
                List.copyOf(op.getResponses().keySet()));
    }

    @Test
    void process_sortOutputFalse_schemaOrderUnchanged() {
        OpenAPI openAPI = openApiWithSchemas("UserDto", "AgentDto", "OrderDto");

        disabled.process(openAPI);

        assertEquals(List.of("UserDto", "AgentDto", "OrderDto"),
                List.copyOf(openAPI.getComponents().getSchemas().keySet()));
    }

    // ==========================================================================
    // Null / empty safety
    // ==========================================================================

    @Test
    void process_nullOpenAPI_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> enabled.process(null));
    }

    @Test
    void process_nullPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        enabled.process(openAPI);
        assertNull(openAPI.getPaths());
    }

    @Test
    void process_emptyPaths_doesNotThrow() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        enabled.process(openAPI);
        assertEquals(0, openAPI.getPaths().size());
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private static OpenAPI openApiWith(String... paths) {
        OpenAPI openAPI = new OpenAPI();
        Paths p = new Paths();
        for (String path : paths) {
            p.addPathItem(path, new PathItem());
        }
        openAPI.setPaths(p);
        return openAPI;
    }

    private static OpenAPI openApiWithOperation(String path, Operation operation) {
        OpenAPI openAPI = new OpenAPI();
        Paths paths = new Paths();
        paths.addPathItem(path, new PathItem().get(operation));
        openAPI.setPaths(paths);
        return openAPI;
    }

    private static Operation operationWithResponses(String... codes) {
        ApiResponses responses = new ApiResponses();
        for (String code : codes) {
            responses.addApiResponse(code, new ApiResponse().description(code));
        }
        return new Operation().responses(responses);
    }

    private static OpenAPI openApiWithSchemas(String... names) {
        Map<String, Schema> schemas = new LinkedHashMap<>();
        for (String name : names) {
            schemas.put(name, new Schema<>().name(name));
        }
        return new OpenAPI().components(new Components().schemas(schemas));
    }

    /** Builds an object schema whose properties are string schemas, inserted in the given order. */
    @SuppressWarnings("rawtypes")
    private static Schema<?> objectSchema(String... propertyNames) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        for (String name : propertyNames) {
            properties.put(name, new Schema<>().type("string"));
        }
        return new Schema<>().type("object").properties(properties);
    }

    /** Wraps a schema in an {@code application/json} content map. */
    private static Content jsonContent(Schema<?> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }

    /** Returns a schema's property names in iteration order. */
    private static List<String> propertyNames(Schema<?> schema) {
        return List.copyOf(schema.getProperties().keySet());
    }
}
