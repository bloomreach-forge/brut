# Troubleshooting Guide

<!-- AI-METADATA
test-types: [component, pagemodel, jaxrs]
patterns: [debugging, errors, fixes]
keywords: [troubleshooting, error, null, exception, setup, debug, logging]
difficulty: all-levels
-->

## Common Setup Issues

### Endpoints Not Working After Adding BRUT

**Symptom:** Real HST endpoints return errors or stop responding after adding BRUT as a dependency

**Cause:** BRUT is declared without `<scope>test</scope>`. BRUT replaces core HST beans (pipelines, component manager, link creator, etc.) with test-oriented implementations. When these are on the runtime classpath, they shadow the real production beans.

**Fix:**
Ensure all BRUT dependencies use `<scope>test</scope>`:
```xml
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-resources</artifactId>
    <version>${brut.version}</version>
    <scope>test</scope>  <!-- Required - do not omit -->
</dependency>
```

### Missing Bean Packages

**Symptom:** `NoClassDefFoundError` or beans return null properties

**Cause:** BRUT can't find your `@Node` annotated beans

**Fix:**
```java
// Specify your bean packages explicitly
@BrxmComponentTest(beanPackages = {"com.example.beans", "com.example.components"})
```

### YAML Content Not Found

**Symptom:** `NullPointerException` when accessing content

**Cause:** Content path doesn't match YAML structure

**Fix:**
1. Check YAML file is in `src/test/resources`
2. Verify the path matches your content root:
```java
// YAML has: /petbase-telecom/herobanners/test-hero
brxm.setSiteContentBasePath("/content/documents/petbase-telecom");

// Component looks for: herobanners/test-hero
when(paramInfo.getDocument()).thenReturn("herobanners/test-hero");
```

### Spring Context Not Loading

**Symptom:** `BeanCreationException` or missing beans in JAX-RS tests

**Cause:** Spring config path incorrect or missing imports

**Fix:**
```java
@BrxmJaxrsTest(
    springConfigs = {"/com/example/test-jaxrs-resources.xml"}  // Must start with /
)
```

Verify your Spring XML:
```xml
<!-- Required import for JAX-RS -->
<import resource="classpath:/org/hippoecm/hst/site/optional/jaxrs/SpringComponentManager-rest-jackson.xml"/>
```

### Node Types Not Registered

**Symptom:** `NoSuchNodeTypeException` when importing YAML

**Cause:** Custom CND types not registered before import

**Fix:**
```java
@BeforeEach
void setUp() throws RepositoryException {
    // Register node types BEFORE importing YAML
    brxm.registerNodeType("myproject:HeroBanner");
    brxm.registerNodeType("myproject:CallToAction");

    // Then import content
    URL resource = getClass().getResource("/test-content.yaml");
    ImporterUtils.importYaml(resource, brxm.getRootNode(),
            "/content/documents", "hippostd:folder");
}
```

---

## Runtime Issues

### Null Request/Response

**Symptom:** `NullPointerException` on `brxm.getHstRequest()`

**Cause:** Field injection not working (field is private or final)

**Fix:**
```java
// WRONG: private field
private DynamicComponentTest brxm;

// CORRECT: package-private (no modifier)
DynamicComponentTest brxm;

// ALSO CORRECT: explicitly package-private
private DynamicComponentTest brxm;  // Actually this IS correct - just ensure @BrxmComponentTest is present
```

Ensure the annotation is on the class:
```java
@BrxmComponentTest(beanPackages = {"com.example.beans"})  // Required!
class MyTest {
    private DynamicComponentTest brxm;  // Will be injected
}
```

### Content Bean Returns Null

**Symptom:** `getContentBean(request)` returns null

**Cause:** Content path not set or content not published

**Fix:**
1. Set site content base path:
```java
brxm.setSiteContentBasePath("/content/documents/myproject");
```

2. Ensure YAML content has publish state:
```yaml
/my-document:
  jcr:primaryType: hippo:handle
  /my-document:
    jcr:primaryType: myproject:Document
    hippo:availability: [live]        # Required!
    hippostd:state: published         # Required!
```

### Component Parameters Not Working

**Symptom:** `getComponentParametersInfo()` returns default values

**Cause:** Mock not set before calling `doBeforeRender`

**Fix:**
```java
@Test
void testWithParameters() {
    // Set parameters BEFORE calling component method
    MyComponentInfo paramInfo = mock(MyComponentInfo.class);
    when(paramInfo.getDocument()).thenReturn("path/to/doc");
    brxm.setComponentParameters(paramInfo);

    // Now call component
    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());
}
```

### Request Attributes Not Found

**Symptom:** `getRequestAttributeValue()` returns null

**Cause:** Wrong attribute name or component didn't set it

**Fix:**
1. Check the exact attribute name in your component:
```java
// In component
request.setAttribute("heroBanner", model);

// In test - must match exactly
HeroBannerModel model = brxm.getRequestAttributeValue("heroBanner");
```

2. Verify component logic actually sets the attribute (check conditional paths)

---

## Decision Tree: Which Test Type to Use?

```
What are you testing?
│
├─► HST Component (extends CommonComponent)?
│   └─► Use @BrxmComponentTest
│
├─► JAX-RS REST endpoint (@Path annotated)?
│   └─► Use @BrxmJaxrsTest
│
├─► Page Model API response?
│   └─► Use @BrxmPageModelTest
│
├─► Just a POJO/utility class?
│   └─► Use plain JUnit (no BRUT needed)
│
└─► Integration with real HST config?
    └─► Add loadProjectContent = true to any BRUT test
```

---

## Error Message Quick Reference

| Error | Likely Cause | Solution |
|-------|--------------|----------|
| Endpoints broken after adding BRUT | Missing `<scope>test</scope>` | Add `<scope>test</scope>` to all BRUT dependencies |
| `NoSuchNodeTypeException` | CND not registered | Call `registerNodeType()` or add CND pattern to Spring config |
| `PathNotFoundException` | Content path wrong | Verify YAML structure matches path |
| `BeanCreationException` | Spring config error | Check XML syntax and import paths |
| `NullPointerException` on request | No `@BrxmComponentTest` | Add annotation to test class |
| `NoClassDefFoundError` | Missing bean package | Add to `beanPackages` parameter |
| `UnsupportedOperationException` | Method not mocked | Mock the component parameter interface |
| `InvalidItemStateException` | Session not saved | Call `brxm.recalculateRepositoryPaths()` after import |

---

## Debug Logging Setup

Enable verbose logging to diagnose issues:

### logback-test.xml

Create `src/test/resources/logback-test.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- BRUT logging -->
    <logger name="org.bloomreach.forge.brut" level="DEBUG"/>

    <!-- HST logging -->
    <logger name="org.hippoecm.hst" level="DEBUG"/>

    <!-- JCR repository -->
    <logger name="org.apache.jackrabbit" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### Run with debug output

```bash
mvn test -Dtest=MyTest -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

---

## Specific Scenarios

### Testing Without Production Config

If `loadProjectContent = true` causes issues, use stub data instead:

```java
@BrxmComponentTest(
    beanPackages = {"com.example.beans"}
    // No loadProjectContent - uses minimal stub
)
class IsolatedComponentTest {
    // Test with YAML imports only
}
```

### Testing With Production Config

If your tests need real HST sitemap/channels:

```java
@BrxmPageModelTest(
    loadProjectContent = true,
    repositoryDataModules = {"application", "site"}  // Specify needed modules
)
class IntegrationTest {
    // Has access to real HST config
}
```

### Resetting Between Tests

If tests affect each other, ensure proper cleanup:

```java
@BeforeEach
void setUp() {
    // Reset for new request (clears attributes, parameters, session)
    brxm.setupForNewRequest();

    // Re-initialize component
    component = new MyComponent();
    component.init(null, brxm.getComponentConfiguration());
}
```

---

## Getting Help

1. **Check the BRUT README** - [README.MD](../README.MD)
2. **Review working examples** - Look at demo project tests
3. **Enable debug logging** - Often reveals the actual issue
4. **Verify dependencies** - Ensure brut-components/brut-resources versions match

## Related Guides

- [Getting Started](getting-started.md) - Initial setup
- [Common Patterns](common-patterns.md) - Working examples
- [Quick Reference](quick-reference.md) - Fast lookup
