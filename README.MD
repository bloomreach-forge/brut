# BRUT

[![CI](https://github.com/bloomreach-forge/brut/actions/workflows/ci.yml/badge.svg)](https://github.com/bloomreach-forge/brut/actions/workflows/ci.yml)

<!-- AI-METADATA
framework: brxm-testing
test-types: [component, pagemodel, jaxrs]
patterns: [annotation-based, field-injection, mock-repository, diagnostics]
keywords: [brxm, hst, unit-test, component, pagemodel, jaxrs, rest, bloomreach, v1, v1.0, diagnostics, PageModelAssert, resolveModelContent]
min-java: 17
min-brxm: 16.0
-->

Unit testing library for Bloomreach Experience Manager (brXM) delivery-tier components. BRUT spins up a full in-memory brXM stack — repository, HST container, and request pipelines — so tests run the same code paths as production without a running server.

## Compatibility

| BRUT  | brXM  | Java |
|-------|-------|------|
| 5.5.x | 16.7+ | 17+  |
| 5.4.x | 16.7+ | 17+  |
| 5.3.x | 16.7+ | 17+  |
| 5.2.x | 16.7+ | 17+  |
| 5.1.x | 16.x  | 17+  |
| 5.0.x | 16.x  | 17+  |
| 4.x   | 15.x  | 11+  |

## Installation

In your parent `pom.xml`, declare the version and add dependency management:

```xml
<properties>
    <brut.version>5.5.0</brut.version>
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

In `site/components/pom.xml`, add the dependencies you need:

```xml
<!-- HST component tests -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-components</artifactId>
    <scope>test</scope>
</dependency>

<!-- JAX-RS and Page Model API tests -->
<dependency>
    <groupId>org.bloomreach.forge.brut</groupId>
    <artifactId>brut-resources</artifactId>
    <scope>test</scope>
</dependency>
```

> Both dependencies must declare `<scope>test</scope>`. BRUT replaces core HST beans (component manager, pipelines, link creator) with test implementations; if they reach the runtime classpath they will shadow the real beans and break production endpoints.

## Usage

### HST component test

```java
@BrxmComponentTest
class HeroBannerTest {

    private DynamicComponentTest brxm;
    private HeroBanner component;

    @BeforeEach
    void setUp() {
        component = new HeroBanner();
        component.init(null, brxm.getComponentConfiguration());
    }

    @Test
    void doBeforeRender_withDocumentParameter_setsTitle() {
        HeroBannerInfo params = mock(HeroBannerInfo.class);
        when(params.getDocument()).thenReturn("herobanners/test-hero");
        brxm.setComponentParameters(params);

        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        HeroBannerModel model = brxm.getRequestAttributeValue("heroBanner");
        assertThat(model.getTitle()).isEqualTo("Welcome");
    }
}
```

Bean packages are auto-detected from `project-settings.xml`. Override when needed: `@BrxmComponentTest(beanPackages = {"com.example.beans"})`.

### JAX-RS endpoint test

```java
@BrxmJaxrsTest(resources = {NewsResource.class})
class NewsResourceTest {

    @Test
    void getArticles_returnsPagedList(DynamicJaxrsTest brxm) {
        String response = brxm.request()
                .get("/site/api/news")
                .queryParam("limit", "5")
                .execute();

        assertThat(response).contains("articles");
    }
}
```

### Page Model API test

```java
@BrxmPageModelTest(loadProjectContent = true)
class NewsPageTest {

    @Test
    void newsPage_resolvesArticleList(DynamicPageModelTest brxm) throws Exception {
        PageModelResponse model = brxm.request()
                .get("/site/resourceapi/news")
                .executeAsPageModel();

        PageComponent list = model.findComponentByName("NewsList").orElseThrow();
        List<ArticleData> articles = model.resolveModelContentList(list, "items", ArticleData.class);

        assertFalse(articles.isEmpty());
    }
}
```

`loadProjectContent = true` loads your project's actual HCM modules (sitemap, channels, content types) into the test repository. Omit it when stub data is sufficient.

## Modules

- **brut-common** — In-memory JCR repository and YAML content import utilities.
- **brut-components** — HST component test harness (`@BrxmComponentTest`, `DynamicComponentTest`).
- **brut-resources** — JAX-RS and Page Model API test harness (`@BrxmJaxrsTest`, `@BrxmPageModelTest`).

## Documentation

- [Getting Started](user-guide/getting-started.md)
- [JAX-RS Testing](user-guide/jaxrs-testing.md)
- [Stubbing Test Data](user-guide/stubbing-test-data.md)
- [Authentication Patterns](user-guide/authentication-patterns.md)
- [Common Patterns](user-guide/common-patterns.md)
- [Troubleshooting](user-guide/troubleshooting.md)
- [Architecture](user-guide/architecture.md)
- [Release Notes](release-notes.md)

## Releasing

Releases are managed through GitHub Actions. Use the [Release workflow](../../actions/workflows/release.yml) to cut a release, or run the [Test Release workflow](../../actions/workflows/test-release.yml) first for a dry run.
