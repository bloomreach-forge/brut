#  B.R.U.T.

**Bloomreach Unit Testing Library for BrXM Delivery Tier**

Zero-config unit testing for BrXM HST components, JAX-RS APIs, and Page Model API. Reduces test boilerplate by 66-74% with annotation-based testing.

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
- Architecture: `ARCHITECTURE.md`

## Install

(Make sure the following dependencies are installed as test scoped)

For component testing install the brut-components dependency in the site webapp:

```xml
    <dependency>
      <groupId>org.bloomreach.forge.brut</groupId>
      <artifactId>brut-components</artifactId>
      <version>${brut.version}</version>
      <scope>test</scope>
    </dependency>
```

For headless testing install the brut-resources dependency in the site webapp:

```xml
    <dependency>
      <groupId>org.bloomreach.forge.brut</groupId>
      <artifactId>brut-resources</artifactId>
      <version>${brut.version}</version>
      <scope>test</scope>
    </dependency>
```

## Quick Start

### Annotation-Based Testing (Recommended)

BRUT 5.1.0+ provides zero-config annotation-based testing with automatic setup and teardown:

#### Page Model API Test
```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"}
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
    beanPackages = {"org.example.model"},
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
@BrxmComponentTest(beanPackages = {"org.example.beans"})
public class NewsListComponentTest {
    private DynamicComponentTest brxm;

    @Test
    void testComponent() throws Exception {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertNotNull(brxm.getHstRequestContext());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
```

**Same zero-config approach for component testing!**

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
            .uri("/site/api/news")
            .method(HttpMethod.POST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .queryParam("category", "tech")
            .execute();

        assertTrue(response.contains("news"));
    }
}
```

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

**Benefits:**
- ✅ Chainable request configuration
- ✅ Auto-cleanup of JCR sessions
- ✅ Less boilerplate for common operations
- ✅ Works with both PageModel and JAX-RS tests

### One-Liner ConfigService Integration

Production-parity configuration in a single parameter:

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    useConfigService = true  // Automatically uses HCM modules!
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
    useConfigService = true,
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

See [ConfigServiceRepository Documentation](docs/config-service-repository.md) for details.

## Documentation

- **[Quick Reference Guide](docs/quick-reference.md)** - Fast lookup for common patterns
- **[Migration Guide](docs/migration-5.1.0.md)** - Upgrade from legacy abstract classes
- **[ConfigService Guide](docs/config-service-repository.md)** - Production-parity configuration
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

For existing tests or special cases, the abstract class approach is still fully supported:

#### Example with Page Model API

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PageModelTest extends AbstractPageModelTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/beans/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Collections.singletonList("/custom-pagemodel.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @Test
    public void testComponentRendering() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");
        String response = invokeFilter();

        JsonNode json = new ObjectMapper().readTree(response);
        assertTrue(json.get("page").size() > 0);
    }
}
```

#### Example with JAX-RS REST Pipeline

**JUnit 5:**
```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JaxrsTest extends AbstractJaxrsTest {

    @BeforeAll
    public void init() {
        super.init();
    }

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

**JUnit 4 (Fully Supported):**
```java
public class JaxrsTest extends AbstractJaxrsTest {

    @Before
    public void setUp() {
        super.init();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
    }

    // Same @Override methods and @Test as above
}
```

### Spring Configuration

Provide CND files and YAML content imports via Spring config:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <bean id="contributedCndResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:org/example/namespaces/**/*.cnd</value>
      </list>
    </constructor-arg>
  </bean>

  <bean id="contributedYamlResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:org/example/imports/**/*.yaml</value>
      </list>
    </constructor-arg>
  </bean>
</beans>
```
