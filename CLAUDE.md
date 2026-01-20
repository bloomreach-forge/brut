# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

B.R.U.T. (Bloomreach Unit Testing Library) is a Maven-based multi-module framework for testing Bloomreach XM (formerly Hippo CMS) components and REST services. It provides:

- **brut-common**: In-memory JCR repository for bootstrapping test content
- **brut-components**: HST component testing framework with mock infrastructure
- **brut-resources**: JAX-RS REST and Page Model API testing framework

Java 17, Maven 3.6+. Current version: 5.1.0-SNAPSHOT (parent pom.xml).

## Common Commands

### Build and Clean
```bash
# Build entire project
mvn clean package

# Build without running tests
mvn clean install -DskipTests

# Build specific module
mvn -pl brut-common clean package
mvn -pl brut-components clean package
mvn -pl brut-resources clean package
```

### Testing
```bash
# Run all tests
mvn clean test

# Run tests in specific module
mvn -pl brut-components test
mvn -pl brut-resources test

# Run single test class
mvn -pl brut-resources test -Dtest=JaxrsTest

# Run single test method
mvn -pl brut-resources test -Dtest=JaxrsTest#testUserEndpoint

# Run tests with debug output
mvn test -X
```

### Project Documentation
```bash
# Generate Javadoc
mvn javadoc:javadoc

# Generate site documentation
mvn site

# Generate site for GitHub Pages (to /docs/)
mvn -Pgithub.pages site
```

## Code Architecture

### Module Structure

**brut-common** - Core repository and utilities
- `BrxmTestingRepository`: Creates temporary in-memory JCR repositories using Jackrabbit
- `utils/ImporterUtils`: YAML and CND (node type) import for content bootstrapping
- `utils/NodeTypeUtils`: Dynamic JCR node type registration
- `utils/ReflectionUtils`: Framework introspection utilities

**brut-components** - HST component testing (Component classes in HST pipeline)
- `BaseComponentTest`: Full setup with JCR repository + HST component infrastructure
- `AbstractRepoTest`: JCR repository integration with object-bean mapping
- `SimpleComponentTest`: Minimal mock HST request/response/component manager setup
- `mock/DelegatingComponentManager`: ThreadLocal-based per-test component isolation

**brut-resources** - JAX-RS REST and Page Model API testing (JAX-RS resources and Page Model endpoints)
- `AbstractJaxrsTest`: Optimized for REST endpoint testing with shared component manager across test methods
- `AbstractPageModelTest`: Specialized for Page Model API with automatic addon module loading and JSON assertions
- `AbstractResourceTest`: Base class managing HST platform services, servlet context, and filter invocation
- Uses ReentrantLock for thread-safe multi-test initialization and WebappContextRegistry for HST model management

### Test Class Hierarchy and Usage

```
SimpleComponentTest (base mock setup)
├── AbstractRepoTest (adds JCR + content bean mapping)
│   └── BaseComponentTest (adds BrxmTestingRepository lifecycle)
│       └── Used for: Component testing with repository content

AbstractResourceTest (manages HST container + platform)
├── AbstractJaxrsTest (REST endpoint testing)
│   └── Used for: JAX-RS resource testing with request/response
└── AbstractPageModelTest (Page Model API testing)
    └── Used for: Page Model component testing with JSON responses
```

## Simplified Test Configuration (v5.1.0+)

## HTTP Session Support (v5.1.0+)

Mock HTTP sessions enable testing of login flows and session-dependent features.

**Example:**
```java
@Test
void testLoginFlow() {
    // Access or create session
    HttpSession session = getHstRequest().getSession();

    // Store user data
    session.setAttribute("userId", "user123");
    session.setAttribute("userName", "John Doe");

    // Session persists across requests in same test
    getHstRequest().setRequestURI("/site/api/profile");
    String response = invokeFilter();

    // Retrieve from session
    assertEquals("user123", getHstRequest().getSession().getAttribute("userId"));
}
```

**Session Methods:**
- `getHstRequest().getSession()` - Get/create session
- `getHstRequest().getSession(false)` - Get session or null
- `getHstRequest().setSession(session)` - Set custom session
- `session.setAttribute(name, value)` - Store data
- `session.getAttribute(name)` - Retrieve data
- `session.invalidate()` - Clear session

### Key Setup Patterns

**Component Testing Pattern** (`BaseComponentTest`):
1. Call `super.setup()` in `@BeforeEach` - initializes repository and mock HST infrastructure
2. Register custom node types via `registerNodeType()`
3. Import YAML test content via `ImporterUtils.importYaml()`
4. Call `recalculateHippoPaths()` to enable HST queries
5. Set site content base via `setSiteContentBase()`
6. Call `super.teardown()` in `@AfterEach`

**JAX-RS Testing Pattern** (`AbstractJaxrsTest` with JUnit 5):
1. Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` for shared component manager
2. Call `super.init()` once in `@BeforeAll` - initializes shared HST platform
3. Call `setupForNewRequest()` in `@BeforeEach` - resets HST model per test
4. Override abstract methods to provide Spring config and HST bean classes
5. Call `super.destroy()` in `@AfterAll`

**Page Model Testing Pattern** (`AbstractPageModelTest`):
- Extends `AbstractJaxrsTest` with automatic pagemodel addon loading
- Use `importComponent()` to create sitemap + page definition from test content
- Use `testComponent()` to invoke and assert JSON responses

### Configuration Files

**brut-resources Spring XMLs** (`src/main/resources/org/bloomreach/forge/brut/resources/hst/`):
- `container.xml`, `platform.xml`, `pipelines.xml`, `filter.xml` - HST infrastructure
- `jcr.xml`, `content-beans.xml`, `linking.xml` - JCR and linking services
- `channel-manager.xml`, `decorators.xml`, `hst-manager.xml` - Channel and response management
- `pagemodel-addon/module.xml` - Page Model API addon configuration

**brut-common** (`src/main/resources/`):
- `repository.xml` - Jackrabbit repository configuration
- `indexing_configuration.xml` - JCR full-text indexing setup

### Content Import Workflow

YAML files define JCR content structure:
```yaml
/root:
  - my:DocumentType:
      my:property: value
      my:nestedNode:
        - my:NestedType:
            my:nestedProp: nestedValue
```

Usage in tests:
```java
URL resource = getClass().getResource("/test-content.yaml");
ImporterUtils.importYaml(resource, rootNode, "/content/documents/mychannel", "hippostd:folder");
recalculateHippoPaths();  // Required for HST queries to work
```

### ThreadLocal and Shared Instance Patterns

- **DelegatingComponentManager**: Provides per-thread component manager isolation for concurrent tests
- **RequestContextProvider**: HST request context stored in ThreadLocal by AbstractResourceTest
- **AbstractJaxrsTest component manager reuse**: Uses `AtomicBoolean` + `ReentrantLock` for first-initialization, then reuses across test methods (performance optimization)
- **WebappContextRegistry**: Thread-safe management of HST webapp contexts for multiple test initialization

### Key Methods and Properties

**From BaseComponentTest/AbstractRepoTest:**
- `getHippoBean(path)` - Retrieve content bean from repository by path
- `setContentBean(bean)` - Set content bean in request context
- `setSiteContentBase(path)` - Configure site root for bean queries
- `recalculateHippoPaths()` - Recalculate Hippo path properties for query support (required after importing YAML)
- `registerNodeType(types...)` - Dynamically register custom node types
- `getComponentManager()` - Access mock component manager

**From AbstractJaxrsTest/AbstractPageModelTest:**
- `getHstRequest()` / `getHstResponse()` - Access mock request/response
- `setupForNewRequest()` - Reset HST model for new test (call in `@BeforeEach`)
- `invokeFilter()` - Execute HST filter pipeline and return response
- `invalidateHstModel()` - Force HST to reload configuration (for testing model updates)
- `testComponent(content, expected)` - Import component and assert JSON matches

**Abstract Methods to Override:**
- `getAnnotatedHstBeansClasses()` - Classpath patterns for HST bean discovery
- `contributeSpringConfigurationLocations()` - Custom Spring XML configuration files
- `contributeHstConfigurationRootPath()` - HST configuration root (e.g., "/hst:myproject")
- `getAnnotatedClassesResourcePath()` - (Components) Classpath patterns for content bean class discovery

## Release and Version Management

Current development version: **5.1.0-SNAPSHOT**

Version compatibility with brXM:
- brXM 16.6.5 → BRUT 5.0.1 (latest stable)
- brXM 16.0.0 → BRUT 5.0.0
- brXM 15.0.1 → BRUT 4.0.1

Artifact coordinates:
```xml
<groupId>org.bloomreach.forge.brut</groupId>
<artifactId>brut-components</artifactId>  <!-- or brut-resources -->
<version>${brut.version}</version>
<scope>test</scope>
```

## Demo Examples

### Traditional Base Class Approach

Reference tests in `demo/site/components/src/test/java/org/example/`:
- **EssentialsListComponentTest** (BaseComponentTest): Component testing with content querying
- **JaxrsTest** (AbstractJaxrsTest): REST endpoint testing with multiple test methods
- **PageModelTest** (AbstractPageModelTest): Page Model API testing with JSON assertions

### Simplified Patterns (v5.1.0+)

Examples showing minimal setup with HTTP session support:
- **AnnotatedComponentTest**: Demonstrates base class usage pattern with reduced boilerplate
- **AnnotatedJaxrsTest**: REST testing with HTTP session attribute examples
- **AnnotatedPageModelTest**: Page Model testing with session support
