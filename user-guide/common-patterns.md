# Common Test Patterns

<!-- AI-METADATA
test-types: [component, pagemodel, jaxrs]
patterns: [mocking, assertions, navigation, infrastructure, naming]
keywords: [component, parameters, attributes, pagemodel, fluent, pojo, mapping]
difficulty: intermediate
-->

## Component Parameter Mocking

Mock `@ParametersInfo` interfaces to control component behavior:

```java
@BrxmComponentTest(beanPackages = {"com.example.beans"})
class HeroBannerTest {

    private DynamicComponentTest brxm;
    private HeroBanner component;

    @BeforeEach
    void setUp() {
        component = new HeroBanner();
        component.init(null, brxm.getComponentConfiguration());
    }

    @Test
    void testWithDocumentParameter() {
        // Mock the ParametersInfo interface
        HeroBannerInfo paramInfo = mock(HeroBannerInfo.class);
        when(paramInfo.getDocument()).thenReturn("herobanners/test-hero");
        brxm.setComponentParameters(paramInfo);

        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        HeroBannerModel model = brxm.getRequestAttributeValue("heroBanner");
        assertThat(model.getTitle()).isEqualTo("Welcome");
    }

    @Test
    void testWithMultipleParameters() {
        CardCollectionSectionInfo paramInfo = mock(CardCollectionSectionInfo.class);
        when(paramInfo.getTitle()).thenReturn("Featured Cards");
        when(paramInfo.getPageSize()).thenReturn(3);
        when(paramInfo.getSortOrder()).thenReturn("desc");
        brxm.setComponentParameters(paramInfo);

        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        // Verify results...
    }
}
```

## Request Attribute Assertions

Verify model attributes set by components:

```java
@Test
void doBeforeRender_setsExpectedAttributes() {
    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    // Type-safe retrieval
    HeroBannerModel model = brxm.getRequestAttributeValue("heroBanner");
    assertThat(model).isNotNull();
    assertThat(model.getTitle()).isEqualTo("Expected Title");
    assertThat(model.getDescription()).contains("expected text");

    // Boolean attributes
    Boolean isLoggedIn = brxm.getRequestAttributeValue("loggedin");
    assertThat(isLoggedIn).isTrue();

    // Null checks for optional attributes
    assertThat((Object) brxm.getRequestAttributeValue("optionalAttr")).isNull();
}
```

## PageModel Fluent API Pattern

Navigate PageModel responses with the fluent API:

```java
@BrxmPageModelTest
class PageModelTest {

    private DynamicPageModelTest brxm;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PageModelResponse fetchPageModel(String path) throws Exception {
        return brxm.request()
            .get("/site/resourceapi" + path)
            .executeAsPageModel();
    }

    private <T> T mapModel(Map<String, Object> model, Class<T> clazz) {
        return MAPPER.convertValue(model, clazz);
    }

    @Test
    void testPageStructure() throws Exception {
        PageModelResponse pageModel = fetchPageModel("/dashboard");

        // Root component
        PageComponent root = pageModel.getRootComponent();
        assertThat(root).isNotNull();
        assertThat(root.getName()).isEqualTo("dashboard");

        // Find component by name
        PageComponent heroBanner = pageModel.findComponentByName("HeroBanner").orElseThrow();
        assertThat(heroBanner.getComponentClass())
            .isEqualTo("com.example.components.HeroBanner");

        // Get child components
        List<PageComponent> children = pageModel.getChildComponents(root);
        assertThat(children).isNotEmpty();

        // Extract model data
        Map<String, Object> model = heroBanner.getModel("heroBanner");
        HeroBannerContent content = mapModel(model, HeroBannerContent.class);
        assertThat(content.getTitle()).isNotBlank();
    }

    @Test
    void testPageLinks() throws Exception {
        PageModelResponse pageModel = fetchPageModel("/");

        assertThat(pageModel.getLinks()).isNotNull();
        assertThat(pageModel.getLinks()).containsKey("self");
    }

    @Test
    void testComponentCount() throws Exception {
        PageModelResponse pageModel = fetchPageModel("/dashboard");

        int componentCount = pageModel.getComponentCount();
        assertThat(componentCount).isGreaterThan(0);
        assertThat(componentCount).isLessThan(100);  // Sanity check
    }
}
```

## Component Navigation Pattern

Navigate through nested container structures:

```java
@Test
void testContainerStructure() throws Exception {
    PageModelResponse pageModel = fetchPageModel("/dashboard");

    PageComponent root = pageModel.getRootComponent();
    List<PageComponent> children = pageModel.getChildComponents(root);

    List<String> containerNames = children.stream()
        .map(PageComponent::getName)
        .toList();

    assertThat(containerNames).contains("main");

    // Navigate deeper
    PageComponent main = pageModel.findComponentByName("main").orElseThrow();
    List<PageComponent> mainChildren = pageModel.getChildComponents(main);

    assertThat(mainChildren.stream()
        .map(PageComponent::getName)
        .toList()
    ).containsAnyOf("maincontainer", "sidebarcontainer");
}
```

## POJO Mapping for Content Assertions

Create simple POJOs to map model content for easier assertions:

```java
// POJO for HeroBanner content
public class HeroBannerContent {
    private String title;
    private String description;
    private CallToAction callToAction;

    // Getters and setters...

    public static class CallToAction {
        private String title;
        private String externalLink;
        // Getters and setters...
    }
}

// POJO for PetProfile content
public class PetProfileContent {
    private String petName;
    private String petBreed;
    private int petAge;
    private String collarStatus;
    private int batteryLevel;
    // Getters and setters...
}

// Usage in tests
@Test
void testContentMapping() throws Exception {
    PageModelResponse pageModel = fetchPageModel("/dashboard");

    PageComponent petProfile = pageModel.findComponentByName("PetProfile").orElseThrow();
    Map<String, Object> model = petProfile.getModel("petProfile");

    PetProfileContent content = MAPPER.convertValue(model, PetProfileContent.class);

    assertThat(content.getPetName()).isEqualTo("Max");
    assertThat(content.getPetBreed()).isEqualTo("Golden Retriever");
    assertThat(content.getPetAge()).isEqualTo(3);
    assertThat(content.getCollarStatus()).isEqualTo("connected");
}
```

## Infrastructure Verification Test Pattern

Always include an infrastructure test to verify BRUT setup:

```java
@Test
@DisplayName("Infrastructure: HST request is properly initialized")
void infrastructure_hstRequestInitialized() {
    assertThat(brxm.getHstRequest()).isNotNull();
    assertThat(brxm.getHstResponse()).isNotNull();
}

// For JAX-RS tests
@Test
@DisplayName("Infrastructure: JAX-RS request is properly initialized")
void infrastructure_jaxrsRequestInitialized() {
    assertThat(brxm.getHstRequest()).isNotNull();
    assertThat(brxm.getComponentManager()).isNotNull();
}

// For PageModel tests
@Test
@DisplayName("Infrastructure: PageModel API returns valid response")
void infrastructure_pageModelApiWorks() throws Exception {
    PageModelResponse pageModel = brxm.request()
        .get("/site/resourceapi/")
        .executeAsPageModel();

    assertThat(pageModel).isNotNull();
    assertThat(pageModel.getRootComponent()).isNotNull();
}
```

## Test Naming Conventions

Use descriptive names that convey intent:

```java
// Pattern: methodName_condition_expectedResult
@Test void doBeforeRender_whenNotLoggedIn_setsLoggedInFalse()
@Test void doBeforeRender_withUserProfile_createsUserFromProfile()
@Test void doBeforeRender_withLoginParameter_setsLoginModelTrue()

// Pattern: context_scenario_expectedBehavior (for PageModel)
@Test void unauthenticated_dashboardShowsPromotionalContent()
@Test void freeTier_userSeesDashboard()
@Test void essentialTier_userHasLocationMap()

// Pattern: structure_aspect_assertion (for structural tests)
@Test void structure_dashboardHasMainContainer()
@Test void structure_dashboardHasExpectedContainers()
@Test void content_petProfileShowsPetInfo()
```

## Request Parameter Testing

Test components that read query parameters:

```java
@Test
void doBeforeRender_withSearchQuery_returnsFilteredResults() {
    brxm.addRequestParameter("q", "golden retriever");
    brxm.addRequestParameter("category", "dogs");
    brxm.addRequestParameter("page", "2");

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    SearchResults results = brxm.getRequestAttributeValue("searchResults");
    assertThat(results.getQuery()).isEqualTo("golden retriever");
    assertThat(results.getCurrentPage()).isEqualTo(2);
}
```

## Content Import Pattern

Import YAML test data properly:

```java
@BeforeEach
void setUp() throws RepositoryException {
    // 1. Register custom node types first
    brxm.registerNodeType("myproject:HeroBanner");
    brxm.registerNodeType("myproject:CallToAction");

    // 2. Import YAML content
    URL resource = getClass().getResource("/test-content.yaml");
    ImporterUtils.importYaml(resource, brxm.getRootNode(),
            "/content/documents", "hippostd:folder");

    // 3. Update paths after import
    brxm.recalculateRepositoryPaths();

    // 4. Set content base path
    brxm.setSiteContentBasePath("/content/documents/myproject");

    // 5. Initialize component
    component = new MyComponent();
    component.init(null, brxm.getComponentConfiguration());
}
```

## Session Management Pattern

Handle HTTP sessions in tests:

```java
@Test
void testWithSession() {
    HttpSession session = brxm.getHstRequest().getSession();
    session.setAttribute("user", userProfile);
    session.setAttribute("cart", shoppingCart);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    // Sessions auto-invalidate between tests via setupForNewRequest()
}

@Test
void testWithMockSession() {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(testUser);
    when(session.getAttribute("preferences")).thenReturn(userPrefs);
    brxm.getHstRequest().setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());
}
```

## Related Guides

- [Getting Started](getting-started.md) - Initial setup
- [Authentication Patterns](authentication-patterns.md) - User/session testing
- [JAX-RS Testing](jaxrs-testing.md) - REST endpoint testing
- [Stubbing Test Data](stubbing-test-data.md) - YAML content structure
