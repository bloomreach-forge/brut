# BRUT Architecture

## Overview
BRUT boots an in-memory brXM delivery tier stack for tests. It builds a repository, loads HCM configuration and content,
initializes the HST model, then executes the same pipelines used in production (component rendering, JAX-RS, Page Model API).

## Modules
### brut-common
- In-memory repository and YAML import utilities.
- Repository configuration comes from `repository.xml` on the classpath (you can override it).
- Test authentication is provided by an in-memory login module.
- **Shared utilities:**
  - `BrutTestConfigurationException` - Unified error handling with factory methods
  - `TestConfigurationLogger` - Consistent configuration logging
  - `TestInstanceInjector` - JUnit 5 field injection
  - `AbstractBrutRepository` - Shared CND/namespace handling

### brut-resources
- Loads HCM modules (config + content) via Config Service APIs.
- Initializes the HST container and request pipelines.
- **Bootstrap components:**
  - `ConfigServiceBootstrapStrategy` - Orchestrates HCM-based bootstrap
  - `RuntimeTypeStubber` - Stubs missing namespaces/node types
  - `JcrNodeSynchronizer` - Syncs JCR nodes between trees
  - `ConfigServiceReflectionBridge` - Reflection access to ConfigService
- **Request utilities:**
  - `MockHstRequest` - Extended mock with HTTP session support (`getSession()`, `setSession()`, `invalidateSession()`)
  - `RequestBuilder` - Fluent API for request setup (`.get()`, `.post()`, `.queryParam()`, `.execute()`)
  - `PageModelResponse` - Utility for navigating Page Model API responses

### brut-components
- Component-test utilities and mocks for HST components.

## Bootstrap Flow
1. Register CND node types from configured patterns.
2. Load HCM modules from descriptors or explicit module paths.
3. Build the configuration model and apply namespaces/node types.
4. Import content definitions into the repository, preserving absolute JCR paths.
5. Build the HST model from the repository.

## Request Flow (Page Model API)
Below is the high-level sequence when a Page Model API request is executed in tests.

```
Test -> DynamicPageModelTest -> HST Container -> HST Model/Cache
  |           |                    |                |
  |           |                    |                +--> Resolve sitemap item
  |           |                    |                +--> Resolve channel (preview/live)
  |           |                    |                +--> Resolve component tree
  |           |                    |                         |
  |           |                    |                         +--> Resolve container refs
  |           |                    |                              (hst:workspace/hst:containers)
  |           |                    |                         +--> Build container items
  |           |                    |
  |           |                    +--> Build Page Model JSON
  |           |
  +--> Response JSON (root + page map with $ref links)
```

## Request Flow (Component Rendering and JAX-RS)
Component rendering and JAX-RS tests share the same HST container and model, but use different pipelines.

```
Test -> DynamicComponentTest/DynamicJaxrsTest -> HST Container -> HST Model/Cache
  |                |                               |                |
  |                |                               |                +--> Resolve mount + sitemap item
  |                |                               |                +--> Resolve component tree
  |                |                               |                +--> Build request context
  |                |                               |
  |                |                               +--> Execute pipeline (component/jaxrs)
  |                |
  +--> Response (rendered output or JAX-RS payload)
```

## HST Configuration Resolution
### Composite Configuration
HST loads a composite configuration: base config overlaid by workspace nodes. This is how workspace containers,
sitemenus, and channel info become visible to the model.

### Container References
- `hst:containercomponentreference` resolves against `hst:workspace/hst:containers`.
- Container items are `hst:containeritemcomponent` children of `hst:containercomponent` nodes.
- If the referenced workspace path is missing, the reference is ignored and no items render.

### Sitemenus and Channel Info
- Sitemenus can live under both `hst:sitemenus` and `hst:workspace/hst:sitemenus`.
- Channel info is resolved from the mount; preview mounts use the preview configuration and its workspace overlays.

## Page Model API
- A sitemap item must resolve for menus to build.
- The Page Model API walks the HST component tree to build component windows and child references.
- Missing container/component references result in missing child windows in the response.

## Caching and Refresh
HST model caches are built during initialization. If configuration/content is imported after the model is built, caches
must be invalidated or the model rebuilt for changes to take effect.

## Customization Points
- Provide a custom `repository.xml` on the classpath for repository behavior and auth.
- Control module loading via explicit module descriptors in tests.
- Add CND/YAML patterns via Spring configuration.
