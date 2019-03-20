
package com.bloomreach.ps.brut.components.mock;

import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.mock.core.container.MockContainerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class MockComponentManagerTest {
    private final MockComponentManager componentManager = new MockComponentManager();

    @Test
    public void getContainerConfigurationTest() {
        MockContainerConfiguration containerConfiguration = componentManager.getContainerConfiguration();
        assertTrue(containerConfiguration instanceof MockContainerConfiguration);
    }

    @Test
    public void setServletConfigTestWithNull() {
        ServletContext context = mock(ServletContext.class);
        componentManager.setServletContext(context);
        assertEquals(context, componentManager.getServletContext());
    }

    @Test
    public void setServletConfig() {
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        doReturn(context).when(servletConfig).getServletContext();

        componentManager.setServletConfig(servletConfig);

        assertEquals(servletConfig, componentManager.getServletConfig());
        assertEquals(context, componentManager.getServletContext());
    }

    @Test
    public void getComponentsOfTypeTest() {
        String component1 = "component1";
        String component2 = "component2";
        componentManager.addComponent("test1", component1);
        componentManager.addComponent("test2", component2);

        Map<String, String> componentsOfType = componentManager.getComponentsOfType(String.class);
        assertEquals(2, componentsOfType.size());
    }

    @Test
    public void getComponentsOfTypeTestWithModuleName() {
        String component1 = "component1";
        String component2 = "component2";
        componentManager.addComponent("test1", component1);
        componentManager.addComponent("test2", component2);

        Map<String, String> componentsOfType = componentManager.getComponentsOfType(String.class, "foo");
        assertEquals(2, componentsOfType.size());
    }

    @Test
    public void getComponentTest() {
        String component1 = "component1";
        String component2 = "component2";
        componentManager.addComponent("test1", component1);
        componentManager.addComponent("test2", component2);

        String result1 = componentManager.getComponent("test1");
        String result2 = componentManager.getComponent("component1");
        assertEquals(component1, result1);
        assertNull(result2);
    }

    @Test
    public void getComponentTestWithModuleName() {
        String component1 = "component1";
        String component2 = "component2";
        componentManager.addComponent("test1", component1);
        componentManager.addComponent("test2", component2);

        String result1 = componentManager.getComponent("test1", "foo");
        String result2 = componentManager.getComponent("component1", "foo");
        assertEquals(component1, result1);
        assertNull(result2);
    }

    @Test
    public void getComponentByClassTest() {
        String component1 = "component1";
        componentManager.addComponent("test1", component1);

        String result1 = componentManager.getComponent(String.class);
        assertEquals(component1, result1);
        Double result2 = componentManager.getComponent(Double.class);
        assertNull(result2);
    }

    @Test
    public void getComponentByClassTestWithMultipleComponentsOfSameType() {
        String component1 = "component1";
        Integer component2 = 2;
        Integer component3 = 3;
        componentManager.addComponent("test1", component1);
        componentManager.addComponent("test2", component2);
        componentManager.addComponent("test3", component3);

        Assertions.assertThrows(ComponentsException.class, () -> componentManager.getComponent(Integer.class));
    }


    @Test
    public void getComponentByClassTestWithAddonModuleName() {
        String component1 = "component1";
        componentManager.addComponent("test1", component1);

        String result1 = componentManager.getComponent(String.class, "foo");
        assertEquals(component1, result1);
        Double result2 = componentManager.getComponent(Double.class, "foo");
        assertNull(result2);
    }
}
