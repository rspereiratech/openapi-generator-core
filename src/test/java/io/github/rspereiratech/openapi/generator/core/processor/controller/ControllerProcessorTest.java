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
package io.github.rspereiratech.openapi.generator.core.processor.controller;

import io.github.rspereiratech.openapi.generator.core.processor.operation.OperationProcessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ControllerProcessorImpl}.
 *
 * <p>Covers path registration, HTTP verb mapping, tag derivation, annotation
 * inheritance from interfaces and abstract superclasses, and the
 * child-overrides-ancestor rule for {@code @Tag} resolution (Swagger-equivalent
 * semantics). Uses a mock {@link OperationProcessor} to isolate the controller
 * processing logic.
 */
@ExtendWith(MockitoExtension.class)
class ControllerProcessorTest {

    @Mock
    OperationProcessor operationProcessor;

    private ControllerProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(operationProcessor.buildOperation(any(), anyString(), any(), any(), any()))
                .thenReturn(new Operation());
        processor = new ControllerProcessorImpl(operationProcessor);
    }

    // ==========================================================================
    // Fixtures — basic controllers
    // ==========================================================================

    @RestController
    @RequestMapping("/api/v1/items")
    static class ItemController {
        @GetMapping           public String listItems() { return "[]"; }
        @GetMapping("/{id}")  public String getItem()   { return "{}"; }
    }

    @RestController
    static class PingController {
        @GetMapping("/ping") public String ping() { return "pong"; }
    }

    @RestController
    @RequestMapping("/api/v1/resources")
    static class CrudController {
        @GetMapping            public String list()    { return "[]"; }
        @PostMapping           public String create()  { return "{}"; }
        @PutMapping("/{id}")   public String replace() { return "{}"; }
        @DeleteMapping("/{id}") public void delete()  {}
        @PatchMapping("/{id}") public String update()  { return "{}"; }
    }

    @RestController
    @RequestMapping("/api/v1/tasks")
    static class RequestMappingController {
        @RequestMapping(method = RequestMethod.GET)
        public String listTasks() { return "[]"; }

        @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
        public void deleteTask() {}
    }

    @RestController
    @RequestMapping("/api/v1/docs")
    @Tag(name = "documentation", description = "Doc endpoints")
    static class TaggedController {
        @GetMapping public String list() { return "[]"; }
    }

    // ==========================================================================
    // Fixtures — interface annotation inheritance
    // ==========================================================================
    //
    // Controller declares only @RestController; all routing and documentation
    // annotations live on the interface — the interface-as-contract pattern.
    // ==========================================================================

    @Tag(name = "users", description = "User management")
    @RequestMapping("/api/v1/users")
    interface UserFixtureApi {
        @GetMapping              List<String> listUsers();
        @GetMapping("/{id}")     String getUserById(@PathVariable Long id);
        @GetMapping("/search")   List<String> searchUsers();
    }

    @RestController
    static class UserFixtureController implements UserFixtureApi {
        @Override public List<String> listUsers()             { return List.of(); }
        @Override public String getUserById(Long id)          { return "user"; }
        @Override public List<String> searchUsers()           { return List.of(); }
    }

    // ==========================================================================
    // Fixtures — abstract superclass annotation inheritance
    // ==========================================================================

    abstract static class AbstractProductFixtureApi<T, ID> {
        @GetMapping("/{id}")    public abstract T      getById(@PathVariable ID id);
        @GetMapping             public abstract List<T> listAll();
        @DeleteMapping("/{id}") public abstract void   deleteById(@PathVariable ID id);
    }

    @RestController
    @Tag(name = "products", description = "Product catalogue")
    @RequestMapping("/api/v1/products")
    static class ProductFixtureController extends AbstractProductFixtureApi<String, Long> {
        @Override public String       getById(Long id)   { return "product"; }
        @Override public List<String> listAll()          { return List.of(); }
        @Override public void         deleteById(Long id) {}
    }

    // ==========================================================================
    // Fixtures — child-overrides-ancestor @Tag resolution
    //
    // Two scenarios, both expected to surface only the most-specific @Tag:
    //   (a) interface hierarchy: AgentFixtureApi extends GenericVertexFixtureController
    //       Both interfaces declare @Tag → only AgentFixtureApi's wins.
    //   (b) class hierarchy:     TaggedChildFixture extends TaggedParentFixture
    //       Both classes declare @Tag → only TaggedChildFixture's wins.
    // ==========================================================================

    @Tag(name = "Generic REST API", description = "Generic CRUD operations")
    @RequestMapping
    interface GenericVertexFixtureController<T, ID> {
        @GetMapping("/{id}") T      getById(@PathVariable ID id);
        @GetMapping          List<T> getAll();
    }

    @Tag(name = "Agents", description = "Agent management")
    interface AgentFixtureApi extends GenericVertexFixtureController<String, String> {
        @GetMapping("/{id}/group") String getAgentGroup(@PathVariable String id);
    }

    @RestController
    @RequestMapping("/api/v1/agents")
    static class AgentFixtureController implements AgentFixtureApi {
        @Override public String       getById(String id)       { return "agent"; }
        @Override public List<String> getAll()                 { return List.of(); }
        @Override public String       getAgentGroup(String id) { return "group"; }
    }

    @Tag(name = "ParentClassTag", description = "Generic class-level tag")
    @RequestMapping("/api/v1/tagged")
    abstract static class TaggedParentFixture {
        @GetMapping public abstract String list();
    }

    @RestController
    @Tag(name = "ChildClassTag", description = "Specific class-level tag")
    static class TaggedChildFixture extends TaggedParentFixture {
        @Override public String list() { return ""; }
    }

    // ==========================================================================
    // process() — path registration
    // ==========================================================================

    @Test
    void process_itemController_populatesExpectedPaths() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(ItemController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths, "Paths must be initialised after processing");
        Assertions.assertAll(
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/items"),
                        "Expected path /api/v1/items"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/items/{id}"),
                        "Expected path /api/v1/items/{id}")
        );
        verify(operationProcessor, times(2)).buildOperation(any(), anyString(), any(), any(), any());
    }

    @Test
    void process_pingController_noPrefixPath() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(PingController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertTrue(paths.containsKey("/ping"), "Expected /ping to be registered");
        verify(operationProcessor, atLeastOnce()).buildOperation(any(), anyString(), any(), any(), any());
    }

    @Test
    void process_pathsInitialisedEvenWhenOpenApiHasNoPaths() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(null);
        processor.process(ItemController.class, openAPI);
        Assertions.assertNotNull(openAPI.getPaths(), "process() must create a Paths object if it does not exist");
    }

    // ==========================================================================
    // process() — HTTP verbs
    // ==========================================================================

    @Test
    void process_crudController_allHttpVerbsRegistered() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(CrudController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertAll(
                () -> Assertions.assertNotNull(paths.get("/api/v1/resources").getGet(),         "GET /resources"),
                () -> Assertions.assertNotNull(paths.get("/api/v1/resources").getPost(),        "POST /resources"),
                () -> Assertions.assertNotNull(paths.get("/api/v1/resources/{id}").getPut(),    "PUT /resources/{id}"),
                () -> Assertions.assertNotNull(paths.get("/api/v1/resources/{id}").getDelete(), "DELETE /resources/{id}"),
                () -> Assertions.assertNotNull(paths.get("/api/v1/resources/{id}").getPatch(),  "PATCH /resources/{id}")
        );
        verify(operationProcessor, times(5)).buildOperation(any(), anyString(), any(), any(), any());
    }

    @Test
    void process_requestMappingWithMethodGet_registeredAsGet() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(RequestMappingController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertNotNull(paths.get("/api/v1/tasks"), "/api/v1/tasks must be registered");
        Assertions.assertNotNull(paths.get("/api/v1/tasks").getGet(),
                "@RequestMapping(method=GET) must produce a GET operation");
    }

    @Test
    void process_requestMappingWithMethodDelete_registeredAsDelete() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(RequestMappingController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths.get("/api/v1/tasks/{id}"));
        Assertions.assertNotNull(paths.get("/api/v1/tasks/{id}").getDelete(),
                "@RequestMapping(method=DELETE) must produce a DELETE operation");
    }

    // ==========================================================================
    // process() — tags
    // ==========================================================================

    @Test
    void process_itemController_registersTag() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(ItemController.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags(), "Tags must be populated");
        boolean hasItemTag = openAPI.getTags().stream().anyMatch(t -> "item".equals(t.getName()));
        Assertions.assertTrue(hasItemTag, "Tag 'item' should be derived from 'ItemController'");
    }

    @Test
    void process_tagAnnotation_usesNameAndDescription() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(TaggedController.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags());
        boolean found = openAPI.getTags().stream()
                .anyMatch(t -> "documentation".equals(t.getName())
                        && "Doc endpoints".equals(t.getDescription()));
        Assertions.assertTrue(found, "Tag name and description from @Tag must be registered");
    }

    @Test
    void process_sameControllerTwice_tagNotDuplicated() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(ItemController.class, openAPI);
        processor.process(ItemController.class, openAPI);

        long tagCount = openAPI.getTags().stream()
                .filter(t -> "item".equals(t.getName()))
                .count();
        Assertions.assertEquals(1, tagCount, "Tag must not be duplicated when same controller is processed twice");
    }

    // ==========================================================================
    // process() — annotation inheritance: interface as contract
    // ==========================================================================

    @Test
    void process_interfaceAnnotationInheritance_pathsFromInterfacePresent() {
        // UserFixtureController carries only @RestController; all routing comes from UserFixtureApi.
        OpenAPI openAPI = new OpenAPI();
        processor.process(UserFixtureController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertAll(
                "Routing annotations from UserFixtureApi must be surfaced on the concrete controller",
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/users"),        "Expected /api/v1/users"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/users/{id}"),   "Expected /api/v1/users/{id}"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/users/search"), "Expected /api/v1/users/search")
        );
        verify(operationProcessor, atLeastOnce()).buildOperation(any(), anyString(), any(), any(), any());
    }

    @Test
    void process_interfaceAnnotationInheritance_tagFromInterfaceRegistered() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(UserFixtureController.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags());
        boolean hasUsersTag = openAPI.getTags().stream().anyMatch(t -> "users".equals(t.getName()));
        Assertions.assertTrue(hasUsersTag, "Tag 'users' from UserFixtureApi must be registered");
    }

    // ==========================================================================
    // process() — annotation inheritance: abstract superclass
    // ==========================================================================

    @Test
    void process_abstractSuperclassInheritance_pathsFromSuperclassPresent() {
        // ProductFixtureController provides @RequestMapping; routing annotations are on the abstract base.
        OpenAPI openAPI = new OpenAPI();
        processor.process(ProductFixtureController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertAll(
                "Routing annotations from AbstractProductFixtureApi must be surfaced",
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/products"),       "Expected /api/v1/products"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/products/{id}"),  "Expected /api/v1/products/{id}")
        );
    }

    @Test
    void process_abstractSuperclassInheritance_tagFromConcreteClassRegistered() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(ProductFixtureController.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags());
        boolean hasProductsTag = openAPI.getTags().stream().anyMatch(t -> "products".equals(t.getName()));
        Assertions.assertTrue(hasProductsTag, "Tag 'products' declared on ProductFixtureController must be registered");
    }

    // ==========================================================================
    // process() — child-overrides-ancestor @Tag resolution
    // ==========================================================================

    @Test
    void process_childInterfaceOverridesParentInterface_onlyChildTagRegistered() {
        // AgentFixtureApi @Tag("Agents") extends GenericVertexFixtureController @Tag("Generic REST API").
        // Only the child interface's tag must win.
        OpenAPI openAPI = new OpenAPI();
        processor.process(AgentFixtureController.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags(), "Tags list must not be null");
        List<String> tagNames = openAPI.getTags().stream()
                .map(io.swagger.v3.oas.models.tags.Tag::getName)
                .toList();
        Assertions.assertTrue(tagNames.contains("Agents"),
                "'Agents' from AgentFixtureApi must be registered");
        Assertions.assertFalse(tagNames.contains("Generic REST API"),
                "Parent interface's @Tag must be shadowed by the child's");
    }

    @Test
    void process_childInterfaceOverridesParentInterface_onlyChildTagPassedToBuildOperation() {
        OpenAPI openAPI = new OpenAPI();
        processor.process(AgentFixtureController.class, openAPI);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> tagsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationProcessor, atLeastOnce()).buildOperation(any(), anyString(), tagsCaptor.capture(), any(), any());
        Collection<String> captured = tagsCaptor.getValue();
        Assertions.assertTrue(captured.contains("Agents"),
                "buildOperation must receive 'Agents'");
        Assertions.assertFalse(captured.contains("Generic REST API"),
                "buildOperation must not receive the shadowed parent tag");
    }

    @Test
    void process_childInterfaceOverridesParentInterface_inheritedPathsPresent() {
        // Paths declared on GenericVertexFixtureController must still be inherited
        // even though its @Tag is shadowed.
        OpenAPI openAPI = new OpenAPI();
        processor.process(AgentFixtureController.class, openAPI);

        Paths paths = openAPI.getPaths();
        Assertions.assertNotNull(paths);
        Assertions.assertAll(
                "Paths from GenericVertexFixtureController must be present",
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/agents"),            "Expected /api/v1/agents"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/agents/{id}"),       "Expected /api/v1/agents/{id}"),
                () -> Assertions.assertTrue(paths.containsKey("/api/v1/agents/{id}/group"), "Expected /api/v1/agents/{id}/group")
        );
    }

    @Test
    void process_childClassOverridesParentClass_onlyChildTagRegistered() {
        // TaggedChildFixture @Tag("ChildClassTag") extends TaggedParentFixture @Tag("ParentClassTag").
        // The same child-overrides rule must apply at the class level.
        OpenAPI openAPI = new OpenAPI();
        processor.process(TaggedChildFixture.class, openAPI);

        Assertions.assertNotNull(openAPI.getTags(), "Tags list must not be null");
        List<String> tagNames = openAPI.getTags().stream()
                .map(io.swagger.v3.oas.models.tags.Tag::getName)
                .toList();
        Assertions.assertTrue(tagNames.contains("ChildClassTag"),
                "'ChildClassTag' from TaggedChildFixture must be registered");
        Assertions.assertFalse(tagNames.contains("ParentClassTag"),
                "Parent class's @Tag must be shadowed by the child's");
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullOperationProcessor_throwsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ControllerProcessorImpl(null));
    }
}
