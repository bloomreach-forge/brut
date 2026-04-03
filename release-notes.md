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
| 16.7.0   | 5.5.0   |
| 16.7.0   | 5.4.0   |
| 16.7.0   | 5.3.x   |
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

### 5.5.0

**HST Bootstrap Reliability and Multi-Tenant Fixes**

This release addresses failures when loading projects that use addon or multi-tenant HST configurations where dependency module node types or namespaces are absent from the test classpath.

#### Bug Fixes

* **Namespace pre-registration** — `ConfigServiceBootstrapStrategy` pre-registers all namespaces declared in the `ConfigurationModel` before invoking `applyNamespacesAndNodeTypes`. Prevents `NamespaceException` failures caused by ordering differences between addon module declarations.

* **Missing supertype pre-stubbing** — Before CND registration, BRUT parses all CND files in the model and stubs any referenced supertypes that are neither defined in the model nor already registered in Jackrabbit. Prevents `invalid supertype` errors when dependency module types are absent.

* **Retry loops for bootstrap steps** — `applyNamespacesAndNodeTypes` and the config delta step are each wrapped in retry loops. The namespace step recovers from `NamespaceException` and invalid supertype errors. The delta step detects "is not a mixin node type" errors and re-stubs the offending type as a mixin before retrying.

* **Unmapped mount patching** — After config delta, BRUT sets `hst:ismapped=false` on any `hst:hosts` mount whose `hst:mountpoint` cannot be resolved within the active HST root. Prevents `HstComponentConfigurationNotFoundException` in multi-tenant and multi-channel setups where not all channels are present.

#### New API

* **`RuntimeTypeStubber.registerStubMixinNodeType(session, nodeType, stubbedNamespaces)`** — Registers a minimal mixin CND stub. Used internally by the supertype pre-registration and mixin recovery loops; also available for advanced test setup.

* **`SpringComponentManager.getInternalApplicationContext()`** — Exposes the Spring `ApplicationContext` created during `initialize()`.

#### Improvements

* **`WebApplicationContext` registration** — `AbstractPageModelTest` registers a `GenericWebApplicationContext` on the test servlet context at startup. Components that call `WebApplicationContextUtils.getWebApplicationContext(servletContext)` now receive a valid context instead of `null`.

* **Bootstrap log noise reduction** — `ConfigurationTreeBuilder` and `ConfigurationConfigService` loggers are temporarily suppressed during bootstrap. Reduces noisy output when loading large HCM module sets.

* **Internal refactoring** — `LoadedModules` and `MissingDependencyInfo` inner classes converted to Java records.

---

### 5.4.0

**Addon HCM Module Auto-Discovery ([FORGE-626](https://issues.onehippo.com/browse/FORGE-626))**

Tests that use `loadProjectContent = true` now automatically include non-platform, non-project HCM modules from the test classpath. Previously, every addon module had to be listed explicitly via `dependencyHcmModules`.

* **Auto-discovery** — BRUT scans the test classpath for `hcm-module.yaml` descriptors that are not part of the brXM platform or the project under test, and loads them automatically. **Upgrade note:** Tests using `loadProjectContent = true` may pick up addon modules that were not loaded before. If a newly-discovered module causes conflicts, use `excludeDependencyHcmModules` to opt out.
* **`excludeDependencyHcmModules`** — New parameter on `@BrxmJaxrsTest` and `@BrxmPageModelTest`. Accepts a list of module names to exclude from auto-discovery when a particular addon causes conflicts.
* **`dependencyHcmModules`** — Repurposed from an opt-in list to a force-include override. Only needed for modules that auto-discovery would miss (e.g., modules with non-standard descriptors).
* **Load order fix** — Addon modules now load before project modules, ensuring addon CND node types are registered before project configuration is applied.

---

### 5.3.0

**Page Model API v1.0 Document Resolution + Ergonomic Content API + Test Performance**

This release fixes silent data loss when parsing Page Model API v1.0 responses, adds helper methods that replace 4-step stream pipelines with single calls, and significantly reduces test-suite startup overhead through JCR repository sharing.

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

#### Performance Improvements

* **JCR repository sharing** — `BrxmComponentTestExtension` now reuses a single `BrxmTestingRepository` across all test classes that share the same configuration fingerprint (bean packages, test resource path, content YAML, and content root). Jackrabbit is bootstrapped once per fingerprint and shut down once at the end of the suite via a JUnit 5 `CloseableResource` in the root store. Each test class still opens its own JCR session and gets a fresh set of mock objects.

* **ConfigurationModel caching** — `ConfigServiceBootstrapStrategy` caches the built `ConfigurationModel` keyed by the SHA-256 fingerprint of its source files. Subsequent test classes with the same HCM modules skip the full `ConfigService` parse step, avoiding repeated YAML/CND processing.

* **Node type registration guard** — `BaseComponentTest.shouldRegisterBaseNodeTypes()` delegates to `BrxmTestingRepository.recordInitialization("__baseNodeTypes__")`. On a shared repository the ~50 `hasNodeType()` calls that previously ran at the start of every test class now run at most once per repository.

* **`BrxmTestingRepository.recordInitialization(key)`** — New first-caller-wins gate. Returns `true` the first time a key is seen (proceed with the operation) and `false` on subsequent calls (skip). Used internally by `BaseComponentTest` for both node-type registration and skeleton YAML import.

* **`BaseComponentTest.setRepository(BrxmTestingRepository)`** — New API that allows a pre-bootstrapped repository to be injected before `setup()` is called. The extension uses this to share a single repository instance across test classes.

* **RuntimeTypeStubber log level** — Stub-specific messages in `RuntimeTypeStubber` raised back from INFO/DEBUG to WARN (reverting the normalization applied in 5.2.0 for these messages only). These stubs indicate missing node type definitions in the project's CND files and warrant developer attention.

#### Parallel Execution Contract

Class-level parallel execution (multiple test classes running concurrently) is supported with the following caveats:

- Each class receives its own JCR session, `DynamicComponentTest` instance, and mock objects.
- `getOrComputeIfAbsent()` in the JUnit 5 root store guarantees a single bootstrap per fingerprint.
- `BrxmTestingRepository.appliedInitKeys` uses a `Collections.synchronizedSet` — individual `add()` calls are atomic, but callers should not assume the associated operation is complete immediately after another thread's `add()` returns `false`.

**Method-level parallel execution** (`@Execution(CONCURRENT)` on test methods within the same class) is **not supported**. All methods in a class share a single `DynamicComponentTest` instance, a single JCR session, and non-thread-safe mock objects. Enabling method-level concurrency will cause data corruption and intermittent failures.

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
* **Log level normalization** — Operational messages from `ConfigServiceBootstrapStrategy`, `JcrNodeSynchronizer`, `ConfigServiceReflectionBridge`, and `SiteContentBaseResolverValve` downgraded from WARN to INFO/DEBUG. These are expected conditions during test bootstrap, not warnings. Note: `RuntimeTypeStubber` messages were temporarily downgraded here but raised back to WARN in 5.3.0, as stubs indicate potentially missing CND definitions.
* **Log noise reduction** — `VirtualHostsService` (duplicate mount alias errors) suppressed; `HstDelegateeFilterBean` reduced from ALL to WARN.
* **Javadoc scope warnings** — `@BrxmComponentTest`, `@BrxmJaxrsTest`, and `@BrxmPageModelTest` annotations now document the `<scope>test</scope>` requirement and the consequences of omitting it.

### 5.1.0

**Annotation-based testing API and production-parity configuration**

Three new annotations replace the abstract class approach:

| Annotation | Test type | Injected field |
|------------|-----------|----------------|
| `@BrxmComponentTest` | HST components | `DynamicComponentTest` |
| `@BrxmPageModelTest` | Page Model API | `DynamicPageModelTest` |
| `@BrxmJaxrsTest` | JAX-RS endpoints | `DynamicJaxrsTest` |

Tests no longer need to extend a base class. Annotate the test class and BRUT injects the test harness via field or parameter injection.

- **`loadProjectContent = true`** — Loads your project's real HCM modules via `ConfigServiceRepository`, giving tests the same HST configuration as production without manual Spring XML setup.
- **Auto-detection** — Bean packages, HST root, and Spring config files are derived from project conventions. Explicit configuration is only needed when defaults don't apply.
- **Fluent request API** — `brxm.request().get(...).queryParam(...).execute()` replaces manual `setRequestURI` + `invokeFilter` calls. `executeAs(Class)` deserialises JSON responses directly; `executeWithStatus(Class)` returns status code and body together.
- **HTTP session support** — `MockHstRequest.getSession()`, `setSession()`, and `invalidateSession()` added.
- **`RequestContextProvider.get()` works** inside JAX-RS resources under test.
- **Performance** — JCR repository shared across test classes with the same configuration fingerprint; `ConfigurationModel` cached by source file hash. Jackrabbit bootstraps once per fingerprint rather than once per class.
- **Internal refactoring** — `ConfigServiceBootstrapStrategy` split into `RuntimeTypeStubber`, `JcrNodeSynchronizer`, and `ConfigServiceReflectionBridge`.

See [Getting Started](user-guide/getting-started.md) and [Common Patterns](user-guide/common-patterns.md) for usage examples.

### 5.0.1

**Multi-Test Support and Stability Improvements**

This release focuses on enabling reliable testing with multiple test methods and improving overall framework stability.

**Key Improvements:**

* **JUnit 4 `@Before` pattern support** - Component manager now properly shared across test instances while maintaining per-test isolation
* **Servlet context registration** - `ReentrantLock`-based synchronization prevents duplicate `HippoWebappContext` registration when multiple test classes run sequentially
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