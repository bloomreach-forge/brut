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

## Performance: Repository Sharing (brut-components)

`BrxmComponentTestExtension` shares a single `BrxmTestingRepository` across all test classes whose configuration fingerprint matches. The fingerprint is computed from:

```
annotatedClassesResourcePath | testResourcePath | content | contentRoot
```

On the first encounter of a fingerprint, a bootstrap `DynamicComponentTest` is created, `setup()` is called (registering node types and importing the skeleton YAML), and the resulting repository is stored in JUnit 5's global root store as a `CloseableResource`. Subsequent test classes with the same fingerprint receive the shared repository via `BaseComponentTest.setRepository()` and open their own JCR session from it.

The repository is shut down exactly once, at the end of the full test suite, when JUnit calls `SharedRepositoryEntry.close()`.

### `BrxmTestingRepository.recordInitialization(key)`

A first-caller-wins gate backed by a `Collections.synchronizedSet`. Returns `true` the first time a key is seen (the caller should perform the operation) and `false` thereafter (the caller should skip). Used by `BaseComponentTest` for both skeleton YAML import and base node-type registration, ensuring each runs at most once per shared repository regardless of how many test classes share it.

### Extension Hooks

| Hook | Default | `BaseComponentTest` override |
|------|---------|------------------------------|
| `shouldImportNodeStructure()` | `true` | Delegates to `recordInitialization(getPathToTestResource())` |
| `shouldRegisterBaseNodeTypes()` | `true` | Delegates to `recordInitialization("__baseNodeTypes__")` |

Custom subclasses can override these hooks to control whether a given operation runs.

## Performance: ConfigurationModel Caching (brut-resources)

`ConfigServiceBootstrapStrategy` caches the built `ConfigurationModel` keyed by a SHA-256 fingerprint of its source HCM module files. Test classes with identical HCM modules reuse the cached model, skipping the full ConfigService parse on every run.

## Parallel Execution Contract

### Class-level parallelism

Running multiple test classes concurrently (JUnit 5 class-level parallel mode) is supported:

- `getOrComputeIfAbsent()` in the root store serializes repository bootstrapping — only one thread creates the repository per fingerprint.
- Each class gets its own JCR session, `DynamicComponentTest`, and mock objects.
- Jackrabbit supports multiple concurrent sessions on a single repository instance.

**Caveat:** `appliedInitKeys` uses `Collections.synchronizedSet` — the `add()` is atomic but callers should not assume the associated operation (e.g. node-type registration) has completed on the thread that received `false`. In practice this is safe because registration happens during `bootstrapSharedRepository()` which completes before any test class can call `setup()` on the shared repo.

### Method-level parallelism

`@Execution(CONCURRENT)` on test methods within the same class is **not supported**. All methods share one `DynamicComponentTest` instance, one JCR session, and non-thread-safe mock objects. Enabling method-level concurrency will cause data corruption and intermittent failures.

## Customization Points
- Provide a custom `repository.xml` on the classpath for repository behavior and auth.
- Control module loading via explicit module descriptors in tests.
- Add CND/YAML patterns via Spring configuration.
- Override `shouldImportNodeStructure()` or `shouldRegisterBaseNodeTypes()` in a subclass of `BaseComponentTest` to control per-repository initialization.
