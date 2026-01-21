# Stubbing Test Data for Component Tests

## Overview

BRUT supports two approaches for test data:
1. **Stubbed YAML content** - Minimal, focused test data you control
2. **Production HCM modules** - Real project configuration via `loadProjectContent = true`

For component unit tests, **stubbed data is strongly preferred**.

## Why Stub Data?

### Test Isolation
Stubbed data makes tests self-contained. Each test controls exactly what data exists, eliminating dependencies on external content state.

```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/news-test-data.yaml",  // Test owns its data
    contentRoot = "/content/documents/site"
)
```

### Reproducibility
Tests with stubbed data produce the same results regardless of:
- What content exists in the CMS
- Which environment runs the tests
- When the tests run

### Speed
Stubbed data loads only what the test needs. Production content can include thousands of documents, channels, and configuration nodes that slow down test initialization.

### Documentation
Test YAML files document the expected data structure. New developers can read the YAML to understand what content shape the component expects.

### Focused Testing
Stubbed data lets you create specific scenarios:
- Edge cases (empty fields, missing references)
- Boundary conditions (pagination limits, date ranges)
- Error conditions (malformed data)

## When to Use Each Approach

| Approach | Use Case |
|----------|----------|
| Stubbed YAML | Component unit tests, bean property access, business logic |
| `loadProjectContent = true` | HST configuration validation, sitemap/mount testing, integration tests |

## Creating Stubbed YAML Content

### Basic Structure

```yaml
# src/test/resources/org/example/news-test-data.yaml
definitions:
  content:
    /news:
      jcr:primaryType: hippostd:folder
      /article-1:
        jcr:primaryType: hippo:handle
        /article-1:
          jcr:primaryType: ns:Article
          ns:title: Test Article
          ns:date: 2024-01-15T10:00:00.000Z
          ns:author: John Doe
```

### Key Points

1. **Match your `@Node` beans** - The `jcr:primaryType` must match the `jcrType` in your bean's `@Node` annotation
2. **Use hippo:handle wrapper** - Documents need the handle/document structure for HST queries to work
3. **Include required properties** - Add properties your component accesses to avoid NPEs
4. **Keep it minimal** - Only include data the test actually uses

### Example: Testing a List Component

```yaml
# Minimal data for testing pagination
definitions:
  content:
    /articles:
      jcr:primaryType: hippostd:folder
      /article-1:
        jcr:primaryType: hippo:handle
        /article-1:
          jcr:primaryType: ns:Article
          ns:title: First Article
          ns:date: 2024-01-01T00:00:00.000Z
      /article-2:
        jcr:primaryType: hippo:handle
        /article-2:
          jcr:primaryType: ns:Article
          ns:title: Second Article
          ns:date: 2024-01-02T00:00:00.000Z
      /article-3:
        jcr:primaryType: hippo:handle
        /article-3:
          jcr:primaryType: ns:Article
          ns:title: Third Article
          ns:date: 2024-01-03T00:00:00.000Z
```

```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/org/example/articles-test-data.yaml",
    contentRoot = "/content/documents/site"
)
class ArticleListComponentTest {
    private DynamicComponentTest brxm;

    @Test
    void pagination_withPageSize2_returnsFirstTwoArticles() {
        // Configure component for page size 2
        ArticleListInfo params = mock(ArticleListInfo.class);
        when(params.getPageSize()).thenReturn(2);
        brxm.setComponentParameters(params);

        // Execute
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        // Verify
        Pageable<Article> result = brxm.getRequestAttributeValue("pageable");
        assertEquals(2, result.getItems().size());
        assertEquals(3, result.getTotal());
    }
}
```

## File Organization

Keep test YAML files close to the tests that use them:

```
src/test/
├── java/org/example/
│   ├── ArticleListComponentTest.java
│   └── NewsDetailComponentTest.java
└── resources/org/example/
    ├── articles-test-data.yaml
    └── news-detail-test-data.yaml
```

## When Production Content is Appropriate

Use `loadProjectContent = true` for:

- **HST Configuration Tests** - Validating sitemap items, mounts, channels
- **Integration Tests** - Testing the full stack with real configuration
- **Smoke Tests** - Verifying production config loads without errors

```java
@BrxmPageModelTest(
    beanPackages = {"org.example.beans"},
    loadProjectContent = true  // Loads real HCM modules
)
class SitemapIntegrationTest {
    // Tests that validate HST configuration structure
}
```

## Summary

| Stubbed Data | Production Content |
|--------------|-------------------|
| Fast | Slower (more data) |
| Isolated | Depends on project state |
| Reproducible | May vary by environment |
| Documents expectations | Tests real config |
| Unit tests | Integration tests |
