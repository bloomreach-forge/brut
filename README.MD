#  B.R.U.T.

Bloomreach Unit Testing Library for BrXM Delivery Tier

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

This module provides abstract test classes for testing HST pipelines (JAX-RS REST, Page Model API).

**Features:**
* Supports both JUnit 4 `@Before` and JUnit 5 `@BeforeAll` patterns
* Thread-safe for parallel test execution
* `RequestContextProvider.get()` works in JAX-RS resources
* Full exception stack traces for debugging

### Example with Page Model API

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

### Example with JAX-RS REST Pipeline

**JUnit 5 (Recommended):**
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