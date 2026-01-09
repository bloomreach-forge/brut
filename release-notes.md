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
| 16.6.5   | 5.0.1   |
| 16.0.0   | 5.0.0   |
| 15.0.1   | 4.0.1   |
| 15.0.0   | 4.0.0   |
| 14.0.0-2 | 3.0.0   |
| 13.4     | 2.1.2   |
| 13.1     | 2.0.0   |
| 12.x     | 1.x     |

## Release Notes

### 5.0.1

**Multi-Test Support and Stability Improvements**

This release focuses on enabling reliable testing with multiple test methods and improving overall framework stability.

**Key Improvements:**

* **JUnit 4 `@Before` pattern support** - Component manager now properly shared across test instances while maintaining per-test isolation
* **Thread-safe initialization** - ReentrantLock-based synchronization prevents race conditions in parallel test execution
* **RequestContextProvider support** - JAX-RS resources can now access `RequestContextProvider.get()` with proper ThreadLocal management
* **Null-safety and error handling** - Defensive checks throughout with clear error messages for initialization issues
* **Exception visibility** - Full stack traces logged and propagated for easier debugging

**Usage:**

Both JUnit 4 and JUnit 5 patterns are supported:

```java
// JUnit 5 (Recommended)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MyTest extends AbstractJaxrsTest {
    @BeforeAll public void init() { super.init(); }
    @BeforeEach public void beforeEach() { setupForNewRequest(); }
}

// JUnit 4 (Fully supported)
public class MyTest extends AbstractJaxrsTest {
    @Before public void setUp() { super.init(); /* custom setup */ }
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