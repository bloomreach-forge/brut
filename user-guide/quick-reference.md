# BRUT Quick Reference Guide

## For Developers

### 1. Add Dependency

```xml
<!-- For JAX-RS / Page Model API Testing -->
<dependency>
  <groupId>org.bloomreach.forge.brut</groupId>
  <artifactId>brut-resources</artifactId>
  <version>${brut.version}</version>
  <scope>test</scope>
</dependency>

<!-- For Component Testing -->
<dependency>
  <groupId>org.bloomreach.forge.brut</groupId>
  <artifactId>brut-components</artifactId>
  <version>${brut.version}</version>
  <scope>test</scope>
</dependency>
```

### 2. Create Your First Test

**JAX-RS API Test:**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmJaxrsTest(beanPackages = {"org.example.model"})
public class MyApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void testEndpoint() {
        brxm.getHstRequest().setRequestURI("/site/api/hello");
        String response = brxm.invokeFilter();
        assertEquals("Hello, World!", response);
    }
}
```

**Page Model API Test:**
```java
package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmPageModelTest(beanPackages = {"org.example.beans"})
public class MyPageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void testComponent() {
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        String response = brxm.invokeFilter();
        assertTrue(response.contains("page"));
    }
}
```

**Component Test:**
```java
package org.example;

import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.bloomreach.forge.brut.components.annotation.DynamicComponentTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@BrxmComponentTest(beanPackages = {"org.example.beans"})
public class MyComponentTest {
    private DynamicComponentTest brxm;

    @Test
    void testComponent() {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
```

### 3. Configuration Options

**All Annotations Support:**

```java
@BrxmJaxrsTest(
    // Required
    beanPackages = {"org.example.model", "org.example.beans"},

    // Optional - auto-detected if not specified
    hstRoot = "/hst:myproject",
    springConfigs = {"/org/example/custom-jaxrs.xml"},
    addonModules = {"/org/example/addon"},

    // Optional - production config
    useConfigService = true  // Loads real HCM modules
)
```

### 4. Fluent APIs

**Request Builder:**
```java
@Test
void testWithFluentApi() {
    String response = brxm.request()
        .uri("/site/api/news")
        .method(HttpMethod.POST)
        .header("Content-Type", "application/json")
        .queryParam("category", "tech")
        .execute();

    assertTrue(response.contains("news"));
}
```

**Repository Access:**
```java
@Test
void testRepositoryAccess() {
    try (RepositorySession session = brxm.repository()) {
        Node newsNode = session.getNode("/content/documents/news");
        assertEquals("hippo:handle", newsNode.getPrimaryNodeType().getName());
    }
    // Auto-cleanup on try-with-resources exit
}
```

### 5. Production Config (ConfigService)

**One-Liner:**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    useConfigService = true  // Uses HCM modules from classpath
)
```

**HCM Structure:**
```
src/test/resources/
├── META-INF/
│   └── hcm-module.yaml
├── hcm-config/
│   └── hst/
│       ├── configurations.yaml
│       ├── hosts.yaml
│       └── sites.yaml
└── hcm-content/
    └── content/
        └── documents/
```

---

## For AI Agents

### Implementation Pattern

When implementing BRUT tests, follow this exact structure:

**1. Annotation Declaration:**
```
Annotation: @Brxm{Type}Test where Type = PageModel | Jaxrs | Component
Required Parameters: beanPackages (String array)
Optional Parameters: hstRoot, springConfigs, addonModules, useConfigService
```

**2. Field Declaration:**
```
Type: Dynamic{Type}Test where Type = PageModel | Jaxrs | Component
Visibility: Any (private, protected, public, package-private)
Name: Any valid identifier (convention: "brxm")
Initialization: Automatic by JUnit extension
```

**3. Test Method Pattern:**
```java
@Test
void descriptiveName() {
    // 1. Setup request
    brxm.getHstRequest().setRequestURI("/uri");

    // 2. Execute
    String response = brxm.invokeFilter();

    // 3. Assert
    assertEquals(expected, actual);
}
```

### Auto-Detection Rules

**Bean Packages:**
- NO auto-detection - MUST be specified explicitly
- Format: Package notation (e.g., "org.example.beans")
- Converted to classpath pattern: "classpath*:org/example/beans/*.class"

**HST Root:**
- Auto-detected from Maven artifactId
- Pattern: `/hst:${artifactId}` from pom.xml
- Override: `hstRoot = "/hst:customname"`

**Spring Configs:**
- JAX-RS: Searches for `custom-jaxrs.xml`, `annotation-jaxrs.xml`, `rest-resources.xml`, `jaxrs-config.xml`
- PageModel: Searches for `custom-pagemodel.xml`, `annotation-pagemodel.xml`, `custom-component.xml`, `component-config.xml`
- Location: In test class package path (converted to resource path)
- Override: `springConfigs = {"/path/to/config.xml"}`

### Error Handling Semantics

**Exception Types:**
- `BrutTestConfigurationException` - Configuration errors (unified in brut-common)

**Error Categories:**
1. **Missing Annotation** - Annotation not present on test class
2. **Missing Field** - No field of required type found
3. **Bootstrap Failed** - Initialization error with full config context
4. **Resource Not Found** - File/resource missing from classpath
5. **Invalid State** - Internal framework error (should not occur)

**Error Message Structure:**
```
{Problem Description}

{Context Section - Configuration/State}

{Root Cause}

To fix:
  1. {Action Item 1}
  2. {Action Item 2}
  ...
```

### Configuration Resolution Algorithm

```
For each annotation parameter:
  IF explicitly specified:
    USE specified value
  ELSE IF auto-detection available:
    RUN auto-detection
    IF found:
      USE auto-detected value
    ELSE:
      USE default value (if exists)
  ELSE:
    THROW configuration error

Priority: Explicit > Auto-detected > Default > Error
```

### File Structure Requirements

**JAX-RS Test:**
```
src/test/java/org/example/MyApiTest.java
src/test/resources/org/example/custom-jaxrs.xml (optional)
```

**Page Model Test:**
```
src/test/java/org/example/MyPageModelTest.java
src/test/resources/org/example/custom-pagemodel.xml (optional)
```

**Component Test:**
```
src/test/java/org/example/MyComponentTest.java
src/test/resources/org/example/custom-component.xml (optional)
```

### Common Patterns

**Pattern 1: Minimal Test (Auto-detection)**
```java
@BrxmJaxrsTest(beanPackages = {"org.example.model"})
public class MinimalTest {
    private DynamicJaxrsTest brxm;

    @Test void test() {
        brxm.getHstRequest().setRequestURI("/api/test");
        assertEquals("OK", brxm.invokeFilter());
    }
}
```

**Pattern 2: Explicit Configuration**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    hstRoot = "/hst:myproject",
    springConfigs = {"/org/example/custom.xml"}
)
public class ExplicitTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

**Pattern 3: Production Config**
```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    useConfigService = true
)
public class ProductionConfigTest {
    private DynamicJaxrsTest brxm;

    @Test void test() { /* ... */ }
}
```

### Implementation Checklist

When generating BRUT tests, verify:

- [ ] Annotation present on class
- [ ] `beanPackages` parameter specified
- [ ] Field of correct Dynamic*Test type declared
- [ ] Field not initialized (handled by extension)
- [ ] Test methods use `brxm.*` to access test infrastructure
- [ ] No inheritance (no `extends` clause)
- [ ] No manual setup/teardown methods needed
- [ ] Imports include annotation and dynamic test class

### Package Structure

```
brut-common:
  org.bloomreach.forge.brut.common.exception
    - BrutTestConfigurationException (unified errors)
  org.bloomreach.forge.brut.common.logging
    - TestConfigurationLogger (logging utilities)
  org.bloomreach.forge.brut.common.junit
    - TestInstanceInjector (JUnit extension helper)

brut-resources:
  org.bloomreach.forge.brut.resources.annotation
    - BrxmPageModelTest (annotation)
    - BrxmJaxrsTest (annotation)
    - DynamicPageModelTest (test class)
    - DynamicJaxrsTest (test class)
  org.bloomreach.forge.brut.resources.bootstrap
    - ConfigServiceBootstrapStrategy (orchestrator)
    - RuntimeTypeStubber (namespace/nodetype stubbing)
    - JcrNodeSynchronizer (node sync operations)
    - ConfigServiceReflectionBridge (ConfigService reflection)

brut-components:
  org.bloomreach.forge.brut.components.annotation
    - BrxmComponentTest (annotation)
    - DynamicComponentTest (test class)
```

---

## Troubleshooting

**Issue:** NullPointerException on `brxm` field
**Fix:** Declare field: `private DynamicPageModelTest brxm;`

**Issue:** "Missing @BrxmPageModelTest annotation"
**Fix:** Add annotation to test class

**Issue:** "Bean packages: NONE" warning
**Fix:** Add `beanPackages = {"org.example.beans"}` to annotation

**Issue:** Spring config not found
**Fix:** Verify file exists at path, use absolute path starting with `/`

**Issue:** HST root not found
**Fix:** Override with correct path: `hstRoot = "/hst:actualname"`

---

## Further Documentation

- **ConfigService Details:** [config-service-repository.md](config-service-repository.md)
- **Architecture:** [architecture.md](architecture.md)
- **Release Notes:** [../release-notes.md](../release-notes.md)
- **Examples:** [../demo/site/components/src/test/java/org/example/](../demo/site/components/src/test/java/org/example/)
