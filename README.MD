#  B.R.U.T.

[![CI](https://github.com/bloomreach-forge/brut/actions/workflows/ci.yml/badge.svg)](https://github.com/bloomreach-forge/brut/actions/workflows/ci.yml)

<!-- AI-METADATA
framework: brxm-testing
test-types: [component, pagemodel, jaxrs]
patterns: [annotation-based, field-injection, mock-repository]
keywords: [brxm, hst, unit-test, component, pagemodel, jaxrs, rest, bloomreach]
min-java: 17
min-brxm: 16.0
-->

**Bloomreach Unit Testing Library for BrXM Delivery Tier**

Zero-config unit testing for BrXM HST components, JAX-RS APIs, and Page Model API. Reduces test boilerplate by 66-74% with annotation-based testing.

## Quick Navigation

| I want to... | Go to... |
|--------------|----------|
| Get started quickly | [Getting Started Guide](user-guide/getting-started.md) |
| Test an HST component | [Component Testing](#component-testing) |
| Test a REST endpoint | [JAX-RS Testing Guide](user-guide/jaxrs-testing.md) |
| Test Page Model API | [Page Model API Test](#page-model-api-test) |
| Test authenticated users | [Authentication Patterns](user-guide/authentication-patterns.md) |
| Create test content | [Stubbing Test Data](user-guide/stubbing-test-data.md) |
| Fix a failing test | [Troubleshooting Guide](user-guide/troubleshooting.md) |
| See common patterns | [Common Patterns](user-guide/common-patterns.md) |
| Understand architecture | [Architecture](user-guide/architecture.md) |

## Version Compatibility

| BRUT Version | brXM Version | Java | JUnit |
|--------------|--------------|------|-------|
| 5.1.x | 16.x | 17+ | 5.x |
| 5.0.x | 16.x | 17+ | 5.x |
| 4.x | 15.x | 11+ | 4.x / 5.x |
| 3.x | 14.x | 11+ | 4.x |

## What is BRUT?

BRUT provides comprehensive testing infrastructure for Bloomreach Experience Manager (brXM) delivery tier components:

- **Component Testing** - Unit test HST components with mock repository
- **JAX-RS API Testing** - Test REST endpoints with full HST pipeline
- **Page Model API Testing** - Test Page Model API responses
- **Production Parity** - Use real HCM configuration in tests
- **Zero Config** - Auto-detection of beans, HST root, and Spring configs

**Key Benefits:**
- ✅ **66-74% Less Code** - Annotation-based API eliminates boilerplate
- ✅ **Production Config** - Load real HCM modules into tests
- ✅ **Fast Feedback** - Clear error messages with fix suggestions
- ✅ **No Inheritance** - Field injection instead of extending base classes
- ✅ **JUnit 5 Native** - Modern testing patterns

**Docs:**
- [Getting Started](user-guide/getting-started.md) - First test in 3 steps
- [Quick Reference](user-guide/quick-reference.md) - Fast lookup
- [Architecture](user-guide/architecture.md) - How BRUT works

## Install

### Step 1: Add version property to parent pom.xml

```xml
<properties>
    <brut.version>5.1.0</brut.version>
</properties>
```

### Step 2: Add dependency management to parent pom.xml (recommended)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-components</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-resources</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 3: Add dependencies to site/components/pom.xml

```xml
<!-- Component Testing -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-components</artifactId>
    <scope>test</scope>
</dependency>

<!-- PageModel/JAX-RS Testing -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-resources</artifactId>
    <scope>test</scope>
</dependency>
```

> **Note:** JUnit 5, Mockito, and AssertJ versions are typically managed by the brXM parent pom. Only add explicit versions if not already provided by your project's dependency management.

## Quick Start

### Annotation-Based Testing (Recommended)

BRUT 5.1.0+ provides zero-config annotation-based testing with automatic setup and teardown:

#### Page Model API Test
```java
@BrxmPageModelTest(
    loadProjectContent = true  // Uses real HCM config from your project
)
public class MyPageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void testComponentRendering() throws IOException {
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        brxm.getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");
        String response = brxm.invokeFilter();

        JsonNode json = new ObjectMapper().readTree(response);
        assertTrue(json.get("page").size() > 0);
    }
}
```

#### JAX-RS REST API Test
```java
@BrxmJaxrsTest(
    springConfigs = {"/org/example/custom-jaxrs.xml", "/org/example/rest-resources.xml"}
)
public class MyJaxrsTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        brxm.getHstRequest().setRequestURI("/site/api/hello/user");
        String response = brxm.invokeFilter();
        assertEquals("Hello, World! user", response);
    }
}
```

> **`loadProjectContent = true`** loads your project's real HCM configuration (HST sitemap, channels, content types, and CND definitions) into the test repository. Without it, BRUT uses minimal stub data sufficient for basic tests but lacking production routes and content structures. Enable this option for production-parity testing where your tests exercise actual HST pipelines and content paths.

### Key Features

| Feature | Annotation-Based | Legacy (Abstract Classes) |
|---------|------------------|---------------------------|
| **Lines of Code** | ~16 lines per test | ~47 lines per test |
| **Boilerplate Reduction** | **66-74%** | - |
| **Auto-Detection** | ✅ HST root, bean paths | ❌ Manual configuration |
| **Field Injection** | ✅ No inheritance | ❌ Must extend base class |
| **JUnit 5 Native** | ✅ Extension-based | ⚠️ Requires @TestInstance |
| **Production Config** | ✅ ConfigServiceRepository | ⚠️ Manual setup |
| **Thread-Safe** | ✅ Yes | ✅ Yes |

### When to Configure Manually

| Scenario | Solution |
|----------|----------|
| Beans in multiple packages | `beanPackages = {"pkg1", "pkg2"}` |
| HST root doesn't match artifactId | `hstRoot = "/hst:actual-name"` |
| Custom JAX-RS resources | `springConfigs = {"/path/to/config.xml"}` |
| Need custom CND/YAML patterns | Create Spring config with `contributedCndResourcesPatterns` |
| Mix ConfigService with custom resources | `loadProjectContent = true, springConfigs = {...}` |

See [Quick Reference - When to Configure Manually](user-guide/quick-reference.md#3a-when-to-configure-manually) for details.

### Before/After Comparison

**Before (Legacy Abstract Class):**
```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JaxrsTest extends AbstractJaxrsTest {
    @BeforeAll
    public void init() { super.init(); }

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/model/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/custom-jaxrs.xml", "/rest-resources.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @Test
    public void testEndpoint() {
        getHstRequest().setRequestURI("/site/api/hello/user");
        String response = invokeFilter();
        assertEquals("Hello, World! user", response);
    }
}
```

**After (Annotation-Based):**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    springConfigs = {"/custom-jaxrs.xml", "/rest-resources.xml"}
)
public class JaxrsTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        brxm.getHstRequest().setRequestURI("/site/api/hello/user");
        String response = brxm.invokeFilter();
        assertEquals("Hello, World! user", response);
    }
}
```

**Result:** 47 lines → 16 lines (66% reduction)

### Component Testing

```java
@BrxmComponentTest()  // beanPackages optional for simple tests
public class HeroBannerTest {
    private DynamicComponentTest brxm;
    private HeroBanner component;

    @BeforeEach
    void setUp() throws RepositoryException {
        // Register custom node types
        brxm.registerNodeType("myproject:HeroBanner");
        brxm.registerNodeType("myproject:CallToAction");

        // Import test content from YAML
        URL resource = getClass().getResource("/test-content.yaml");
        ImporterUtils.importYaml(resource, brxm.getRootNode(),
                "/content/documents", "hippostd:folder");
        brxm.recalculateRepositoryPaths();
        brxm.setSiteContentBasePath("/content/documents/myproject");

        // Initialize component
        component = new HeroBanner();
        component.init(null, brxm.getComponentConfiguration());
    }

    @Test
    void testWithParameters() {
        // Mock component parameters
        HeroBannerInfo paramInfo = mock(HeroBannerInfo.class);
        when(paramInfo.getDocument()).thenReturn("herobanners/test-hero");
        brxm.setComponentParameters(paramInfo);

        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        HeroBannerModel model = brxm.getRequestAttributeValue("heroBanner");
        assertThat(model.getTitle()).isEqualTo("Welcome");
    }

    @Test
    void testWithLoggedInUser() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("JohnDoe");
        brxm.getHstRequest().setUserPrincipal(principal);

        // ...
    }
}
```

**Component test features:**
- ✅ `registerNodeType()` - Register custom JCR node types
- ✅ `ImporterUtils.importYaml()` - Import test content
- ✅ `recalculateRepositoryPaths()` - Update hippo:paths after import
- ✅ `setSiteContentBasePath()` - Set site content root
- ✅ `setComponentParameters()` - Set mocked ParameterInfo
- ✅ `addRequestParameter()` - Add request parameters
- ✅ `getRequestAttributeValue()` - Assert on model attributes

### Fluent Test Utilities

BRUT 5.1.0+ provides fluent APIs for common test operations:

#### Fluent Request Builder

```java
@BrxmJaxrsTest(beanPackages = {"org.example.model"})
public class FluentApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testFluentRequest() {
        String response = brxm.request()
            .get("/site/api/news")           // Sets URI and method
            .withHeader("X-Custom", "value") // Add custom header
            .queryParam("category", "tech")  // Add query parameter
            .execute();

        assertTrue(response.contains("news"));
    }
}

// Page Model API with PageModelResponse utilities
@BrxmPageModelTest(loadProjectContent = true)
public class PageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void testPageModel() throws Exception {
        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/")
            .executeAsPageModel();

        PageComponent root = pageModel.getRootComponent();
        PageComponent header = pageModel.findComponentByName("header").orElseThrow();
        List<PageComponent> children = pageModel.getChildComponents(root);
    }
}
```

#### Fluent Authentication

```java
// Authenticated user with roles
@Test
void testProtectedEndpoint() {
    String response = brxm.request()
        .get("/site/api/admin/settings")
        .asUser("john", "admin", "editor")  // username + roles
        .execute();

    assertThat(response).contains("settings");
}

// Role-only access testing
@Test
void testRoleBasedAccess() {
    String response = brxm.request()
        .get("/site/api/reports")
        .withRole("manager")  // role without username
        .execute();

    assertThat(response).contains("reports");
}
```

See [Authentication Patterns](user-guide/authentication-patterns.md) for advanced scenarios.

#### Auto-Managed Repository Sessions

```java
@Test
void testRepositoryAccess() {
    try (RepositorySession session = brxm.repository()) {
        Node newsNode = session.getNode("/content/documents/news");
        assertEquals("hippo:handle", newsNode.getPrimaryNodeType().getName());
    }
    // Session automatically closed
}
```

#### HTTP Session Support

```java
@Test
void testWithSession() {
    HttpSession session = brxm.getHstRequest().getSession();
    session.setAttribute("user", userProfile);

    // Or inject a mock session
    brxm.getHstRequest().setSession(mockSession);

    // Sessions auto-invalidate between tests in setupForNewRequest()
}
```

**Benefits:**
- ✅ Chainable request configuration
- ✅ Auto-cleanup of JCR sessions
- ✅ HTTP session support with test isolation
- ✅ PageModelResponse utilities for navigating API responses
- ✅ Works with both PageModel and JAX-RS tests

### Custom JAX-RS Resources

When testing custom REST endpoints, create a Spring config to register your resources:

**File:** `src/test/resources/com/example/test-jaxrs-resources.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <!-- CND patterns for custom node types (when not using ConfigService) -->
  <bean id="contributedCndResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:hcm-config/namespaces/**/*.cnd</value>
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

**Test class:**
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

### One-Liner ConfigService Integration

Production-parity configuration in a single parameter:

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    loadProjectContent = true  // Automatically uses HCM modules!
)
public class NewsTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        // Test uses real brXM configuration from HCM modules
        brxm.getHstRequest().setRequestURI("/site/api/news");
        String response = brxm.invokeFilter();
        assertEquals("expected", response);
    }
}
```

**No Spring XML needed** - BRUT auto-generates `ConfigServiceRepository` with your project's HCM modules.
By default BRUT loads standard repository-data modules (`application`, `site`, `development`, `site-development`, `webfiles`).
Add extra modules explicitly when needed:

```java
@BrxmPageModelTest(
    loadProjectContent = true,
    repositoryDataModules = {"cms"}
)
```

If CMS or addon config is present but not needed for delivery-tier tests, BRUT can prune unreachable definitions under
`/hippo:configuration/hippo:frontend`, `/hippo:configuration/hippo:modules`, and `/hippo:configuration/hippo:translations`.
By default BRUT only allows config roots under `/hst:`, `/content`, `/webfiles`, and `/hippo:namespaces`.
Override with `-Dbrut.configservice.allowedConfigRoots=/hst:,/hippo:configuration/hippo:modules` or
use `-Dbrut.configservice.allowedConfigRoots=*` to disable filtering.
Unreachable roots outside the allowed list are also pruned by default unless `brut.configservice.pruneConfigRoots`
is explicitly set.
Override roots with `-Dbrut.configservice.pruneConfigRoots=/hippo:configuration/hippo:frontend,/hippo:configuration/hippo:modules`,
use `-Dbrut.configservice.pruneConfigRoots=*` to prune any unreachable root, or disable pruning with
`-Dbrut.configservice.pruneFrontendConfig=false`.

### Production-Parity Configuration

BRUT 5.1.0+ includes `ConfigServiceRepository` for loading real brXM configuration in tests:

```java
// Uses brXM's ConfigService to load HCM modules (YAML + CND)
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"}
)
public class MyTest {
    private DynamicPageModelTest brxm;

    @Test
    void testWithProductionConfig() {
        // Repository bootstrapped with production HCM config
        // No manual YAML imports needed!
    }
}
```

**Benefits:**
- ✅ Same config format as production (HCM modules)
- ✅ No duplicate YAML files for tests
- ✅ Explicit module loading (no classpath pollution)
- ✅ Works with both PageModel and JAX-RS tests

See [ConfigServiceRepository Documentation](user-guide/config-service-repository.md) for details.

## Documentation

- **[Quick Reference Guide](user-guide/quick-reference.md)** - Fast lookup for common patterns
- **[Architecture](user-guide/architecture.md)** - How BRUT works internally
- **[ConfigService Guide](user-guide/config-service-repository.md)** - Production-parity configuration
- **[Release Notes](release-notes.md)** - Version history and features
- **[Examples](demo/site/components/src/test/java/org/example/)** - Working test examples

## Contents

### B.R.U.T Common

* This module contains the repository that other modules depend on. This module was initially a fork of the project
[InMemoryJcrRepository](https://github.com/openweb-nl/in-memory-jcr).

* The repository itself can be used standalone. It supports YAML import as main mechanism for bootstrapping content to it.

* Note that you could also provide your own repository.xml (see com.bloomreach.ps.brut.common.repository.BrxmTestingRepository.getRepositoryConfigFileLocation)

* If you are importing yaml that references images, make sure you choose the zip export option. Unzip the export
in the classpath.

* You can import nodes like the following:

```java
java.net.URL resource = getClass().getResource("/news.yaml");
YamlImporter.importYaml(resource, rootNode, "/content/documents/mychannel", "hippostd:folder");

```

### B.R.U.T. Components

This module is for testing HST components. This is a fork of the project called [Hippo Unit Tester](https://github.com/openweb-nl/hippo-unit-tester) by OpenWeb.

 An [example](https://github.com/bloomreach-forge/brut/blob/master/demo/site/components/src/test/java/org/example/EssentialsListComponentTest.java) of usage of this module

### B.R.U.T. Resources

This module provides testing infrastructure for HST pipelines (JAX-RS REST, Page Model API).

**Recommended Approach:** Use annotation-based testing (see Quick Start above)

**Features:**
* ✅ **Annotation-based API** - Zero-config with `@BrxmPageModelTest` and `@BrxmJaxrsTest`
* ✅ **Production-parity config** - ConfigServiceRepository loads real HCM modules
* ✅ **Convention over configuration** - Auto-detects bean paths, HST root
* ✅ **Field injection** - No inheritance required
* ✅ **Thread-safe** for parallel test execution
* ✅ **RequestContextProvider.get()** works in JAX-RS resources
* ✅ Full exception stack traces for debugging
* ⚠️ Legacy abstract classes still supported (see below)

### Legacy Approach: Abstract Classes

For existing tests or when you need features not yet available in the annotation-based API, the abstract class approach (`AbstractJaxrsTest`, `AbstractPageModelTest`) is still fully supported.

See [Legacy API Guide](user-guide/legacy-api.md) for examples and migration guidance.
