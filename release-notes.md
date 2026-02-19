<!--
  Copyright 2024 Bloomreach, Inc (http://www.bloomreach.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

## Version Compatibility

| brXM     | B.R.U.T |
|----------|---------|
| 16.7.0   | 5.3.0   |
| 16.7.0   | 5.2.0   |
| 16.6.5   | 5.1.0   |
| 16.6.5   | 5.0.1   |
| 16.0.0   | 5.0.0   |
| 15.0.1   | 4.0.1   |
| 15.0.0   | 4.0.0   |
| 14.0.0-2 | 3.0.0   |
| 13.4     | 2.1.2   |
| 13.1     | 2.0.0   |
| 12.x     | 1.x     |

## Release Notes

### 5.3.0

**Page Model API v1.0 Document Resolution + Ergonomic Content API**

This release fixes silent data loss when parsing Page Model API v1.0 responses and adds helper methods that replace 4-step stream pipelines with single calls.

#### Bug Fixes

* **v1.0 document resolution** — `PageModelResponse` now correctly handles v1.0 responses where documents and imagesets live inside the `page` section (no separate `content` section). Previously, Jackson silently dropped the `data` field on these entries because `page` was typed `Map<String, PageComponent>`. A new `@JsonSetter("page")` splits entries by `type`: `document` and `imageset` entries route to an internal `documents` map; component entries stay in `page`.
* **`resolveContent` v1.0 fallback** — `resolveContent(ref)` previously only checked `content` (v1.1). It now also checks the `documents` map, so `/page/`-scoped `$ref` values resolve in v1.0 responses.
* **`hasContent()` recognizes v1.0** — Returns `true` when `documents` is non-empty, even when `content` is absent.
* **`getComponentCount()` excludes documents/imagesets** — Counts only component entries. Documents and imagesets parsed from the v1.0 `page` map are no longer included in the count.

#### New API

* **`resolveModelContent(component, modelName, type)`** — Resolves a single `$ref` from a component model to a typed `Optional<T>`. Works with both `/content/` (v1.1) and `/page/` (v1.0) refs.
* **`resolveModelContentList(component, modelName, type)`** — Resolves a list of `$ref` objects from a component model to `List<T>`. Same dual-format support.
* **`getDocuments()`** — Exposes the parsed document/imageset map for v1.0 responses.

**Before (manual 4-step pipeline):**
```java
List<ArticleData> articles = component.<List<Map<String, Object>>>getModel("items")
        .stream()
        .map(m -> PageModelMapper.INSTANCE.convertValue(m, ContentRef.class))
        .map(pageModel::resolveContent)
        .filter(Objects::nonNull)
        .map(item -> item.as(ArticleData.class))
        .toList();
```

**After (one call):**
```java
List<ArticleData> articles = pageModel.resolveModelContentList(component, "items", ArticleData.class);
```

#### Backward Compatibility

| Caller | Impact |
|--------|--------|
| `setPage(Map<String, PageComponent>)` | Preserved — programmatic callers unaffected; `documents` stays empty |
| `getPage()` | Returns only components — **behavioural change for v1.0 responses**: documents/imagesets no longer appear here (this is the correct behaviour) |
| `resolveContent()` | New fallback only — existing `/content/` behaviour unchanged |
| `getComponentCount()` | Counts only components — **behavioural change for v1.0 responses** (fix, not regression) |

#### Diagnostics Package (New)

New `org.bloomreach.forge.brut.resources.diagnostics` package. Bare `AssertionError` failures now include structured, actionable output pointing to the exact YAML file or configuration node that needs fixing.

**`PageModelAssert`** — Fluent assertions that embed diagnostic output on failure:

```java
PageModelAssert.assertThat(pageModel, "/news", "mysite")
    .hasPage("newsoverview")
    .hasComponent("NewsList")
    .containerNotEmpty("main");
```

Failure output example:
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

| Assertion | What it diagnoses |
|-----------|------------------|
| `hasPage(name)` | `pagenotfound`, wrong page — sitemap and page config guidance |
| `hasComponent(name)` | Missing component — lists all available component names |
| `containerNotEmpty(name)` | Empty container — workspace reference and child component guidance |
| `containerHasMinChildren(name, n)` | Insufficient children count |
| `hasContent()` | No content items in response |
| `componentHasModel(comp, model)` | Missing model on a named component |

**`PageModelDiagnostics`** — Manual diagnostic calls returning `DiagnosticResult` without failing the test:
- `diagnosePageNotFound(expectedPage, path, pageModel)`
- `diagnoseComponentNotFound(name, requestUri, pageModel)`
- `diagnoseEmptyContainer(containerName, pageModel)`
- `diagnoseEmptyResponse(requestUri)`

**`ConfigurationDiagnostics`** — Diagnoses `ConfigService` bootstrap failures. Walks the full exception chain and identifies:
- YAML parse errors — with file name, line number, and column
- Circular module dependencies — suggests checking `after:` in `hcm-module.yaml`
- Duplicate node names — identifies conflicting definitions
- Missing property definitions — names the property, namespace, and node type
- Invalid node types — names the type and namespace prefix

**`ConfigErrorParser`** — Extracts structured data from `ConfigurationRuntimeException` messages: JCR paths, YAML file references, node types, property issues.

**`DiagnosticResult`** — Java record: `severity()`, `message()`, `recommendations()`. `toString()` formats with `[ERROR]`/`[WARN]`/`[OK]` prefix and bullet-pointed recommendations.

**`DiagnosticSeverity`** — `SUCCESS`, `INFO`, `WARNING`, `ERROR`.

### 5.2.0

**brXM 16.7.0 Compatibility & Security Hardening**

This release upgrades the brXM parent POM to 16.7.0 and includes breaking changes in transitive dependencies.

#### Breaking Changes

* **brXM 16.7.0 required** — Parent POM upgraded from `hippo-cms7-project:16.6.5` to `16.7.0`. Projects on 16.6.x must remain on BRUT 5.1.x.
* **commons-lang3 migration** — Internal Spring config references updated from `org.apache.commons.lang.StringUtils` to `org.apache.commons.lang3.StringUtils`, matching brXM 16.7.0's classpath. Projects overriding `hst-manager.xml` must update their copies.

#### Security

* **CVE-2025-48924** — CI workflow hardened to prevent credential exposure: heredoc quoting (`<< 'EOF'`), Maven `${env.*}` syntax for server credentials, and fork PR builds are now skipped (secrets are unavailable to fork workflows).

#### Improvements

* **Fork PR handling** — CI detects fork PRs and skips the build with a notice, instead of failing with 401 Unauthorized on the private Maven repository.
* **Log level normalization** — Operational messages from `ConfigServiceBootstrapStrategy`, `RuntimeTypeStubber`, `JcrNodeSynchronizer`, `ConfigServiceReflectionBridge`, and `SiteContentBaseResolverValve` downgraded from WARN to INFO/DEBUG. These are expected conditions during test bootstrap, not warnings.
* **Log noise reduction** — `VirtualHostsService` (duplicate mount alias errors) suppressed; `HstDelegateeFilterBean` reduced from ALL to WARN.
* **Javadoc scope warnings** — `@BrxmComponentTest`, `@BrxmJaxrsTest`, and `@BrxmPageModelTest` annotations now document the `<scope>test</scope>` requirement and the consequences of omitting it.

### 5.1.0

**Annotation-Based Testing & Production Parity**

This release introduces a completely new annotation-based testing API with convention-over-configuration design, plus production-parity HST configuration via brXM's `ConfigurationConfigService`.

**Quick Start Guides:**
- [Getting Started](user-guide/getting-started.md) - First test in 3 steps
- [Quick Reference](user-guide/quick-reference.md) - Annotation options at a glance
- [Common Patterns](user-guide/common-patterns.md) - Recipes for all test types
- [Troubleshooting](user-guide/troubleshooting.md) - Common issues and fixes

#### Annotation-Based Testing (New)

Three new annotations provide zero-boilerplate testing with automatic field injection - no inheritance required:

| Annotation | Use Case | Injected Field |
|------------|----------|----------------|
| `@BrxmComponentTest` | HST component testing | `DynamicComponentTest` |
| `@BrxmPageModelTest` | Page Model API testing | `DynamicPageModelTest` |
| `@BrxmJaxrsTest` | JAX-RS endpoint testing | `DynamicJaxrsTest` |

**Minimal Example (Parameter Injection - Recommended):**
```java
@BrxmComponentTest  // Zero config for standard project layouts!
public class MyComponentTest {

    @Test
    void testComponent(DynamicComponentTest brxm) {  // Parameter injection - no IDE warnings!
        assertNotNull(brxm.getHstRequest());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
```

**Alternative: Field Injection** (use when you need the instance in `@BeforeEach` or nested classes):
```java
@BrxmComponentTest
public class MyComponentTest {
    @SuppressWarnings("unused")  // Injected by extension
    private DynamicComponentTest brxm;

    @BeforeEach
    void setup() {
        brxm.setSiteContentBasePath("/content/documents/mysite");
    }
}
```

**Key Features:**
- **Auto-detection of bean packages** - Discovers packages from `project-settings.xml`, classpath scanning, or `pom.xml`
- **Auto-detection of node types** - Scans `@Node(jcrType="...")` annotations automatically
- **Auto-detection of HST root** - Derives from project name or Maven artifactId
- **Auto-detection of test resources** - Finds YAML/CND files in conventional locations
- **Parameter injection** (recommended) - Standard JUnit 5 pattern with no IDE warnings
- **Field injection** (alternative) - For `@BeforeEach`/`@Nested` scenarios
- **Nested test support** - Full JUnit 5 `@Nested` class support with field inheritance

**Bean Package Auto-Detection Strategy Chain:**

| Priority | Strategy | Source |
|----------|----------|--------|
| 10 | ProjectSettingsStrategy | `project-settings.xml` (`selectedProjectPackage`, `selectedBeansPackage`) |
| 20 | ClasspathNodeAnnotationStrategy | Scans classpath for `@Node`-annotated classes |
| 30 | PomGroupIdStrategy | Derives from `pom.xml` groupId |
| 40 | TestClassHeuristicStrategy | Appends `.beans`/`.model`/`.domain` to test package |

**Explicit Configuration (when needed):**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},      // Override auto-detection
    resources = {HelloResource.class},          // JAX-RS resources
    loadProjectContent = true                   // Use production HCM modules
)
```

**New Classes:**
- `brut-components`: `BrxmComponentTest`, `DynamicComponentTest`, `BrxmComponentTestExtension`
- `brut-resources`: `BrxmPageModelTest`, `BrxmJaxrsTest`, `DynamicPageModelTest`, `DynamicJaxrsTest`
- `brut-common` (strategy package): `BeanPackageStrategy`, `DiscoveryContext`, `BeanPackageStrategyChain`, `ProjectSettingsStrategy`, `ClasspathNodeAnnotationStrategy`, `PomGroupIdStrategy`, `TestClassHeuristicStrategy`

#### Code Quality: Clean Architecture Refactoring

Major refactoring following Uncle Bob's Clean Code principles:

**Consolidated Exception Handling:**
- Unified `BrutTestConfigurationException` in `brut-common` replaces duplicate exception classes
- Factory methods for semantic error creation: `missingAnnotation()`, `missingField()`, `setupFailed()`, `bootstrapFailed()`, `resourceNotFound()`

**Consolidated Logging:**
- Unified `TestConfigurationLogger` in `brut-common` provides consistent configuration logging
- Methods: `logConfiguration()`, `logSuccess()`, `logFailure()`, `logBeanPatterns()`, `logSpringConfigs()`

**Extracted Responsibilities from ConfigServiceBootstrapStrategy:**
- `RuntimeTypeStubber` - Handles runtime stubbing of missing JCR namespaces and node types
- `JcrNodeSynchronizer` - Handles JCR node synchronization between source and target trees
- `ConfigServiceReflectionBridge` - Encapsulates reflection calls to ConfigService methods
- Result: ConfigServiceBootstrapStrategy reduced from ~1650 to ~1180 lines (28% reduction)

**Shared Utilities:**
- `TestInstanceInjector` in `brut-common` - Shared field injection for JUnit 5 extensions
- `AbstractBrutRepository` in `brut-common` - Shared CND registration and namespace handling

**Key Features:**

* **ConfigServiceRepository** - New repository implementation using brXM's production ConfigService for HST bootstrap
* **Production-Identical Structure** - HST configuration created using the exact same code path as production brXM
* **Zero Maintenance** - Changes in brXM's HST structure propagate automatically (no manual updates needed)
* **Explicit Module Loading** - Uses ModuleReader to load only test HCM modules (avoids framework dependency conflicts)
* **Works with Both Test Types** - Compatible with AbstractJaxrsTest and AbstractPageModelTest
* **Strategy Pattern** - Pluggable bootstrap strategies (ConfigService or manual fallback)

**Usage:**

Requires HCM module descriptor and configuration files:

```java
// 1. Create HCM module descriptor: src/test/resources/META-INF/hcm-module.yaml
group:
  name: myproject-test
project: myproject-test
module:
  name: test-config

// 2. Create HCM config: src/test/resources/hcm-config/hst/demo-hst.yaml
definitions:
  config:
    /hst:myproject:
      jcr:primaryType: hst:hst
    /hst:myproject/hst:configurations:
      jcr:primaryType: hst:configurations
    # ... your HST structure

// 3. Override repository in Spring XML
<bean id="javax.jcr.Repository"
      class="org.bloomreach.forge.brut.resources.ConfigServiceRepository"
      init-method="init" destroy-method="close">
    <constructor-arg ref="cndResourcesPatterns"/>
    <constructor-arg ref="contributedCndResourcesPatterns"/>
    <constructor-arg ref="yamlResourcesPatterns"/>
    <constructor-arg ref="contributedYamlResourcesPatterns"/>
    <constructor-arg value="myproject"/>
</bean>

// 4. Use in tests
@Override
protected List<String> contributeSpringConfigurationLocations() {
    return Arrays.asList("/org/example/config-service-jcr.xml");
}
```

**Documentation:**
- See `docs/config-service-repository.md` for detailed usage guide
- Example integration tests in `demo/site/components/src/test/java/org/example/`

**Architecture:**
- Uses `ModuleReader` for explicit module loading (no classpath scanning)
- Reflection-based access to package-private ConfigService methods
- Bootstrap strategy pattern for flexible initialization

#### 2. HTTP Session Support

MockHstRequest now supports HTTP sessions via Spring's MockHttpSession:

```java
// Get or create session (lazy initialization)
HttpSession session = brxm.getHstRequest().getSession();
session.setAttribute("user", userProfile);

// Inject a mock session
brxm.getHstRequest().setSession(mockSession);

// Manual session cleanup
brxm.getHstRequest().invalidateSession();
```

**Session Isolation:**
- Sessions are automatically invalidated in `setupForNewRequest()` for multi-test classes
- Prevents session state from leaking between tests
- Works with JAX-RS and PageModel tests

#### 3. One-Liner ConfigService Integration

Production-parity configuration simplified to a single parameter:

```java
@BrxmJaxrsTest(
    loadProjectContent = true  // That's it - beanPackages auto-detected!
)
```

Automatically uses ConfigServiceRepository with HCM modules - no manual Spring XML configuration required.

**Implementation Details:**
- Auto-generates Spring configuration with ConfigServiceRepository bean
- Loads minimal framework module for core brXM node types (editor, hipposysedit, webfiles)
- Works with all 3 annotation types (PageModel, JAX-RS, Component)
- Permissive CNDs ensure test bootstrapping success

**Benefits:**
- Zero Spring XML boilerplate for ConfigService setup
- Production-parity configuration in tests
- Explicit HCM module loading (no classpath pollution)

**Minimal Framework CNDs:**
The embedded minimal framework uses permissive node type definitions to prioritize test execution over strict validation. This is acceptable for unit testing scenarios and documented in the CND headers.

#### 4. Enhanced Auto-Detection

Smart convention-over-configuration improvements for annotation-based tests:

**Multi-File Spring Config Detection:**
- Automatically detects multiple Spring XML files per test type
- JAX-RS tests check: `custom-jaxrs.xml`, `annotation-jaxrs.xml`, `rest-resources.xml`, `jaxrs-config.xml`
- PageModel tests check: `custom-pagemodel.xml`, `annotation-pagemodel.xml`, `custom-component.xml`, `component-config.xml`
- All matching files are automatically included

**ConfigService-Aware Resource Detection:**
- CND patterns auto-detected only when `loadProjectContent=false` (avoids conflicts)
- YAML patterns auto-detected only when `loadProjectContent=false` (avoids conflicts)
- Prevents duplicate loading between ConfigService and legacy import mechanisms

**Example:**
```java
@BrxmJaxrsTest(beanPackages = {"org.example.model"})
// NO springConfigs needed - auto-detects custom-jaxrs.xml + rest-resources.xml
public class MyTest {
    private DynamicJaxrsTest brxm;

    @Test
    void test() {
        // Both Spring configs loaded automatically
    }
}
```

**Benefits:**
- Less configuration required in annotations
- Supports projects with multiple Spring config files
- Intelligent conflict prevention with ConfigService
- Explicit annotation values always override auto-detection

#### 5. Fluent Test Utilities

Chainable APIs for common test operations:

**Typed JSON Responses with `executeAs()`:**
```java
// Two lines become one - JSON deserialization built into the fluent API
User user = brxm.request()
    .get("/site/api/user/123")
    .executeAs(User.class);

assertEquals("John", user.getName());
```

**Request Body with `withJsonBody()`:**
```java
// POST with JSON body - sets Content-Type automatically
User created = brxm.request()
    .post("/site/api/users")
    .withJsonBody("{\"name\": \"John\"}")
    .executeAs(User.class);

// Or serialize an object directly
User input = new User("Jane", 25);
brxm.request()
    .post("/site/api/users")
    .withJsonBody(input)  // Auto-serializes to JSON
    .execute();
```

**Response Status Codes with `executeWithStatus()`:**
```java
// Get both status code and typed body
Response<User> response = brxm.request()
    .post("/site/api/users")
    .withJsonBody(newUser)
    .executeWithStatus(User.class);

assertThat(response.status()).isEqualTo(201);
assertThat(response.isSuccessful()).isTrue();
assertThat(response.body().getName()).isEqualTo("John");

// Error response handling
Response<ErrorDto> error = brxm.request()
    .get("/site/api/missing")
    .executeWithStatus(ErrorDto.class);

assertThat(error.status()).isEqualTo(404);
assertThat(error.isClientError()).isTrue();
```

**Fluent Request Builder:**
```java
@BrxmJaxrsTest
public class FluentApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testFluentRequest() {
        String response = brxm.request()
            .get("/site/api/news")
            .withHeader("X-Custom", "value")
            .queryParam("category", "tech")
            .execute();

        assertTrue(response.contains("news"));
    }
}
```

**Auto-Managed Repository Sessions:**
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
- `executeAs(Class)` - JSON response deserialization in one line
- `executeWithStatus(Class)` - Get HTTP status code + typed body together
- `withJsonBody(String)` / `withJsonBody(Object)` - JSON request body in one line
- `Response<T>` wrapper with `status()`, `body()`, `isSuccessful()`, `isClientError()`
- Reduces boilerplate for request setup
- Auto-cleanup of JCR sessions (try-with-resources)
- Method chaining for readable test code
- Works with both PageModel and JAX-RS tests

#### 6. Enhanced Error Messages

Context-rich error messages with actionable fix suggestions:

**Before:**
```
IllegalStateException: Test instance not initialized. This should not happen.
```

**After:**
```
Invalid test state: Test instance not available in beforeEach

Expected: DynamicPageModelTest should be initialized in beforeAll
Actual: Instance is null

This indicates a bug in BRUT or misuse of test infrastructure.
Please report this issue with full stack trace.
```

**Features:**
- **Custom Exception Types** - `BrutConfigurationException` and `ComponentConfigurationException` with semantic factory methods
- **Missing Annotation Errors** - Shows import statement and example usage
- **Missing Field Errors** - Lists all scanned fields and their types to help identify typos
- **Bootstrap Failures** - Shows complete configuration context (bean patterns, Spring configs, HST root)
- **Configuration Summary Logging** - Detailed startup logs showing resolved configuration
- **Step-by-Step Progress** - ConfigServiceRepository logs each initialization step

**Configuration Summary Example:**
```
========================================
PageModel Configuration Summary
========================================
Test Class: org.example.NewsPageModelTest

Bean Patterns:
  - classpath*:org/example/beans/*.class

Spring Configs:
  - /org/example/custom-pagemodel.xml [AUTO-DETECTED]

HST Root: /hst:myproject

========================================
PageModel Initialization Starting
========================================
```

**Error Context Example:**
```
Bootstrap failed during: PageModel test initialization

Configuration attempted:
  Bean patterns: classpath*:org/example/beans/*.class
  Spring configs: /org/example/custom-pagemodel.xml
  HST root: /hst:myproject

Root cause: FileNotFoundException: /org/example/custom-pagemodel.xml

To fix:
  1. Check that all specified resources exist on classpath
  2. Verify bean packages contain valid Spring components
  3. Ensure Spring config files are well-formed XML
  4. Check logs above for more specific error details
```

**Benefits:**
- Faster debugging with clear error context
- No more "this should not happen" messages
- Configuration visibility for troubleshooting
- Actionable fix suggestions in every error
- Field scan results show exactly what was found vs. expected

### 5.0.1

**Multi-Test Support and Stability Improvements**

This release focuses on enabling reliable testing with multiple test methods and improving overall framework stability.

**Key Improvements:**

* **JUnit 4 `@Before` pattern support** - Component manager now properly shared across test instances while maintaining per-test isolation
* **Thread-safe initialization** - ReentrantLock-based synchronization prevents race conditions in parallel test execution
* **RequestContextProvider support** - JAX-RS resources can now access `RequestContextProvider.get()` with proper ThreadLocal management
* **Null-safety and error handling** - Defensive checks throughout with clear error messages for initialization issues
* **Exception visibility** - Full stack traces logged and propagated for easier debugging

**Usage:**

Both JUnit 4 and JUnit 5 patterns are supported:

```java
// JUnit 5 (Recommended)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MyTest extends AbstractJaxrsTest {
    @BeforeAll public void init() { super.init(); }
    @BeforeEach public void beforeEach() { setupForNewRequest(); }
}

// JUnit 4 (Fully supported)
public class MyTest extends AbstractJaxrsTest {
    @Before public void setUp() { super.init(); /* custom setup */ }
}
```

### 5.0.0
Compatibility with brXM version 16.0.0

### 4.0.1
Compatibility with brXM version 15.0.1+

### 4.0.0
Compatibility with brXM version 15.0.0

### 3.0.0
Compatibility with brXM version 14.0.0-2

### 2.1.2
Compatibility with brXM version 13.4.0

* Fixed breaking changes coming from brXM due to dynamic beans feature. Dynamic beans are not supported in brut.
* Subclasses of SimpleComponentTest in brut-components can now provide their own SpringComponentManager
* Fixed a bug in brut-resources where servletContext was null in SpringComponentManager (dynamic beans regression)

### 2.0.0

<p class="smallinfo">Release date: 30 March 2019</p>

+ Upgrade to brXM 13

### 1.0.1

<p class="smallinfo">Release date: 30 March 2019</p>

+ Apply Bloomreach Forge best practices and publish it on the Forge, under different Maven coordinates of the artifacts.
+ Available for brXM 12.x (developed and tested on 12.6.0)

### 1.0.0
+ Older release with different group id