package com.bloomreach.ps.brxm.unittester.mock;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.mock.core.container.MockContainerConfiguration;

public class MockComponentManager implements ComponentManager {
    private Map<String, Object> components = new HashMap<>();
    private ServletConfig servletConfig;
    private ServletContext servletContext;
    protected MockContainerConfiguration containerConfiguration = new MockContainerConfiguration();

    public MockComponentManager() {
    }

    public MockContainerConfiguration getContainerConfiguration() {
        return containerConfiguration;
    }

    public void setConfigurationResources(String[] configurationResources) {
    }

    public String[] getConfigurationResources() {
        return new String[0];
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        if (servletConfig != null) {
            this.servletContext = servletConfig.getServletContext();
        }

    }

    /**
     * @deprecated
     */
    @Deprecated
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    public void initialize() {
    }

    public void start() {
    }

    public <T> T getComponent(String name) {
        return (T) this.components.get(name);
    }

    public <T> T getComponent(Class<T> requiredType) throws ComponentsException {
        Map<String, T> componentsOfType = getComponentsOfType(requiredType);
        if (componentsOfType.isEmpty()) {
            return null;
        } else if (componentsOfType.size() != 1) {
            throw new ComponentsException("Multiple components found by the specified type, " + requiredType);
        } else {
            // return the only item in the map
            return (T) componentsOfType.get(componentsOfType.keySet().iterator().next());
        }
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        return components.entrySet().stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> requiredType.isAssignableFrom(it.getValue().getClass()))
                .collect(Collectors.toMap(Entry::getKey, e -> (T) e.getValue()));
    }


    public <T> T getComponent(String name, String... addonModuleNames) {
        return this.getComponent(name);
    }

    public <T> T getComponent(Class<T> requiredType, String... addonModuleNames) {
        return this.getComponent(requiredType);
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String... addonModuleNames) {
        return getComponentsOfType(requiredType);
    }

    public void publishEvent(EventObject event) {
    }

    public void registerEventSubscriber(Object subscriber) {
    }

    public void unregisterEventSubscriber(Object subscriber) {
    }

    public void stop() {
    }

    public void close() {
    }

    /**
     * Add a component to the components Map
     *
     * @param name      the name
     * @param component the component
     * @param <T>       can be any type
     */
    public <T> void addComponent(String name, T component) {
        this.components.put(name, component);
    }
}
