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
| 16.0.0   | 5.0.0   |
| 15.0.1   | 4.0.1   |
| 15.0.0   | 4.0.0   |
| 14.0.0-2 | 3.0.0   |
| 13.4     | 2.1.2   |
| 13.1     | 2.0.0   |
| 12.x     | 1.x     |

## Release Notes

### 5.0.1

**Critical Bug Fixes for JAX-RS Testing:**

* **Fixed:** Exception stack traces were being swallowed in `AbstractResourceTest.invokeFilter()`. Now logs full exception details and properly propagates errors for better debugging.
* **Fixed:** `RequestContextProvider.get()` returned null in JAX-RS resources, causing NPE. ThreadLocal is now properly set and cleaned up using reflection to access private methods.
* **Fixed:** `IllegalStateException: There is already an HstModel registered` when running multiple test methods in the same test class. Now gracefully handles already-registered models.

**Thread Safety Improvements:**

* **Fixed:** Race condition in component manager initialization that could cause `BeanCreationException: A service of type HstSiteMapItemHandlerFactories is already registered`. Added synchronized blocks with proper locking mechanism.
* **Fixed:** Race condition in `HippoWebappContextRegistry` registration. Check-then-register operation is now atomic.
* **Fixed:** ThreadLocal memory leak risk. `RequestContextProvider` cleanup now guaranteed via finally block.

**API Changes:**

* Added `AbstractJaxrsTest.setupForNewRequest()` method - subclasses should call this in `@BeforeEach` when using `PER_CLASS` lifecycle to properly reset state between test methods.

**Migration Notes:**

For tests extending `AbstractJaxrsTest` with multiple test methods:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MyJaxrsTest extends AbstractJaxrsTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @BeforeEach  // Add this if you have multiple test methods
    public void beforeEach() {
        setupForNewRequest();  // Ensures clean state per test
    }

    @Test
    public void testOne() { /* ... */ }

    @Test
    public void testTwo() { /* ... */ }
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