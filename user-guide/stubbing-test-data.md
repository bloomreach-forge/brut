# Stubbing Test Data for Component Tests

<!-- AI-METADATA
test-types: [component, pagemodel]
patterns: [yaml, content, stub, fixture]
keywords: [yaml, content, stub, fixture, test-data, handle, document, folder]
difficulty: intermediate
-->

## Overview

BRUT supports two approaches for test data:
1. **Stubbed YAML content** - Minimal, focused test data you control
2. **Production HCM modules** - Real project configuration via `loadProjectContent = true`

For component unit tests, **stubbed data is strongly preferred**.

## The `content` and `contentRoot` Parameters

These two `@BrxmComponentTest` parameters work together to load test data:

| Parameter | Purpose | Example |
|-----------|---------|---------|
| `content` | **Source**: Classpath path to YAML file | `/test-content.yaml` |
| `contentRoot` | **Target**: JCR path where content is imported | `/content/documents/mysite` |

```java
@BrxmComponentTest(
    beanPackages = {"org.example.beans"},
    content = "/articles-test-data.yaml",        // Load from classpath
    contentRoot = "/content/documents/mysite"    // Import to this JCR path
)
```

**What happens:**
1. BRUT reads `content` from classpath (e.g., `src/test/resources/articles-test-data.yaml`)
2. YAML is imported at `contentRoot` path in the mock repository
3. `contentRoot` also becomes the `siteContentBasePath` for HST queries

**Path Resolution:**
- `/articles-test-data.yaml` → `src/test/resources/articles-test-data.yaml`
- `/org/example/test-data.yaml` → `src/test/resources/org/example/test-data.yaml`

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

## Complete YAML Structure Example

A real-world test content YAML showing multiple content types, folders, and compound types:

```yaml
# src/test/resources/petbase-test-content.yaml
/petbase-telecom:
  jcr:primaryType: hippostd:folder
  jcr:mixinTypes: ['hippo:harddocument', 'hippotranslation:translated']
  jcr:uuid: a1b2c3d4-e5f6-7890-abcd-ef1234567890
  hippostd:foldertype: [new-document, new-folder]
  hippotranslation:id: petbase-test-id
  hippotranslation:locale: en

  /herobanners:
    jcr:primaryType: hippostd:folder
    jcr:mixinTypes: ['mix:referenceable']
    jcr:uuid: b2c3d4e5-f6a7-8901-bcde-f12345678901

    /test-hero:
      jcr:primaryType: hippo:handle
      jcr:mixinTypes: ['mix:referenceable']
      jcr:uuid: c3d4e5f6-a7b8-9012-cdef-123456789012

      /test-hero:
        jcr:primaryType: petbase:HeroBanner
        jcr:mixinTypes: ['mix:referenceable']
        jcr:uuid: d4e5f6a7-b8c9-0123-def0-123456789013
        hippo:availability: [live]
        hippostd:state: published
        petbase:title: Welcome to PetBase
        petbase:description: Hello ${USER}, welcome to our pet care platform!

        # Compound type (nested content)
        /petbase:callToAction:
          jcr:primaryType: petbase:CallToAction
          petbase:title: Get Started
          petbase:externalLink: /plans

  /pricingcards:
    jcr:primaryType: hippostd:folder
    jcr:mixinTypes: ['mix:referenceable']
    jcr:uuid: d6e7f8a9-b0c1-2345-9012-345678901234

    /free-plan:
      jcr:primaryType: hippo:handle
      jcr:mixinTypes: ['mix:referenceable']
      jcr:uuid: e7f8a9b0-c1d2-3456-0123-456789012345

      /free-plan:
        jcr:primaryType: petbase:PricingCard
        jcr:mixinTypes: ['mix:referenceable']
        jcr:uuid: f8a9b0c1-d2e3-4567-1234-567890123456
        hippo:availability: [live]
        hippostd:state: published
        petbase:title: Free Plan
        petbase:planName: Free
        petbase:price: "0"
        petbase:interval: month
        petbase:description: Basic features for pet owners
        petbase:highlighted: false
        petbase:features: ['Basic tracking', 'Community access']

        /petbase:callToAction:
          jcr:primaryType: petbase:CallToAction
          petbase:title: Get Started Free
          petbase:externalLink: /signup/free
```

## Required Properties Checklist

For content to be queryable and accessible, ensure these properties:

| Property | Required? | Purpose |
|----------|-----------|---------|
| `jcr:primaryType` | **Yes** | Must match `@Node(jcrType)` annotation |
| `jcr:mixinTypes: ['mix:referenceable']` | Recommended | Enables UUID-based references |
| `jcr:uuid` | Recommended | Unique identifier for the node |
| `hippo:availability: [live]` | **Yes** | Makes document visible to HST queries |
| `hippostd:state: published` | **Yes** | Marks document as published |
| Custom properties (`ns:title`, etc.) | As needed | Data your component accesses |

## Compound Types (Nested Content)

For reusable embedded content structures like CTAs, images, or addresses:

```yaml
# Parent document
/my-document:
  jcr:primaryType: hippo:handle
  /my-document:
    jcr:primaryType: myproject:Page
    hippo:availability: [live]
    hippostd:state: published
    myproject:title: My Page

    # Compound type - child node with its own type
    /myproject:callToAction:
      jcr:primaryType: myproject:CallToAction
      myproject:title: Learn More
      myproject:link: /about

    # Multiple compounds of same type
    /myproject:images:
      jcr:primaryType: myproject:ImageSet
      myproject:alt: Hero image
      # ... image properties

    /myproject:images[2]:
      jcr:primaryType: myproject:ImageSet
      myproject:alt: Thumbnail
```

## Handle/Document Structure

brXM documents follow a handle/variant pattern:

```
/folder                          <- hippostd:folder
  /document-name                 <- hippo:handle (container)
    /document-name               <- actual document (same name as handle)
      properties...
```

**Why this structure?**
- The handle allows multiple variants (draft, live, preview)
- HST queries find handles, then resolve to the correct variant
- `hippo:availability: [live]` determines which variant is "live"

```yaml
# Correct structure
/articles:
  jcr:primaryType: hippostd:folder
  /my-article:                          # Handle
    jcr:primaryType: hippo:handle
    /my-article:                        # Document (same name!)
      jcr:primaryType: myproject:Article
      hippo:availability: [live]
      # ...properties
```

## Multi-Value Properties

For arrays/lists in YAML:

```yaml
/my-document:
  jcr:primaryType: myproject:PricingCard
  # Single value
  myproject:title: Premium Plan

  # Multi-value property (array)
  myproject:features: ['Feature 1', 'Feature 2', 'Feature 3']

  # Multi-value with complex values
  jcr:mixinTypes: ['mix:referenceable', 'hippo:harddocument']
```

## Summary

| Stubbed Data | Production Content |
|--------------|-------------------|
| Fast | Slower (more data) |
| Isolated | Depends on project state |
| Reproducible | May vary by environment |
| Documents expectations | Tests real config |
| Unit tests | Integration tests |

## Related Guides

- [Getting Started](getting-started.md) - Initial BRUT setup
- [Common Patterns](common-patterns.md) - Content import patterns
- [Troubleshooting](troubleshooting.md) - YAML import issues
