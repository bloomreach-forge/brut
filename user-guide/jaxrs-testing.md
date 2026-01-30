# JAX-RS Testing Guide

<!-- AI-METADATA
test-types: [jaxrs]
patterns: [rest, endpoint, get, post, json, zero-config]
keywords: [jaxrs, rest, api, endpoint, resources, jackson, json]
difficulty: beginner
-->

## Minimal Setup

```java
@BrxmJaxrsTest(
    resources = {HelloResource.class}
)
class HelloResourceTest {
    private DynamicJaxrsTest brxm;

    @Test
    void hello_returnsGreeting() {
        String response = brxm.request()
            .get("/site/api/hello/world")
            .execute();

        assertEquals("Hello, World!", response);
    }
}
```

**That's it.** No Spring XML required.

## How It Works

| Parameter | What it does |
|-----------|--------------|
| `resources` | JAX-RS classes to register (auto-wrapped in `SingletonResourceProvider`) |
| `beanPackages` | Packages for HST content beans |

**Auto-detected:**
- HST root from Maven artifactId
- Test YAML from `<testPackage>/imports/*.yaml`
- Jackson JSON support
- Project content via ConfigService

## Request Testing

### GET Requests

```java
@Test
void getUser_returnsUserData() {
    String json = brxm.request()
        .get("/site/api/users/123")
        .execute();

    UserResponse user = new ObjectMapper().readValue(json, UserResponse.class);
    assertEquals("123", user.getId());
}
```

### With Query Parameters

```java
@Test
void search_returnsFilteredResults() {
    String json = brxm.request()
        .get("/site/api/search")
        .queryParam("q", "news")
        .queryParam("limit", "10")
        .execute();

    // ...
}
```

### With Headers

```java
@Test
void secureEndpoint_requiresAuth() {
    String json = brxm.request()
        .get("/site/api/secure/data")
        .withHeader("Authorization", "Bearer token")
        .execute();

    // ...
}
```

### POST Requests

```java
@Test
void createItem_returnsCreated() {
    setRequestBody("{\"name\": \"test\"}");
    brxm.getHstRequest().setRequestURI("/site/api/items");
    brxm.getHstRequest().setMethod(HttpMethod.POST);
    brxm.getHstRequest().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    String json = brxm.invokeFilter();
    // ...
}

private void setRequestBody(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    brxm.getHstRequest().setInputStream(
        new DelegatingServletInputStream(new ByteArrayInputStream(bytes))
    );
}
```

## Authentication

```java
@Test
void adminEndpoint_requiresAdminRole() {
    String response = brxm.request()
        .get("/site/api/admin/users")
        .asUser("admin", "admin", "editor")
        .execute();

    assertThat(response).contains("users");
}

@Test
void reportEndpoint_requiresRole() {
    String response = brxm.request()
        .get("/site/api/reports")
        .withRole("manager")
        .execute();

    // ...
}
```

### Testing Auth Failures

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

See [Authentication Patterns](authentication-patterns.md) for more details.

## Multiple Resources

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {UserResource.class, NewsResource.class, AuthResource.class}
)
class ApiTest {
    // All three resources are registered
}
```

## Annotation Reference

```java
@BrxmJaxrsTest(
    // Required: Bean packages for HST content beans
    beanPackages = {"org.example.model"},

    // JAX-RS resources to register (replaces Spring XML)
    resources = {HelloResource.class},

    // Optional: Custom YAML patterns (auto-detected from <package>/imports/)
    yamlPatterns = {"classpath*:custom/path/**/*.yaml"},

    // Optional: Custom CND patterns
    cndPatterns = {"classpath*:custom/namespaces/**/*.cnd"},

    // Optional: Load production HCM content
    loadProjectContent = true,

    // Optional: Override auto-detected HST root
    hstRoot = "/hst:myproject",

    // Optional: Additional Spring configs (for special cases)
    springConfigs = {"/org/example/custom.xml"}
)
```

## Advanced: Spring XML Configuration

For complex scenarios requiring Spring-managed dependencies, create a Spring XML file:

**File:** `src/test/resources/com/example/test-jaxrs.xml`

```xml
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
              <constructor-arg ref="someSpringBean"/>
            </bean>
          </constructor-arg>
        </bean>
      </list>
    </property>
  </bean>
</beans>
```

**Usage:**
```java
@BrxmJaxrsTest(
    beanPackages = {"com.example.model"},
    springConfigs = {"/com/example/test-jaxrs.xml"}
)
```

## Common Issues

| Issue | Fix |
|-------|-----|
| 404 Not Found | Check `@Path` annotation matches request URI |
| Empty response | Verify resource is in `resources` array |
| JSON parse error | Ensure response POJO has matching field names |
| Mount not found | Add test YAML with HST mount config to `<package>/imports/` |

## Required Imports

```java
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
```

## Related

- [Quick Reference](quick-reference.md) - All annotation options
- [Authentication Patterns](authentication-patterns.md) - Testing secured endpoints
- [Stubbing Test Data](stubbing-test-data.md) - YAML content setup
