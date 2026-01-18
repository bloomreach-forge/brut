# ConfigServiceRepository - Production Parity Testing

## Overview

`ConfigServiceRepository` leverages brXM's production `ConfigurationConfigService` to create HST configuration in tests, providing production-identical JCR structure without manual node construction.

**Key Benefits:**
- **Production Parity**: Uses exact same bootstrap code as real brXM
- **Zero Maintenance**: brXM structure changes propagate automatically
- **Explicit Control**: Loads only your test HCM modules (no framework dependencies)
- **Proven**: Works with both JAX-RS and PageModel tests

## Quick Start

### 1. Add HCM Module Descriptor

**File**: `src/test/resources/META-INF/hcm-module.yaml`

```yaml
group:
  name: myproject-test
project: myproject-test
module:
  name: test-config
```

**Important**: Do NOT include `config:` or `after:` sections. ModuleReader discovers config by directory convention.

### 2. Add HCM Configuration

**Directory**: `src/test/resources/hcm-config/hst/`

**File**: `demo-hst.yaml`

```yaml
definitions:
  config:
    /hst:myproject:
      jcr:primaryType: hst:hst
    /hst:myproject/hst:sites:
      jcr:primaryType: hst:sites
    /hst:myproject/hst:sites/myproject:
      jcr:primaryType: hst:site
      hst:content: /content/documents/myproject
    /hst:myproject/hst:configurations:
      jcr:primaryType: hst:configurations
    /hst:myproject/hst:configurations/myproject:
      jcr:primaryType: hst:configuration
    /hst:myproject/hst:configurations/myproject/hst:sitemap:
      jcr:primaryType: hst:sitemap
    /hst:myproject/hst:configurations/myproject/hst:sitemap/root:
      jcr:primaryType: hst:sitemapitem
      hst:componentconfigurationid: hst:pages/homepage
    /hst:myproject/hst:configurations/myproject/hst:pages:
      jcr:primaryType: hst:pages
    /hst:myproject/hst:configurations/myproject/hst:pages/homepage:
      jcr:primaryType: hst:component
    /hst:myproject/hst:hosts:
      jcr:primaryType: hst:virtualhosts
```

**Note**: BRUT uses `/hst:myproject` as HST root (not `/hst:hst`) for test isolation.

### 3. Override Repository Bean

**File**: `src/test/resources/org/example/config-service-jcr.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <bean id="javax.jcr.Repository"
        class="org.bloomreach.forge.brut.resources.ConfigServiceRepository"
        init-method="init"
        destroy-method="close">
    <constructor-arg ref="cndResourcesPatterns"/>
    <constructor-arg ref="contributedCndResourcesPatterns"/>
    <constructor-arg ref="yamlResourcesPatterns"/>
    <constructor-arg ref="contributedYamlResourcesPatterns"/>
    <constructor-arg value="myproject"/>  <!-- project namespace -->
  </bean>

</beans>
```

### 4. Use in Your Test

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MyIntegrationTest extends AbstractJaxrsTest {

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList(
            "/org/example/config-service-jcr.xml",  // ConfigServiceRepository override
            "/org/example/custom-jaxrs.xml",
            "/org/example/rest-resources.xml"
        );
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";  // BRUT uses project-specific root
    }

    @Test
    void testHstStructureCreated() throws Exception {
        Repository repo = getComponentManager().getComponent(Repository.class);
        Session session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

        assertTrue(session.nodeExists("/hst:myproject"));
        assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject"));
    }
}
```

## How It Works

### Architecture

ConfigServiceRepository uses **ModuleReader** for explicit module loading:

```
Test Resources (target/test-classes)
├── META-INF/hcm-module.yaml          ← Module descriptor
└── hcm-config/                       ← Config discovered by convention
    └── hst/*.yaml                    ← Your HST definitions
                 ↓
           ModuleReader                ← Loads module explicitly (no classpath scan)
                 ↓
    ConfigurationModelImpl.build()    ← Builds configuration model
                 ↓
      ConfigurationConfigService      ← brXM's production bootstrap service
                 ↓
          JCR Repository               ← Production-identical structure
```

**Key Insight**: We use `ModuleReader.read(path, false)` to load test modules explicitly by path, avoiding `ClasspathConfigurationModelReader` which scans for ALL modules (including framework JARs with unmet dependencies).

### Initialization Flow

1. **Register CNDs**: Node type definitions from classpath
2. **Load Test Modules**: ModuleReader finds `META-INF/hcm-module.yaml` in `target/test-classes`
3. **Discover Config**: ModuleReader automatically finds `hcm-config/**/*.yaml`
4. **Build Model**: Creates `ConfigurationModelImpl` with only test modules
5. **Apply via ConfigService**: Uses reflection to call package-private ConfigService methods
6. **Create JCR Nodes**: ConfigService writes production-identical structure
7. **Import YAML Content**: Additional content from YAML patterns
8. **Recalculate Paths**: Update hippo:paths properties

## HCM Module Format

### Correct Format (ModuleReader)

```yaml
# META-INF/hcm-module.yaml
group:
  name: test-group
project: test-project
module:
  name: test-config
  # NO 'config:' key here! ModuleReader discovers by convention.
```

ModuleReader automatically discovers:
- `hcm-config/**/*.yaml` → Configuration
- `hcm-content/**/*.yaml` → Content
- `namespaces/**/*.cnd` → Node types

### Common Mistakes

```yaml
# WRONG - config: key is invalid for ModuleReader
module:
  name: test-config
  config:              # ← ERROR: Not valid!
    source: /hcm-config

# WRONG - Missing dependencies cause errors
group:
  name: test-group
  after:
    - hippo-cms       # ← ERROR: hippo-cms doesn't exist in test env
```

### HCM Config Path Format

HCM config uses **flat paths**, not nested YAML:

```yaml
# ✅ CORRECT - Flat paths
definitions:
  config:
    /hst:myproject:
      jcr:primaryType: hst:hst
    /hst:myproject/hst:sites:
      jcr:primaryType: hst:sites
    /hst:myproject/hst:sites/myproject:
      jcr:primaryType: hst:site

# ❌ WRONG - Nested structure (this creates wrong paths)
definitions:
  config:
    /hst:myproject:
      jcr:primaryType: hst:hst
      /hst:sites:                    # Wrong - creates /hst:sites not /hst:myproject/hst:sites
        jcr:primaryType: hst:sites
```

## Works With Both Test Types

### JAX-RS Tests

```java
public class MyJaxrsTest extends AbstractJaxrsTest {
    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/org/example/config-service-jcr.xml");
    }

    @Test
    void testEndpoint() {
        getHstRequest().setRequestURI("/site/api/hello/world");
        String response = invokeFilter();
        assertEquals("Hello, World! world", response);
    }
}
```

### PageModel Tests

```java
public class MyPageModelTest extends AbstractPageModelTest {
    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/org/example/config-service-jcr.xml");
    }

    @Test
    void testPageModel() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        String response = invokeFilter();
        JsonNode json = new ObjectMapper().readValue(response, JsonNode.class);
        assertTrue(json.get("page").size() > 0);
    }
}
```

## Comparison: SkeletonRepository vs ConfigServiceRepository

| Aspect | SkeletonRepository | ConfigServiceRepository |
|--------|-------------------|------------------------|
| **HST Bootstrap** | Minimal hardcoded structure | Full production ConfigService |
| **Maintenance** | Manual updates when brXM changes | Automatic (uses production code) |
| **Structure** | Basic hst:hst node only | Complete HST tree |
| **Setup** | Zero configuration | Requires HCM module + config |
| **Speed** | Faster (minimal setup) | Slightly slower (full bootstrap) |
| **Use Case** | Simple unit tests | Integration tests needing real HST |
| **Production Parity** | No | Yes (exact same code path) |

## Troubleshooting

### "ParserException: Key 'config' is not allowed"

**Cause**: Using ClasspathConfigurationModelReader format in hcm-module.yaml

**Fix**: Remove `config:` section. ModuleReader discovers by convention:

```yaml
# Wrong
module:
  name: test-config
  config:             # Remove this
    source: /hcm-config

# Correct
module:
  name: test-config
```

### "MissingDependencyException: missing dependency 'hippo-cms'"

**Cause**: Module declares dependency on framework module not present in test

**Fix**: Remove `after:` section from group:

```yaml
# Wrong
group:
  name: test-group
  after:
    - hippo-cms       # Remove this

# Correct
group:
  name: test-group
```

### HST nodes not created

**Cause**: HCM config files missing or in wrong location

**Fix**:
- Verify `hcm-config/` directory exists in test resources
- Check YAML syntax
- Ensure paths use `/hst:myproject` (not `/hst:hst`)

### "Module descriptor not found: .../META-INF/META-INF/hcm-module.yaml"

**Cause**: Bug in path resolution (fixed in 5.1.0-SNAPSHOT)

**Fix**: Upgrade to latest version

## Implementation Details

### Reflection for Package-Private Methods

ConfigService methods are package-private. We use reflection:

```java
Method method = ConfigurationConfigService.class.getDeclaredMethod(
    "computeAndWriteDelta",
    ConfigurationModel.class,
    ConfigurationModel.class,
    Session.class,
    boolean.class
);
method.setAccessible(true);
method.invoke(configService, baseline, update, session, true);
method.setAccessible(false);
```

### Bootstrap Strategy Pattern

ConfigServiceRepository uses pluggable strategies:

- **ConfigServiceBootstrapStrategy** - Uses ConfigService (preferred)
  - Auto-detects via `canHandle()` - checks for `META-INF/hcm-module.yaml`
  - Loads modules explicitly with ModuleReader

- **ManualBootstrapStrategy** - Minimal setup (fallback)
  - Always returns true for `canHandle()`
  - Creates basic `/hst:hst` node only

Strategy selection is automatic based on classpath resources.

## Example Project Structure

```
src/test/
├── java/org/example/
│   └── MyIntegrationTest.java
└── resources/
    ├── META-INF/
    │   └── hcm-module.yaml              # Module descriptor
    ├── hcm-config/
    │   └── hst/
    │       └── demo-hst.yaml            # HST configuration
    └── org/example/
        └── config-service-jcr.xml       # Repository override
```

## See Also

- **Example Tests**:
  - `demo/site/components/src/test/java/org/example/ConfigServiceRepositoryIntegrationTest.java`
  - `demo/site/components/src/test/java/org/example/ConfigServicePageModelIntegrationTest.java`

## Version

Available since: BRUT 5.1.0
