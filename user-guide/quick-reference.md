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

**JAX-RS API Test:**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmJaxrsTest(beanPackages = {"org.example.model"})
public class MyApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        brxm.getHstRequest().setRequestURI("/site/api/hello");
        String response = brxm.invokeFilter();
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

**All Annotations Support:**

```java
@BrxmJaxrsTest(
    // Bean packages - specify packages containing HST beans
    beanPackages = {"org.example.model", "org.example.beans"},

    // Optional - auto-detected if not specified
    hstRoot = "/hst:myproject",
    springConfigs = {"/org/example/custom-jaxrs.xml"},
    addonModules = {"/org/example/addon"},

    // Optional - production config
    loadProjectContent = true  // Loads real HCM modules
)
```

### 3a. When to Configure Manually

**Use auto-detection (no explicit config) when:**
- Your project follows standard brXM conventions
- HST root matches Maven artifactId (`/hst:${artifactId}`)
- Spring configs are in test class package with standard names

**Specify `beanPackages` explicitly when:**
- Your beans are in multiple packages
- Bean packages differ from test class package
- You need beans from external modules

**Specify `springConfigs` explicitly when:**
- You have custom JAX-RS resources to register
- You need to provide CND/YAML patterns for content
- Spring config names don't match auto-detection patterns
- You're mixing `loadProjectContent = true` with custom resources

**Specify `hstRoot` explicitly when:**
- HST root doesn't match Maven artifactId
- Testing against a different project's configuration
- Multi-site setup with non-standard naming

### 3b. Custom JAX-RS Resources in Tests

When testing custom REST endpoints, create a Spring config to register your resources:

**File:** `src/test/resources/com/example/test-jaxrs-resources.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <!-- CND patterns for custom node types -->
  <bean id="contributedCndResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:hcm-config/namespaces/**/*.cnd</value>
      </list>
    </constructor-arg>
  </bean>

  <!-- YAML patterns for test content -->
  <bean id="contributedYamlResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:com/example/imports/**/*.yaml</value>
      </list>
    </constructor-arg>
  </bean>

  <!-- Import REST JAX-RS/Jackson support -->
  <import resource="classpath:/org/hippoecm/hst/site/optional/jaxrs/SpringComponentManager-rest-jackson.xml"/>

  <!-- Define your REST Resource bean -->
  <bean id="myResource" class="com.example.rest.MyResource"/>

  <!-- Wrap in ResourceProvider for CXF -->
  <bean id="myResourceProvider" class="org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider">
    <constructor-arg ref="myResource"/>
  </bean>

  <!-- Register custom REST resource providers -->
  <bean id="customRestPlainResourceProviders" class="org.springframework.beans.factory.config.ListFactoryBean">
    <property name="sourceList">
      <list>
        <ref bean="myResourceProvider"/>
      </list>
    </property>
  </bean>

</beans>
```

**Usage:**
```java
@BrxmJaxrsTest(
    loadProjectContent = true,
    springConfigs = {"/com/example/test-jaxrs-resources.xml"}
)
class MyResourceTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testCustomEndpoint() {
        String response = brxm.request()
            .get("/site/api/my-endpoint")
            .execute();
        // ...
    }
}
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
Parameters:
  - beanPackages: String array (optional for Component tests, recommended for others)
  - hstRoot: String (auto-detected from Maven artifactId)
  - springConfigs: String array (auto-detected from classpath)
  - addonModules: String array (optional)
  - loadProjectContent: boolean (default false - use HCM modules for HST bootstrap)
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
- NO auto-detection - MUST be specified explicitly
- Format: Package notation (e.g., "org.example.beans")
- Converted to classpath pattern: "classpath*:org/example/beans/*.class"

**HST Root:**
- Auto-detected from Maven artifactId
- Pattern: `/hst:${artifactId}` from pom.xml
- Override: `hstRoot = "/hst:customname"`

**Spring Configs:**
- JAX-RS: Searches for `custom-jaxrs.xml`, `annotation-jaxrs.xml`, `rest-resources.xml`, `jaxrs-config.xml`
- PageModel: Searches for `custom-pagemodel.xml`, `annotation-pagemodel.xml`, `custom-component.xml`, `component-config.xml`
- Location: In test class package path (converted to resource path)
- Override: `springConfigs = {"/path/to/config.xml"}`

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

**JAX-RS Test:**
```
src/test/java/org/example/MyApiTest.java
src/test/resources/org/example/custom-jaxrs.xml (optional)
```

**Page Model Test:**
```
src/test/java/org/example/MyPageModelTest.java
src/test/resources/org/example/custom-pagemodel.xml (optional)
```

**Component Test:**
```
src/test/java/org/example/MyComponentTest.java
src/test/resources/org/example/custom-component.xml (optional)
```

### Common Patterns

**Pattern 1: Minimal Test (Auto-detection)**
```java
@BrxmJaxrsTest(beanPackages = {"org.example.model"})
public class MinimalTest {
    private DynamicJaxrsTest brxm;

    @Test void test() {
        brxm.getHstRequest().setRequestURI("/api/test");
        assertEquals("OK", brxm.invokeFilter());
    }
}
```

**Pattern 2: Explicit Configuration**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    hstRoot = "/hst:myproject",
    springConfigs = {"/org/example/custom.xml"}
)
public class ExplicitTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

**Pattern 3: Production Config**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    loadProjectContent = true
)
public class ProductionConfigTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

### Implementation Checklist

When generating BRUT tests, verify:

- [ ] Annotation present on class
- [ ] `beanPackages` parameter specified
- [ ] Field of correct Dynamic*Test type declared
- [ ] Field not initialized (handled by extension)
- [ ] Test methods use `brxm.*` to access test infrastructure
- [ ] No inheritance (no `extends` clause)
- [ ] No manual setup/teardown methods needed
- [ ] Imports include annotation and dynamic test class

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
