# Legacy API: Abstract Classes

<!-- AI-METADATA
test-types: [pagemodel, jaxrs]
patterns: [legacy, abstract-class, inheritance]
keywords: [AbstractPageModelTest, AbstractJaxrsTest, junit4, junit5, migration]
difficulty: intermediate
-->

> **Note:** This is the legacy approach using abstract base classes. For new tests, we recommend the [annotation-based API](../README.MD#annotation-based-testing-recommended) which reduces boilerplate by 66-74%.

For existing tests or special cases, the abstract class approach is still fully supported.

## Example with Page Model API

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

## Example with JAX-RS REST Pipeline

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

## Spring Configuration

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

## Migration to Annotation-Based API

To migrate from abstract classes to the annotation-based API:

| Legacy | Annotation-Based |
|--------|------------------|
| `extends AbstractJaxrsTest` | `@BrxmJaxrsTest` + `DynamicJaxrsTest brxm` |
| `extends AbstractPageModelTest` | `@BrxmPageModelTest` + `DynamicPageModelTest brxm` |
| `getAnnotatedHstBeansClasses()` | `beanPackages = {...}` |
| `contributeSpringConfigurationLocations()` | `springConfigs = {...}` |
| `contributeHstConfigurationRootPath()` | `hstRoot = "..."` (auto-detected) |
| `getHstRequest()` | `brxm.getHstRequest()` |
| `invokeFilter()` | `brxm.invokeFilter()` or `brxm.request().execute()` |

## Related Documentation

- [Quick Reference](quick-reference.md) - Annotation-based patterns
- [Getting Started](getting-started.md) - First test setup
- [Architecture](architecture.md) - How BRUT works
