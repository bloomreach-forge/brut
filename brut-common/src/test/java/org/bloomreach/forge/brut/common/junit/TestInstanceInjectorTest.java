package org.bloomreach.forge.brut.common.junit;

import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestInstanceInjectorTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestInstanceInjectorTest.class);

    interface TestInfrastructure {
        String getValue();
    }

    static class MockInfrastructure implements TestInfrastructure {
        @Override
        public String getValue() {
            return "injected";
        }
    }

    @Test
    @DisplayName("inject: injects instance into matching field type")
    void inject_injectsIntoMatchingField() throws Exception {
        class TestClass {
            TestInfrastructure infra;
        }
        TestClass testObject = new TestClass();
        ExtensionContext context = mockContextFor(testObject);
        MockInfrastructure instance = new MockInfrastructure();

        TestInstanceInjector.inject(context, instance, TestInfrastructure.class, LOG);

        assertNotNull(testObject.infra);
        assertEquals("injected", testObject.infra.getValue());
    }

    @Test
    @DisplayName("inject: throws exception when no matching field found")
    void inject_throwsWhenNoMatchingField() {
        class TestClass {
            String unrelatedField;
        }
        TestClass testObject = new TestClass();
        ExtensionContext context = mockContextFor(testObject);
        MockInfrastructure instance = new MockInfrastructure();

        BrutTestConfigurationException exception = assertThrows(
                BrutTestConfigurationException.class,
                () -> TestInstanceInjector.inject(context, instance, TestInfrastructure.class, LOG)
        );

        assertTrue(exception.getMessage().contains("TestInfrastructure"));
    }

    @Test
    @DisplayName("inject: injects into field of subtype")
    void inject_injectsIntoSubtypeField() throws Exception {
        class TestClass {
            MockInfrastructure concreteInfra;
        }
        TestClass testObject = new TestClass();
        ExtensionContext context = mockContextFor(testObject);
        MockInfrastructure instance = new MockInfrastructure();

        TestInstanceInjector.inject(context, instance, MockInfrastructure.class, LOG);

        assertNotNull(testObject.concreteInfra);
        assertEquals("injected", testObject.concreteInfra.getValue());
    }

    @Nested
    @DisplayName("Nested class field injection")
    class NestedClassInjection {

        @Test
        @DisplayName("inject: finds field declared in enclosing class for nested test instance")
        void inject_findsFieldInEnclosingClass() throws Exception {
            OuterTestClass outer = new OuterTestClass();
            OuterTestClass.NestedTestClass nested = outer.new NestedTestClass();
            ExtensionContext context = mockContextFor(nested);
            MockInfrastructure instance = new MockInfrastructure();

            TestInstanceInjector.inject(context, instance, TestInfrastructure.class, LOG);

            assertNotNull(outer.infra, "Field in enclosing class should be injected");
            assertEquals("injected", outer.infra.getValue());
        }

        @Test
        @DisplayName("inject: prefers field in nested class over enclosing class")
        void inject_prefersNestedClassField() throws Exception {
            OuterWithBothFields outer = new OuterWithBothFields();
            OuterWithBothFields.NestedWithField nested = outer.new NestedWithField();
            ExtensionContext context = mockContextFor(nested);
            MockInfrastructure instance = new MockInfrastructure();

            TestInstanceInjector.inject(context, instance, TestInfrastructure.class, LOG);

            assertNotNull(nested.nestedInfra, "Field in nested class should be injected");
            assertNull(outer.outerInfra, "Field in outer class should not be injected");
        }

        @Test
        @DisplayName("inject: traverses multiple nesting levels")
        void inject_traversesMultipleNestingLevels() throws Exception {
            OuterTestClass outer = new OuterTestClass();
            OuterTestClass.NestedTestClass nested = outer.new NestedTestClass();
            OuterTestClass.NestedTestClass.DeeplyNestedTestClass deeplyNested = nested.new DeeplyNestedTestClass();
            ExtensionContext context = mockContextFor(deeplyNested);
            MockInfrastructure instance = new MockInfrastructure();

            TestInstanceInjector.inject(context, instance, TestInfrastructure.class, LOG);

            assertNotNull(outer.infra, "Field in outermost class should be injected");
            assertEquals("injected", outer.infra.getValue());
        }
    }

    static class OuterTestClass {
        TestInfrastructure infra;

        @Nested
        class NestedTestClass {
            @Nested
            class DeeplyNestedTestClass {
            }
        }
    }

    static class OuterWithBothFields {
        TestInfrastructure outerInfra;

        @Nested
        class NestedWithField {
            TestInfrastructure nestedInfra;
        }
    }

    private ExtensionContext mockContextFor(Object testObject) {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getRequiredTestInstance()).thenReturn(testObject);
        return context;
    }
}
