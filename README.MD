#  B.R.U.T.

BloomReach Unit Testing Library for BrXM Delivery Tier

## Install

(Make sure the following dependencies are installed as test scoped)

For component testing install the brut-components dependency in the site webapp:

```xml
    <dependency>
      <groupId>com.bloomreach.ps</groupId>
      <artifactId>brut-components</artifactId>
      <version>${brut.version}</version>
      <scope>test</scope>
    </dependency>
```

For headless testing install the brut-resources dependency in the site webapp:

```xml
    <dependency>
      <groupId>com.bloomreach.ps</groupId>
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
 
 An [example](https://code.onehippo.org/prof-services/brut/blob/master/demo/myhippoproject/site/src/test/java/org/example/EssentialsListComponentTest.java) of usage of this module
 
### B.R.U.T. Resources

This module itself depends on the repository in the common module. It has abstract test classes that start up
an HST Container from scratch to test different HST pipelines.

### Example with pagemodel api (under test directory):

```java
package org.example;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.bloomreach.ps.brut.resources.AbstractPageModelTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A user (client) of the testing library providing his/her own config/content
 */
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
        return Collections.singletonList("/org/example/custom-pagemodel.xml");
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @Test
    @DisplayName("Component rendering url response")
    public void test() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");
        String response = invokeFilter();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readValue(response, JsonNode.class);
        assertTrue(jsonNode.get("page").size() > 0);
        assertEquals(jsonNode.get("page").get("models").get("pageable").get("pageSize").intValue(), 10);
    }
}
```

### Example with Jaxrsrestplainpipeline (under test directory):

```java
package org.example;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.example.model.ListItemPagination;
import org.example.model.NewsItemRep;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.bloomreach.ps.brut.resources.AbstractJaxrsTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A user (client) of the testing library providing his/her own config/content
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JaxrsTest extends AbstractJaxrsTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
    }

    private void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        unregisterHstModel();
        registerHstModel();
        setupHstResponse();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/model/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/org/example/custom-jaxrs.xml", "/org/example/rest-resources.xml");
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @Test
    @DisplayName("Test invoking the user endpoint")
    public void testUserEndpoint() {
        String user = "baris";
        getHstRequest().setRequestURI("/site/api/hello/" + user);
        String response = invokeFilter();
        assertEquals("Hello, World! " + user, response);
    }

    @Test
    @DisplayName("Test HST config changes are not visible if HST model is not reloaded after a node update via JCR API")
    public void testMountParamsUpdated() throws Exception {
        String key = "paramName";
        String value = "paramValue";
        getHstRequest().setRequestURI("/site/api/hello/mount/" + key);
        String response = invokeFilter();
        setParamsOnMount(new String[]{key}, new String[]{value});
        assertEquals("", response,
                "Expected nothing to change since the HST model was not explicitly reloaded");

        invalidateHstModel();
        String response2 = invokeFilter();
        assertEquals(value, response2, "Expected param value to be updated since HST model was loaded");
    }

    @Test
    @DisplayName("Test running HST query in news endpoint")
    public void testNewsEndpoint() throws Exception {
        getHstRequest().setRequestURI("/site/api/news");
        String response = invokeFilter();
        ListItemPagination<NewsItemRep> pageable = new ObjectMapper().readValue(response, new TypeReference<ListItemPagination<NewsItemRep>>() {
        });
        assertEquals(3, pageable.getItems().size(), "Pageable didn't have enough results");
    }

    private void setParamsOnMount(String[] paramNames, String[] paramValues) throws Exception {
        Repository repository = getComponentManager().getComponent(Repository.class);
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        String rootMountPath = "/hst:myproject/hst:hosts/dev-localhost/localhost/hst:root";
        Node rootMount = session.getNode(rootMountPath);
        rootMount.setProperty("hst:parameternames", paramNames);
        rootMount.setProperty("hst:parametervalues", paramValues);
        session.save();
        session.logout();
    }

}
```

### Example spring config

Note that as the user of this library you provide the cnd and yaml files to be imported in the repository.

```xml

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <bean id="contributedCndResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:client/packagename/namespaces/**/*.cnd</value>
      </list>
    </constructor-arg>
  </bean>

  <bean id="contributedYamlResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
      <list>
        <value>classpath*:client/packagename/imports/**/*.yaml</value>
      </list>
    </constructor-arg>
  </bean>

</beans>

```