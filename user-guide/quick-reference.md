# BRUT Quick Reference Guide

## For Developers

### 1. Add Dependencies

```xml
<!-- JUnit 5 (Required for annotation-based testing) -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.10.2</version>
  <scope>test</scope>
</dependency>

<!-- For JAX-RS / Page Model API Testing -->
<dependency>
  <groupId>org.bloomreach.forge.brut</groupId>
  <artifactId>brut-resources</artifactId>
  <version>${brut.version}</version>
  <scope>test</scope>
</dependency>

<!-- For Component Testing -->
<dependency>
  <groupId>org.bloomreach.forge.brut</groupId>
  <artifactId>brut-components</artifactId>
  <version>${brut.version}</version>
  <scope>test</scope>
</dependency>
```

**Note:** Most brXM projects already include JUnit 5 via the parent POM.

### 2. Create Your First Test

**JAX-RS API Test (Zero-Config):**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.example.rest.HelloResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {HelloResource.class}  // No Spring XML needed
)
public class MyApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        String response = brxm.request()
            .get("/site/api/hello/world")
            .execute();
        assertEquals("Hello, World!", response);
    }
}
```

**Page Model API Test:**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmPageModelTest(beanPackages = {"org.example.beans"})
public class MyPageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void testComponent() {
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        String response = brxm.invokeFilter();
        assertTrue(response.contains("page"));
    }
}
```

**Component Test:**
```java
package org.example;

import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.bloomreach.forge.brut.components.annotation.DynamicComponentTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmComponentTest(beanPackages = {"org.example.beans"})
public class MyComponentTest {
    private DynamicComponentTest brxm;

    @Test
    void testComponent() {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
```

### 3. Configuration Options

**JAX-RS Test (Recommended):**

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {HelloResource.class, NewsResource.class}  // JAX-RS classes
)
```

**Full Options:**

```java
@BrxmJaxrsTest(
    // Required: Bean packages for HST content beans
    beanPackages = {"org.example.model"},

    // JAX-RS resources (replaces Spring XML)
    resources = {HelloResource.class},

    // Custom YAML patterns (auto-detected from <package>/imports/)
    yamlPatterns = {"classpath*:custom/**/*.yaml"},

    // Custom CND patterns
    cndPatterns = {"classpath*:custom/**/*.cnd"},

    // Load production HCM content
    loadProjectContent = true,

    // Override auto-detected values
    hstRoot = "/hst:myproject",
    springConfigs = {"/org/example/custom.xml"},
    addonModules = {"/org/example/addon"}
)
```

### 3a. When to Configure Manually

**Use minimal config (recommended) when:**
- You have JAX-RS resources to test → use `resources` parameter
- Test YAML is in `<package>/imports/` → auto-detected
- HST root matches Maven artifactId → auto-detected

**Specify `yamlPatterns` when:**
- Test YAML is in non-standard location
- You need additional YAML beyond auto-detected paths

**Specify `springConfigs` when:**
- Resources need Spring-managed dependencies (constructor injection)
- You need custom Spring beans in the test context

**Specify `hstRoot` when:**
- HST root doesn't match Maven artifactId
- Testing against a different project's configuration

### 3b. Advanced: Spring XML (Rare)

**Most tests don't need Spring XML.** Use it only when resources require Spring-managed dependencies:

```xml
<!-- src/test/resources/com/example/test-jaxrs.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <import resource="classpath:/org/hippoecm/hst/site/optional/jaxrs/SpringComponentManager-rest-jackson.xml"/>

  <bean id="customRestPlainResourceProviders" class="org.springframework.beans.factory.config.ListFactoryBean">
    <property name="sourceList">
      <list>
        <bean class="org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider">
          <constructor-arg>
            <bean class="com.example.rest.MyResource">
              <constructor-arg ref="someService"/>  <!-- Spring dependency -->
            </bean>
          </constructor-arg>
        </bean>
      </list>
    </property>
  </bean>
</beans>
```

```java
@BrxmJaxrsTest(
    beanPackages = {"com.example.model"},
    springConfigs = {"/com/example/test-jaxrs.xml"}
)
```

### 4. Fluent APIs

**Request Builder (JAX-RS):**
```java
@Test
void testWithFluentApi() {
    String response = brxm.request()
        .get("/site/api/news")              // Sets URI and GET method
        .withHeader("X-Custom", "value")    // Add custom header
        .queryParam("category", "tech")     // Add query parameter
        .execute();                         // Execute and get response

    assertTrue(response.contains("news"));
}

// POST with body
@Test
void testPostRequest() {
    setRequestBody("{\"name\": \"test\"}");  // See helper below
    brxm.getHstRequest().setRequestURI("/site/api/items");
    brxm.getHstRequest().setMethod(HttpMethod.POST);
    brxm.getHstRequest().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    String response = brxm.invokeFilter();
    // ...
}

// Helper for setting request body
private void setRequestBody(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    brxm.getHstRequest().setInputStream(
        new DelegatingServletInputStream(new ByteArrayInputStream(bytes))
    );
}
```

**Fluent Authentication:**
```java
// Authenticated user with roles
@Test
void testProtectedEndpoint() {
    String response = brxm.request()
        .get("/site/api/admin/users")
        .asUser("admin", "admin", "editor")  // username, roles...
        .execute();

    assertThat(response).contains("users");
}

// Role-only (no username needed)
@Test
void testRoleBasedAccess() {
    String response = brxm.request()
        .get("/site/api/reports")
        .withRole("manager", "viewer")  // roles only
        .execute();

    assertThat(response).contains("reports");
}
```

| Method | Description |
|--------|-------------|
| `asUser(username, roles...)` | Sets remote user and assigned roles |
| `withRole(roles...)` | Sets roles without username |

**Mock Authentication (Testing Failures):**
```java
@Test
void login_fails_forInvalidUser() {
    brxm.authentication().rejectUser("baduser");

    String response = brxm.request()
        .post("/site/api/auth/login")
        .execute();

    assertThat(response).contains("401");
}
```

| Method | Description |
|--------|-------------|
| `authentication().rejectUser(name)` | Reject credentials with this username |
| `authentication().rejectPassword(pwd)` | Reject credentials with this password |

See [Authentication Patterns](authentication-patterns.md) for advanced scenarios.

**Request Builder (Page Model):**
```java
@Test
void testPageModelApi() throws Exception {
    PageModelResponse pageModel = brxm.request()
        .get("/site/resourceapi/")
        .executeAsPageModel();  // Parse response as PageModelResponse

    assertNotNull(pageModel.getRootComponent());
}
```

**Repository Access:**
```java
@Test
void testRepositoryAccess() {
    try (RepositorySession session = brxm.repository()) {
        Node newsNode = session.getNode("/content/documents/news");
        assertEquals("hippo:handle", newsNode.getPrimaryNodeType().getName());
    }
    // Auto-cleanup on try-with-resources exit
}
```

### 5. Component Test Patterns

**Annotation-Driven Setup with Auto-Detection:**

Node types are automatically detected from `@Node(jcrType="...")` annotations in your bean packages - no manual registration needed:

```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/test-content.yaml",
    contentRoot = "/content/documents/myproject"
)
class MyComponentTest {
    private DynamicComponentTest brxm;
    private MyComponent component;

    @BeforeEach
    void setUp() {
        // Node types auto-detected from @Node annotations in beanPackages!
        // Content imported automatically from annotation parameters
        component = new MyComponent();
        component.init(null, brxm.getComponentConfiguration());
    }
}
```

**Manual Setup (when needed):**
```java
@BrxmComponentTest()  // beanPackages optional for simple tests
class MyComponentTest {
    private DynamicComponentTest brxm;
    private MyComponent component;

    @BeforeEach
    void setUp() throws RepositoryException {
        // Manual node type registration (only needed for types not in beanPackages
        // or when you need type inheritance)
        brxm.registerNodeType("myproject:Article");
        brxm.registerNodeType("myproject:Author");

        // Import test content from YAML
        URL resource = getClass().getResource("/test-content.yaml");
        ImporterUtils.importYaml(resource, brxm.getRootNode(),
                "/content/documents", "hippostd:folder");

        // Recalculate hippo:paths after import
        brxm.recalculateRepositoryPaths();

        // Set site content base path
        brxm.setSiteContentBasePath("/content/documents/myproject");

        // Initialize component
        component = new MyComponent();
        component.init(null, brxm.getComponentConfiguration());
    }
}
```

**Node Type Inheritance (explicit nodeTypes required):**
```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    nodeTypes = {"myproject:Article extends myproject:BaseDocument", "myproject:BaseDocument"},
    content = "/test-content.yaml",
    contentRoot = "/content/documents/myproject"
)
class InheritanceTest {
    // When you need type inheritance for includeSubtypes queries,
    // specify nodeTypes explicitly with "extends" syntax
}
```

**Mocking Component Parameters:**
```java
@Test
void testComponentWithParameters() {
    // Mock the ParameterInfo interface
    MyComponentInfo paramInfo = mock(MyComponentInfo.class);
    when(paramInfo.getDocument()).thenReturn("articles/my-article");
    when(paramInfo.getPageSize()).thenReturn(10);

    // Set on request
    brxm.setComponentParameters(paramInfo);

    // Execute component
    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    // Assert on model attributes
    MyModel model = brxm.getRequestAttributeValue("model");
    assertThat(model).isNotNull();
}
```

**Request Parameters and Principal:**
```java
@Test
void testWithRequestParameters() {
    // Add request parameters
    brxm.addRequestParameter("page", "2");
    brxm.addRequestParameter("sort", "date");

    // Set logged-in user
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("testuser");
    brxm.getHstRequest().setUserPrincipal(principal);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());
}
```

### 5a. Stubbed vs Production Test Data

**Stubbed YAML (recommended for unit tests):**
```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/test-articles.yaml",      // Minimal, focused test data
    contentRoot = "/content/documents/site"
)
```
- Fast, isolated, reproducible
- Test controls exactly what data exists
- YAML documents expected data structure

**Production HCM (for integration tests):**
```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"},
    loadProjectContent = true  // Loads real HCM modules
)
```
- Validates real HST configuration
- Slower, depends on project content state

**See also:** [Stubbing Test Data Guide](stubbing-test-data.md) for YAML structure examples and best practices.

### 6. HTTP Session Support

**Using Sessions in Tests:**
```java
@Test
void testWithSession() {
    // Get or create session (lazy initialization)
    HttpSession session = brxm.getHstRequest().getSession();
    session.setAttribute("user", new User("John"));

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    // Verify session was used
    assertThat(session.getAttribute("loginCount")).isEqualTo(1);
}

// Mock session for more control
@Test
void testWithMockedSession() {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(new User("Jane"));

    brxm.getHstRequest().setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());
}
```

**Session Isolation Between Tests:**
Sessions are automatically invalidated in `setupForNewRequest()` for JAX-RS/PageModel tests.
For manual cleanup:
```java
brxm.getHstRequest().invalidateSession();
```

### 7. PageModelResponse Utilities

**Navigating Page Model Structure:**
```java
@BrxmPageModelTest(loadProjectContent = true)
class MyPageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void testPageStructure() throws Exception {
        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/")
            .executeAsPageModel();

        // Get root component (usually the page template)
        PageComponent root = pageModel.getRootComponent();
        assertThat(root.getName()).isEqualTo("homepage");

        // Find component by name anywhere in tree
        PageComponent header = pageModel.findComponentByName("header").orElseThrow();
        assertThat(header).isNotNull();

        // Get direct children of a component
        List<PageComponent> children = pageModel.getChildComponents(root);
        assertThat(children).extracting(PageComponent::getName)
            .containsAnyOf("main", "header", "footer");

        // Resolve $ref references
        for (var childRef : root.getChildren()) {
            PageComponent child = pageModel.resolveComponent(childRef);
            assertThat(child.getId()).isNotNull();
        }
    }

    @Test
    void testComponentModels() throws Exception {
        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/")
            .executeAsPageModel();

        PageComponent heroBanner = pageModel.findComponentByName("HeroBanner").orElseThrow();

        // Check component class
        assertThat(heroBanner.getComponentClass())
            .isEqualTo("com.example.components.HeroBanner");

        // Check if component has a document model
        assertThat(heroBanner.hasModel("document")).isTrue();
        assertThat(heroBanner.getDocumentRef()).isPresent();

        // Access page-level data
        assertThat(pageModel.getChannel()).isNotNull();
        assertThat(pageModel.getLinks()).containsKey("self");
        assertThat(pageModel.getComponentCount()).isGreaterThan(0);
    }
}
```

### 8. Production Config (ConfigService)

**One-Liner:**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    loadProjectContent = true  // Uses HCM modules from classpath
)
```

**HCM Structure:**
```
src/test/resources/
├── META-INF/
│   └── hcm-module.yaml
├── hcm-config/
│   └── hst/
│       ├── configurations.yaml
│       ├── hosts.yaml
│       └── sites.yaml
└── hcm-content/
    └── content/
        └── documents/
```

---

## For AI Agents

### Implementation Pattern

When implementing BRUT tests, follow this exact structure:

**1. Annotation Declaration:**
```
Annotation: @Brxm{Type}Test where Type = PageModel | Jaxrs | Component

JAX-RS Parameters (prefer zero-config):
  - beanPackages: String[] - packages for HST beans (required)
  - resources: Class<?>[] - JAX-RS resource classes (recommended, replaces Spring XML)
  - yamlPatterns: String[] - YAML patterns (auto-detected from <package>/imports/)
  - cndPatterns: String[] - CND patterns (optional)
  - loadProjectContent: boolean - use HCM modules (default true)
  - hstRoot: String - auto-detected from Maven artifactId
  - springConfigs: String[] - only for Spring-managed dependencies
```

**2. Field Declaration:**
```
Type: Dynamic{Type}Test where Type = PageModel | Jaxrs | Component
Visibility: Any (private, protected, public, package-private)
Name: Any valid identifier (convention: "brxm")
Initialization: Automatic by JUnit extension
```

**3. Test Method Pattern:**
```java
@Test
void descriptiveName() {
    // 1. Setup request
    brxm.getHstRequest().setRequestURI("/uri");

    // 2. Execute
    String response = brxm.invokeFilter();

    // 3. Assert
    assertEquals(expected, actual);
}
```

### Auto-Detection Rules

**Bean Packages:**
- MUST be specified explicitly via `beanPackages`
- Format: Package notation (e.g., "org.example.beans")

**JAX-RS Resources:**
- Specify via `resources` parameter (preferred)
- Auto-wrapped in `SingletonResourceProvider`
- Jackson JSON support auto-included

**Test YAML Patterns:**
- Auto-detected from: `classpath*:<testPackage>/imports/**/*.yaml`
- Also checks: `classpath*:<testPackage>/test-data/**/*.yaml`
- Override: `yamlPatterns = {"classpath*:custom/**/*.yaml"}`

**HST Root:**
- Auto-detected from Maven artifactId: `/hst:${artifactId}`
- Override: `hstRoot = "/hst:customname"`

### Error Handling Semantics

**Exception Types:**
- `BrutTestConfigurationException` - Configuration errors (unified in brut-common)

**Error Categories:**
1. **Missing Annotation** - Annotation not present on test class
2. **Missing Field** - No field of required type found
3. **Bootstrap Failed** - Initialization error with full config context
4. **Resource Not Found** - File/resource missing from classpath
5. **Invalid State** - Internal framework error (should not occur)

**Error Message Structure:**
```
{Problem Description}

{Context Section - Configuration/State}

{Root Cause}

To fix:
  1. {Action Item 1}
  2. {Action Item 2}
  ...
```

### Configuration Resolution Algorithm

```
For each annotation parameter:
  IF explicitly specified:
    USE specified value
  ELSE IF auto-detection available:
    RUN auto-detection
    IF found:
      USE auto-detected value
    ELSE:
      USE default value (if exists)
  ELSE:
    THROW configuration error

Priority: Explicit > Auto-detected > Default > Error
```

### File Structure Requirements

**JAX-RS Test (Zero-Config):**
```
src/test/java/org/example/MyApiTest.java
src/test/resources/org/example/imports/hst-config.yaml  (auto-detected)
```

**JAX-RS Test (With Spring XML - rare):**
```
src/test/java/org/example/MyApiTest.java
src/test/resources/org/example/custom-jaxrs.xml
```

**Page Model Test:**
```
src/test/java/org/example/MyPageModelTest.java
src/test/resources/org/example/imports/hst-config.yaml  (auto-detected)
```

**Component Test:**
```
src/test/java/org/example/MyComponentTest.java
src/test/resources/test-content.yaml
```

### Common Patterns

**Pattern 1: Zero-Config JAX-RS (Recommended)**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {HelloResource.class}
)
public class ZeroConfigTest {
    private DynamicJaxrsTest brxm;

    @Test void test() {
        String response = brxm.request()
            .get("/site/api/hello/world")
            .execute();
        assertEquals("Hello, World!", response);
    }
}
```

**Pattern 2: Multiple Resources**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {UserResource.class, NewsResource.class, AuthResource.class}
)
public class MultiResourceTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

**Pattern 3: Custom YAML Location**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {HelloResource.class},
    yamlPatterns = {"classpath*:custom/hst-config/**/*.yaml"}
)
public class CustomYamlTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

### Implementation Checklist

When generating BRUT JAX-RS tests:

- [ ] `@BrxmJaxrsTest` annotation on class
- [ ] `beanPackages` parameter with HST bean packages
- [ ] `resources` parameter with JAX-RS resource classes
- [ ] `private DynamicJaxrsTest brxm;` field (auto-injected)
- [ ] Use `brxm.request().get("/path").execute()` for requests
- [ ] Test YAML in `<package>/imports/` for HST mount config
- [ ] No Spring XML needed for simple resources

### Package Structure

```
brut-common:
  org.bloomreach.forge.brut.common.exception
    - BrutTestConfigurationException (unified errors)
  org.bloomreach.forge.brut.common.logging
    - TestConfigurationLogger (logging utilities)
  org.bloomreach.forge.brut.common.junit
    - TestInstanceInjector (JUnit extension helper)

brut-resources:
  org.bloomreach.forge.brut.resources.annotation
    - BrxmPageModelTest (annotation)
    - BrxmJaxrsTest (annotation)
    - DynamicPageModelTest (test class)
    - DynamicJaxrsTest (test class)
  org.bloomreach.forge.brut.resources.bootstrap
    - ConfigServiceBootstrapStrategy (orchestrator)
    - RuntimeTypeStubber (namespace/nodetype stubbing)
    - JcrNodeSynchronizer (node sync operations)
    - ConfigServiceReflectionBridge (ConfigService reflection)

brut-components:
  org.bloomreach.forge.brut.components.annotation
    - BrxmComponentTest (annotation)
    - DynamicComponentTest (test class)
```

---

## Troubleshooting

**Issue:** NullPointerException on `brxm` field
**Fix:** Declare field: `private DynamicPageModelTest brxm;`

**Issue:** "Missing @BrxmPageModelTest annotation"
**Fix:** Add annotation to test class

**Issue:** "Bean packages: NONE" warning
**Fix:** Add `beanPackages = {"org.example.beans"}` to annotation

**Issue:** Spring config not found
**Fix:** Verify file exists at path, use absolute path starting with `/`

**Issue:** HST root not found
**Fix:** Override with correct path: `hstRoot = "/hst:actualname"`

---

## Further Documentation

- **Stubbing Test Data:** [stubbing-test-data.md](stubbing-test-data.md)
- **ConfigService Details:** [config-service-repository.md](config-service-repository.md)
- **Architecture:** [architecture.md](architecture.md)
- **Release Notes:** [../release-notes.md](../release-notes.md)
- **Examples:** [../demo/site/components/src/test/java/org/example/](../demo/site/components/src/test/java/org/example/)
