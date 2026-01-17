# Migration Guide: BRUT 5.1.0

## Overview

BRUT 5.1.0 introduces a modern annotation-based testing API that reduces boilerplate by 66-74%. This guide shows you how to migrate existing tests to the new approach.

**Good news:** Migration is completely optional. Your existing tests using `AbstractPageModelTest` and `AbstractJaxrsTest` will continue to work without any changes.

---

## Why Migrate?

**Benefits of Annotation-Based Testing:**

| Feature | Annotation-Based | Legacy Abstract Classes |
|---------|------------------|-------------------------|
| Lines of code per test | ~16 lines | ~47 lines |
| Boilerplate reduction | **66-74%** | - |
| Auto-detection | ✅ HST root, bean paths | ❌ Manual override methods |
| No inheritance | ✅ Field injection | ❌ Must extend base class |
| JUnit 5 native | ✅ Extension-based | ⚠️ Requires @TestInstance |
| Setup methods | ❌ Not needed | ✅ Manual @BeforeAll/@BeforeEach |
| Error messages | ✅ Context-rich with fix suggestions | ⚠️ Generic error messages |
| Debugging | ✅ Configuration summary logging | ⚠️ Manual logging |

---

## Migration: Page Model API Tests

### Before (Abstract Class)

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

### After (Annotation-Based)

```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"}
)
public class PageModelTest {

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

**Changes:**
1. ❌ Remove `extends AbstractPageModelTest`
2. ✅ Add `@BrxmPageModelTest` annotation with `beanPackages`
3. ✅ Add `private DynamicPageModelTest brxm;` field (auto-injected)
4. ❌ Remove `@BeforeAll init()` and `@AfterAll destroy()` methods
5. ❌ Remove `@Override` configuration methods
6. ✅ Change `getHstRequest()` to `brxm.getHstRequest()`
7. ✅ Change `invokeFilter()` to `brxm.invokeFilter()`

**Result:** 47 lines → 21 lines (55% reduction)

---

## Migration: JAX-RS REST API Tests

### Before (Abstract Class)

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

    @AfterAll
    public void destroy() {
        super.destroy();
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

### After (Annotation-Based)

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

**Changes:**
1. ❌ Remove `extends AbstractJaxrsTest`
2. ✅ Add `@BrxmJaxrsTest` annotation with `beanPackages` and `springConfigs`
3. ✅ Add `private DynamicJaxrsTest brxm;` field (auto-injected)
4. ❌ Remove `@BeforeAll init()`, `@BeforeEach beforeEach()`, `@AfterAll destroy()` methods
5. ❌ Remove `@Override` configuration methods
6. ✅ Change `getHstRequest()` to `brxm.getHstRequest()`
7. ✅ Change `invokeFilter()` to `brxm.invokeFilter()`

**Note:** Default headers (Accept: application/json) and method (GET) are set automatically in `@BrxmJaxrsTest`.

**Result:** 47 lines → 16 lines (66% reduction)

---

## Convention Over Configuration

The annotation-based API uses smart conventions to reduce configuration:

### Auto-Detected Settings

| Setting | Convention | Override |
|---------|-----------|----------|
| **HST Root** | `/hst:${artifactId}` from pom.xml | `hstRoot = "/hst:myproject"` |
| **Bean Packages** | None (must specify) | `beanPackages = {"org.example.beans"}` |
| **Spring Configs** | `/org/example/custom-pagemodel.xml`<br>`/org/example/custom-jaxrs.xml` | `springConfigs = {"/path/to/config.xml"}` |

### Explicit Configuration Example

If conventions don't match your project structure:

```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans", "org.example.model"},
    hstRoot = "/hst:customproject",
    springConfigs = {"/org/example/custom-config.xml"}
)
public class ExplicitConfigTest {
    private DynamicPageModelTest brxm;

    @Test
    void testWithExplicitConfig() {
        // Test using explicit configuration
    }
}
```

---

## Migrating to ConfigServiceRepository

### What is ConfigServiceRepository?

`ConfigServiceRepository` loads real brXM configuration (HCM modules) into tests, eliminating the need to duplicate YAML files for testing.

### Before (Manual YAML Imports)

```xml
<!-- custom-pagemodel.xml -->
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
```

**Problems:**
- Duplicate YAML files for tests
- Manual maintenance when config changes
- Different format from production

### After (ConfigServiceRepository)

```xml
<!-- custom-pagemodel.xml -->
<bean id="contributedCndResourcesPatterns" class="java.util.ArrayList">
    <constructor-arg>
        <list>
            <value>classpath*:org/example/namespaces/**/*.cnd</value>
        </list>
    </constructor-arg>
</bean>

<bean id="repository" class="org.bloomreach.forge.brut.common.repository.config.ConfigServiceRepository">
    <constructor-arg>
        <bean class="org.bloomreach.forge.brut.common.repository.config.ModuleReader">
            <constructor-arg>
                <list>
                    <value>hcm-config</value>
                    <value>hcm-content</value>
                </list>
            </constructor-arg>
        </bean>
    </constructor-arg>
</bean>
```

**Benefits:**
- ✅ Uses production HCM modules directly
- ✅ No duplicate YAML files
- ✅ Same config format as production
- ✅ Explicit module loading (no classpath pollution)

### Module Structure

```
src/test/resources/
├── hcm-config/
│   └── hst/
│       ├── configurations.yaml
│       ├── hosts.yaml
│       └── sites.yaml
└── hcm-content/
    └── content/
        └── documents/
            └── news.yaml
```

**Documentation:** See [ConfigServiceRepository Guide](config-service-repository.md) for complete details.

---

## One-Liner ConfigService Integration (Easiest)

BRUT 5.1.0+ simplifies ConfigService setup to a single annotation parameter.

### Before (Manual Spring XML + Annotation)

**Step 1:** Create Spring XML configuration:
```xml
<!-- custom-jaxrs.xml -->
<bean id="repository" class="org.bloomreach.forge.brut.resources.ConfigServiceRepository">
    <constructor-arg>
        <bean class="org.bloomreach.forge.brut.common.repository.config.ModuleReader">
            <constructor-arg>
                <list>
                    <value>hcm-config</value>
                    <value>hcm-content</value>
                </list>
            </constructor-arg>
        </bean>
    </constructor-arg>
</bean>
```

**Step 2:** Reference XML in test:
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    springConfigs = {"/custom-jaxrs.xml"}
)
```

### After (One-Liner)

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    useConfigService = true  // That's it!
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

**What happens behind the scenes:**
1. BRUT auto-generates Spring XML with ConfigServiceRepository bean
2. Loads minimal framework module for core brXM node types (editor, hipposysedit, webfiles)
3. Loads your project's HCM modules from classpath (hcm-config/, hcm-content/)
4. Repository is ready with production-parity configuration

**Benefits:**
- ✅ No manual Spring XML for ConfigService setup
- ✅ Same HCM format as production (YAML + CND)
- ✅ Explicit module loading (no classpath pollution)
- ✅ Works with all 3 annotation types (PageModel, JAX-RS, Component)
- ✅ Permissive CNDs ensure test bootstrapping success

**Note:** The minimal framework uses permissive node type definitions (documented in CND headers) to prioritize test execution over strict validation. This trade-off is acceptable for unit testing scenarios.

---

## Migration Strategy

### Recommended Approach: Gradual Migration

1. **Keep existing tests working** - No immediate changes required
2. **Migrate new tests first** - Use annotations for all new tests
3. **Migrate high-value tests** - Convert frequently modified tests
4. **Leave stable tests** - Low-touch tests can stay as-is

### Step-by-Step Migration

**For each test class:**

1. ✅ Create new test class with `@BrxmPageModelTest` or `@BrxmJaxrsTest`
2. ✅ Copy annotation parameters from `@Override` methods
3. ✅ Add `private Dynamic*Test brxm;` field
4. ✅ Copy test methods, update `getHstRequest()` → `brxm.getHstRequest()`
5. ✅ Run tests to verify behavior matches
6. ✅ Delete old test class once confident

**Tip:** Keep old and new tests side-by-side during transition to compare behavior.

---

## Enhanced Error Handling (5.1.0)

Annotation-based tests provide significantly better error messages and debugging information.

### Configuration Summary on Startup

Every test logs its resolved configuration at startup:

```
========================================
JAX-RS Configuration Summary
========================================
Test Class: org.example.NewsApiTest

Bean Patterns:
  - classpath*:org/example/model/*.class

Spring Configs:
  - /org/example/custom-jaxrs.xml [AUTO-DETECTED]
  - /org/example/rest-resources.xml

HST Root: /hst:myproject

========================================
JAX-RS Initialization Starting
========================================
```

This makes it easy to verify what configuration was actually used, especially when auto-detection is involved.

### Context-Rich Error Messages

**Old (Legacy Abstract Classes):**
```
IllegalStateException: Test instance not initialized. This should not happen.
```

**New (Annotation-Based):**
```
Invalid test state: Test instance not available in beforeEach

Expected: DynamicJaxrsTest should be initialized in beforeAll
Actual: Instance is null

This indicates a bug in BRUT or misuse of test infrastructure.
Please report this issue with full stack trace.
```

### Missing Field Detection

**Old:**
```
IllegalStateException: Test class org.example.MyTest must declare a field of type DynamicJaxrsTest.
Example: private DynamicJaxrsTest brxm;
```

**New:**
```
Missing required field of type DynamicJaxrsTest in test class: org.example.MyTest

To fix:
  Add a field of type DynamicJaxrsTest to your test class (any visibility, any name)

Example:
  private DynamicJaxrsTest brxm;

Fields scanned: testName (String), data (List)
None matched required type: DynamicJaxrsTest
```

The new error shows exactly which fields were found, making typos easy to spot.

### Bootstrap Failures with Full Context

**Old:**
```
RuntimeException: ConfigService bootstrap failed
```

**New:**
```
Bootstrap failed during: JAX-RS test initialization

Configuration attempted:
  Bean patterns: classpath*:org/example/model/*.class
  Spring configs: /org/example/custom-jaxrs.xml, /org/example/rest-resources.xml
  HST root: /hst:myproject

Root cause: FileNotFoundException: class path resource [org/example/rest-resources.xml] cannot be opened

To fix:
  1. Check that all specified resources exist on classpath
  2. Verify bean packages contain valid Spring components
  3. Ensure Spring config files are well-formed XML
  4. Check logs above for more specific error details
```

Shows the complete configuration context and provides actionable steps to fix the issue.

---

## Troubleshooting

### Test Field Not Injected

**Error:** `Missing required field of type DynamicPageModelTest`

**Cause:** Missing or misnamed field declaration

**Solution:** The error message will show exactly which fields were scanned. Declare the correct field type:
```java
@BrxmPageModelTest(...)
public class MyTest {
    private DynamicPageModelTest brxm;  // Must declare this field

    @Test
    void test() {
        brxm.getHstRequest()...  // Now works
    }
}
```

**Tip:** Check the "Fields scanned" section in the error message to spot typos.

### HST Root Not Found

**Error:** `Configuration not found at /hst:myproject`

**Cause:** HST root auto-detection failed or wrong artifact name

**Solution:** Explicitly specify `hstRoot`:
```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"},
    hstRoot = "/hst:actualprojectname"
)
```

### Bean Packages Not Scanned

**Error:** `No beans found` or bean not recognized

**Cause:** Missing or incorrect `beanPackages`

**Solution:** Ensure bean packages match your project structure:
```java
@BrxmPageModelTest(
    beanPackages = {
        "org.example.beans",
        "org.example.model"
    }
)
```

### Spring Config Not Found

**Error:** `FileNotFoundException` for Spring config

**Cause:** Wrong path or missing file

**Solution:** Verify file exists at specified path:
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    springConfigs = {
        "/org/example/custom-jaxrs.xml",  // Check this path
        "/org/example/rest-resources.xml"
    }
)
```

---

## FAQ

### Q: Do I have to migrate?

**A:** No! Existing tests using abstract classes continue to work. Migration is optional and can be done gradually.

### Q: Can I mix old and new approaches?

**A:** Yes! You can have some tests using abstract classes and others using annotations in the same project.

### Q: What about JUnit 4?

**A:** Annotation-based testing requires JUnit 5. If you're using JUnit 4, continue using abstract classes or migrate to JUnit 5 first.

### Q: Will abstract classes be removed?

**A:** No plans to remove them. They remain fully supported for backward compatibility.

### Q: Can I use ConfigServiceRepository with abstract classes?

**A:** Yes! ConfigServiceRepository works with both approaches. Just configure it in your Spring XML and override `contributeRepository()`.

---

## Getting Help

- **Examples:** See `demo/site/components/src/test/java/org/example/` for complete examples
- **Documentation:** [ConfigServiceRepository Guide](config-service-repository.md)
- **Issues:** Report bugs at [GitHub Issues](https://github.com/bloomreach-forge/brut/issues)

---

## Summary

**Annotation-Based Testing Benefits:**
- ✅ 66-74% less boilerplate code
- ✅ No inheritance required (field injection)
- ✅ Auto-detection of configuration
- ✅ Native JUnit 5 support
- ✅ Cleaner, more maintainable tests

**Migration is optional and gradual.** Start with new tests, migrate high-value tests, and leave stable tests as-is.
