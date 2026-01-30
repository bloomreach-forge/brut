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

**JAX-RS API Test (Parameter Injection - Recommended):**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.example.rest.HelloResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmJaxrsTest(resources = {HelloResource.class})  // beanPackages auto-detected!
public class MyApiTest {

    @Test
    void testEndpoint(DynamicJaxrsTest brxm) {  // Parameter injection - no IDE warnings!
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

@BrxmPageModelTest  // Zero-config for standard project layouts
public class MyPageModelTest {

    @Test
    void testComponent(DynamicPageModelTest brxm) {
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

@BrxmComponentTest  // beanPackages, nodeTypes auto-detected
public class MyComponentTest {

    @Test
    void testComponent(DynamicComponentTest brxm) {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
```

### 3. Configuration Options

**JAX-RS Test (Minimal):**

```java
@BrxmJaxrsTest(resources = {HelloResource.class, NewsResource.class})
// beanPackages auto-detected from project-settings.xml or pom.xml
```

**Full Options (all optional except where noted):**

```java
@BrxmJaxrsTest(
    // Optional: Override auto-detected bean packages
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

**Use zero-config (recommended) when:**
- Project has `project-settings.xml` → beanPackages auto-detected
- Test YAML is in `<package>/imports/` → auto-detected
- HST root matches Maven artifactId → auto-detected

**Specify `beanPackages` when:**
- Project lacks `project-settings.xml` and auto-detection fails
- You need packages different from what auto-detection finds

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
        .execute();                         // Execute and get response as String

    assertTrue(response.contains("news"));
}

// Typed response - JSON deserialization in one line
@Test
void testTypedResponse() {
    User user = brxm.request()
        .get("/site/api/user/123")
        .executeAs(User.class);             // Execute and deserialize JSON

    assertEquals("John", user.getName());
}

// POST with JSON body - fluent API
@Test
void testPostRequest() {
    User created = brxm.request()
        .post("/site/api/users")
        .withJsonBody("{\"name\": \"John\"}")  // Sets body + Content-Type
        .executeAs(User.class);

    assertEquals("John", created.getName());
}

// POST with object serialization
@Test
void testPostWithObject() {
    User input = new User("Jane", 25);
    User created = brxm.request()
        .post("/site/api/users")
        .withJsonBody(input)                   // Auto-serializes to JSON
        .executeAs(User.class);

    assertEquals("Jane", created.getName());
}

// Response with status code
@Test
void testResponseWithStatus() {
    Response<User> response = brxm.request()
        .post("/site/api/users")
        .withJsonBody(new User("John", 30))
        .executeWithStatus(User.class);        // Get status + typed body

    assertEquals(201, response.status());
    assertTrue(response.isSuccessful());
    assertEquals("John", response.body().getName());
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
```

### Annotation Parameter Reference

**@BrxmJaxrsTest Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `beanPackages` | String[] | auto-detected | HST content bean packages (from project-settings.xml or classpath scan) |
| `resources` | Class<?>[] | `{}` | JAX-RS resource classes (recommended - eliminates Spring XML) |
| `hstRoot` | String | auto-detected | HST configuration root (from Maven artifactId: `/hst:${artifactId}`) |
| `springConfigs` | String[] | auto-detected | Spring XML configs (only for Spring-managed dependencies) |
| `yamlPatterns` | String[] | auto-detected | YAML patterns (from `<testPackage>/imports/**/*.yaml`) |
| `cndPatterns` | String[] | `{}` | CND node type definition patterns |
| `loadProjectContent` | boolean | `true` | Load real HCM modules via ConfigServiceRepository |

**@BrxmPageModelTest Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `beanPackages` | String[] | auto-detected | HST content bean packages |
| `hstRoot` | String | auto-detected | HST configuration root |
| `springConfig` | String | auto-detected | Spring XML config for PageModel pipeline |
| `loadProjectContent` | boolean | `false` | Load real HCM modules via ConfigServiceRepository |

**@BrxmComponentTest Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `beanPackages` | String[] | auto-detected | HST content bean packages |
| `nodeTypes` | String[] | auto-detected | Node types to register (from `@Node` annotations in beanPackages) |
| `content` | String | `""` | **YAML resource path** - classpath path to YAML file with test content |
| `contentRoot` | String | `""` | **Import target path** - JCR path where content is imported + sets site base |
| `testResourcePath` | String | `""` | Legacy: YAML path (use `content` instead) |

**content and contentRoot Explained:**

These parameters work together to import test data into the mock repository:

```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/petbase-test-content.yaml",      // Source: classpath resource
    contentRoot = "/content/documents/mysite"    // Target: where to import
)
```

- **`content`**: Path to YAML file on classpath (e.g., `/news.yaml`, `/org/example/test-data.yaml`)
- **`contentRoot`**: JCR path where YAML content is imported. Also sets `siteContentBasePath` for HST queries.

**Example YAML structure:**
```yaml
# /petbase-test-content.yaml - imported at /content/documents/mysite
definitions:
  content:
    /articles:                           # Creates /content/documents/mysite/articles
      jcr:primaryType: hippostd:folder
      /my-article:
        jcr:primaryType: hippo:handle
        /my-article:
          jcr:primaryType: myproject:Article
          hippo:availability: [live]
```

See [Stubbing Test Data](stubbing-test-data.md) for complete YAML structure guide.

**2. Injection (Choose One):**

**Parameter Injection (Recommended - No IDE Warnings):**
```java
@Test
void testMethod(DynamicJaxrsTest brxm) {
    // brxm is injected automatically
}
```

**Field Injection (For @BeforeEach or Nested Classes):**
```java
@SuppressWarnings("unused")  // Injected by extension
private DynamicJaxrsTest brxm;
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
- **Auto-detected** from `project-settings.xml` if present (uses `selectedProjectPackage` and `selectedBeansPackage`)
- **Auto-detected** by scanning classpath for `@Node`-annotated classes in common package patterns
- **Auto-detected** from `pom.xml` groupId as fallback
- **Fallback**: derives from test class package (e.g., `testpkg.beans`, `testpkg.model`)
- Can always be explicitly specified via `beanPackages` to override auto-detection
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

    @Test void test(DynamicJaxrsTest brxm) {
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

    @Test void test(DynamicJaxrsTest brxm) { /* ... */ }
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

    @Test void test(DynamicJaxrsTest brxm) { /* ... */ }
}
```

### Implementation Checklist

When generating BRUT JAX-RS tests:

- [ ] `@BrxmJaxrsTest` annotation on class
- [ ] `beanPackages` parameter (optional - auto-detected from `project-settings.xml`)
- [ ] `resources` parameter with JAX-RS resource classes
- [ ] **Parameter injection** (recommended): `void test(DynamicJaxrsTest brxm)`
- [ ] **Or field injection** (for @BeforeEach): `private DynamicJaxrsTest brxm;`
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
**Fix:** Use parameter injection instead: `void test(DynamicPageModelTest brxm)`

**Issue:** "Missing @BrxmPageModelTest annotation"
**Fix:** Add annotation to test class

**Issue:** "Bean packages: NONE" warning
**Fix:** Auto-detection failed. Verify `project-settings.xml` exists, or add explicit `beanPackages = {"org.example.beans"}`

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
