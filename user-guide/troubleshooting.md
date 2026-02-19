# Troubleshooting Guide

<!-- AI-METADATA
test-types: [component, pagemodel, jaxrs]
patterns: [debugging, errors, fixes, diagnostics, assertions]
keywords: [troubleshooting, error, null, exception, setup, debug, logging, diagnostics, PageModelAssert, ConfigurationDiagnostics]
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

---

## Diagnostics API

BRUT provides structured diagnostics that replace bare `AssertionError` failures with actionable output pointing to the exact YAML file or configuration entry that needs fixing.

### `PageModelAssert` — Fluent Assertions

Drop-in replacement for `assertTrue`/`assertNotNull` on `PageModelResponse`:

```java
import org.bloomreach.forge.brut.resources.diagnostics.PageModelAssert;

@Test
void testNewsPage() throws Exception {
    PageModelResponse pageModel = brxm.request()
        .get("/site/resourceapi/news")
        .executeAsPageModel();

    // Each step fails with targeted recommendations if the condition isn't met
    PageModelAssert.assertThat(pageModel, "/news", "mysite")
        .hasPage("newsoverview")
        .hasComponent("NewsList")
        .containerNotEmpty("main");
}
```

Failure example for `hasPage("newsoverview")`:
```
[ERROR] Expected page 'newsoverview' but got 'pagenotfound'

RECOMMENDATIONS:
   • Add sitemap entry in: hst/configurations/mysite/sitemap.yaml for path: /news
   • Ensure sitemap entry points to: hst:pages/newsoverview
   • Create page configuration in: hst/configurations/mysite/pages/newsoverview.yaml
   • Example sitemap entry:
     /news:
       jcr:primaryType: hst:sitemapitem
       hst:componentconfigurationid: hst:pages/newsoverview
```

Failure example for `hasComponent("NewsList")`:
```
[ERROR] Component 'NewsList' not found in PageModel

RECOMMENDATIONS:
   • Available components in PageModel: footer, header, homepage, main
   • Verify component name spelling and case sensitivity
   • Check container references (hst:referencecomponent paths)
```

**All assertion methods:**

| Method | What it checks | Diagnostic on failure |
|--------|---------------|----------------------|
| `hasPage(name)` | Root component name matches | Sitemap + page config YAML guidance |
| `hasComponent(name)` | Component exists by name | Lists all available component names |
| `containerNotEmpty(name)` | Container has children | Workspace reference + child component guidance |
| `containerHasMinChildren(name, n)` | At least `n` children | Child count vs expected |
| `hasContent()` | `content` or `documents` map is non-empty | Generic content check |
| `componentHasModel(comp, model)` | Component has named model | Model name + component context |
| `getComponent(name)` | Asserts + returns `PageComponent` | Same as `hasComponent` |

**Factory methods:**
```java
// With request path and channel (recommended — gives better sitemap advice)
PageModelAssert.assertThat(pageModel, "/news", "mysite")

// Without context (path defaults to "/", channel to "unknown")
PageModelAssert.assertThat(pageModel)
```

### `PageModelDiagnostics` — Manual Diagnostic Calls

Use when you want the diagnostic result without immediately failing:

```java
import org.bloomreach.forge.brut.resources.diagnostics.PageModelDiagnostics;
import org.bloomreach.forge.brut.resources.diagnostics.DiagnosticSeverity;

DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
    "NewsList", "/site/resourceapi/news", pageModel);

if (result.severity() == DiagnosticSeverity.ERROR) {
    log.warn("Diagnostic: {}", result);  // Log rather than fail
}
```

Available methods:

| Method | Use when |
|--------|----------|
| `diagnosePageNotFound(page, path, pm)` | Root component doesn't match expected name |
| `diagnoseComponentNotFound(name, uri, pm)` | `findComponentByName` returns empty |
| `diagnoseEmptyContainer(name, pm)` | `getChildComponents` returns empty list |
| `diagnoseEmptyResponse(uri)` | Page Model API returns empty string |

### `ConfigurationDiagnostics` — Bootstrap Failure Diagnostics

Wraps exceptions from `ConfigService` bootstrap to give targeted YAML/CND recommendations:

```java
import org.bloomreach.forge.brut.resources.diagnostics.ConfigurationDiagnostics;

try {
    repository.init();
} catch (Exception e) {
    DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(e);
    throw new RuntimeException(result.toString(), e);
}
```

Recognises and explains:

| Exception type | Output |
|----------------|--------|
| `ParserException` | File name + line/column of the YAML syntax error |
| `CircularDependencyException` | Which `after:` declarations cause the cycle |
| `DuplicateNameException` | Conflicting node path across YAML files |
| `ConfigurationRuntimeException` (property) | Property name, namespace, and node type |
| `ConfigurationRuntimeException` (node type) | Type name and namespace prefix |
| Any other exception | Generic recommendations + class name |

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
