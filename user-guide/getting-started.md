# Getting Started with BRUT

<!-- AI-METADATA
test-types: [component, pagemodel, jaxrs]
patterns: [setup, first-test, hello-world]
keywords: [installation, maven, dependencies, quickstart, beginner]
difficulty: beginner
-->

## Prerequisites

Before you begin, ensure you have:

- [ ] Java 17 or higher
- [ ] Maven 3.8 or higher
- [ ] brXM 16.x project
- [ ] JUnit 5 in your test dependencies

## Step 1: Add Dependencies

### In your parent pom.xml

Add the version property and dependency management:

```xml
<properties>
    <brut.version>5.1.0</brut.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-components</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-resources</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### In site/components/pom.xml

Add the dependencies (version inherited from parent):

```xml
<!-- BRUT Component Testing -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-components</artifactId>
    <scope>test</scope>
</dependency>

<!-- BRUT PageModel/JAX-RS Testing -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-resources</artifactId>
    <scope>test</scope>
</dependency>
```

> **Warning: `<scope>test</scope>` is required.** BRUT replaces core HST beans (pipelines, component manager, link creator, etc.) with test-oriented implementations. If BRUT is on the runtime classpath without test scope, its mock beans will shadow production beans and real HST endpoints will stop working. Always declare BRUT dependencies with `<scope>test</scope>`.

> **Note:** JUnit 5, Mockito, and AssertJ are typically managed by the brXM parent pom. Only add explicit versions if not already provided.

## Step 2: Write Your First Test

Create a component test in `src/test/java`:

```java
package com.example.components;

import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.bloomreach.forge.brut.components.annotation.DynamicComponentTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BrxmComponentTest  // Zero-config! Bean packages auto-detected from project-settings.xml
class MyFirstTest {

    @Test
    void infrastructure_testIsProperlyInitialized(DynamicComponentTest brxm) {
        // Parameter injection - no IDE warnings, standard JUnit 5 pattern
        assertThat(brxm.getHstRequest()).isNotNull();
        assertThat(brxm.getHstResponse()).isNotNull();
    }

    @Test
    void doBeforeRender_setsExpectedAttribute(DynamicComponentTest brxm) {
        MyComponent component = new MyComponent();
        component.init(null, brxm.getComponentConfiguration());
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        String result = brxm.getRequestAttributeValue("myAttribute");
        assertThat(result).isEqualTo("expected value");
    }
}
```

**Alternative: Field Injection** (use when you need the instance in `@BeforeEach`):

```java
@BrxmComponentTest
class MyFirstTest {

    @SuppressWarnings("unused")  // Injected by extension
    private DynamicComponentTest brxm;
    private MyComponent component;

    @BeforeEach
    void setUp() {
        component = new MyComponent();
        component.init(null, brxm.getComponentConfiguration());
    }

    @Test
    void doBeforeRender_setsExpectedAttribute() {
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());
        String result = brxm.getRequestAttributeValue("myAttribute");
        assertThat(result).isEqualTo("expected value");
    }
}
```

> **Note:** If auto-detection doesn't find your beans (e.g., no `project-settings.xml`), add explicit packages: `@BrxmComponentTest(beanPackages = {"com.example.beans"})`

## Step 3: Run Your Test

```bash
cd site/components
mvn test -Dtest=MyFirstTest
```

**Expected output:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.components.MyFirstTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

## What Just Happened?

1. **`@BrxmComponentTest`** - BRUT's JUnit 5 extension that:
   - Bootstraps an in-memory JCR repository
   - Initializes HST mock objects (request, response, component configuration)
   - Provides `DynamicComponentTest` via parameter or field injection

2. **`DynamicComponentTest brxm`** - Your test harness providing:
   - `getHstRequest()` / `getHstResponse()` - Mock HST request/response
   - `getComponentConfiguration()` - Mock component config for `init()`
   - `getRequestAttributeValue(name)` - Retrieve attributes set by your component

3. **Injection patterns** - Choose between:
   - **Parameter injection** (recommended) - `void test(DynamicComponentTest brxm)` - no IDE warnings
   - **Field injection** - `private DynamicComponentTest brxm;` - needed for `@BeforeEach` access

4. **`beanPackages`** - Tells BRUT where to scan for `@Node` annotated beans. This is **optional** if your project has a `project-settings.xml` file - BRUT will auto-detect packages from `selectedProjectPackage` and `selectedBeansPackage` settings

## Next Steps

| I want to... | Go to... |
|--------------|----------|
| Test PageModel API responses | [Quick Reference - PageModel Tests](quick-reference.md#pagemodel-tests) |
| Test JAX-RS REST endpoints | [JAX-RS Testing Guide](jaxrs-testing.md) |
| Add test content from YAML | [Stubbing Test Data](stubbing-test-data.md) |
| Test authenticated users | [Authentication Patterns](authentication-patterns.md) |
| Fix a failing test | [Troubleshooting Guide](troubleshooting.md) |
| Understand BRUT architecture | [Architecture](architecture.md) |

## Complete pom.xml Example

### Parent pom.xml (properties and dependency management)

```xml
<properties>
    <brut.version>5.1.0</brut.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- BRUT dependencies -->
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-components</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.bloomreach.forge.brut</groupId>
            <artifactId>brut-resources</artifactId>
            <version>${brut.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### site/components/pom.xml (dependencies)

```xml
<!-- BRUT Testing -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-components</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-resources</artifactId>
    <scope>test</scope>
</dependency>

<!-- These are typically managed by brXM parent pom -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>test</scope>
</dependency>

<!-- Optional: Include your repository-data-site for production HST config -->
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}-repository-data-site</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

> **Tip:** Avoid hardcoding versions in module pom.xml files. Use `${property}` references or rely on dependency management from the parent pom.
