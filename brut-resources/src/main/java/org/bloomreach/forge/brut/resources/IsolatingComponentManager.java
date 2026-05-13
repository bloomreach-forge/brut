package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;

import jakarta.servlet.ServletContext;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ComponentManager} that routes all calls to a per-thread delegate.
 * <p>
 * A single instance of this class is registered with {@link org.hippoecm.hst.site.HstServices}
 * once at class load and never replaced. Each test class sets its own
 * {@link SpringComponentManager} into this delegate via {@link #set(ComponentManager)} during
 * bootstrap, and clears it via {@link #remove()} during teardown.
 * <p>
 * This eliminates the flakiness caused by concurrent test classes writing to the
 * {@code HstServices} global static, since that global always points to this stable delegate.
 */
public class IsolatingComponentManager implements ComponentManager {

    private static final ThreadLocal<ComponentManager> DELEGATE = new ThreadLocal<>();

    public static void set(ComponentManager cm) {
        DELEGATE.set(cm);
    }

    public static void remove() {
        DELEGATE.remove();
    }

    public static ComponentManager getDelegate() {
        return DELEGATE.get();
    }

    private ComponentManager delegate() {
        return DELEGATE.get();
    }

    @Override
    public void setConfigurationResources(String[] configurationResources) {
        ComponentManager cm = delegate();
        if (cm != null) cm.setConfigurationResources(configurationResources);
    }

    @Override
    public String[] getConfigurationResources() {
        ComponentManager cm = delegate();
        return cm != null ? cm.getConfigurationResources() : null;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        ComponentManager cm = delegate();
        if (cm != null) cm.setServletContext(servletContext);
    }

    @Override
    public ServletContext getServletContext() {
        ComponentManager cm = delegate();
        return cm != null ? cm.getServletContext() : null;
    }

    @Override
    public void initialize() {
        ComponentManager cm = delegate();
        if (cm != null) cm.initialize();
    }

    @Override
    public void start() {
        ComponentManager cm = delegate();
        if (cm != null) cm.start();
    }

    @Override
    public <T> T getComponent(String name) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponent(name) : null;
    }

    @Override
    public <T> T getComponent(Class<T> requiredType) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponent(requiredType) : null;
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponentsOfType(requiredType) : new HashMap<>();
    }

    @Override
    public <T> T getComponent(String name, String... addonModuleNames) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponent(name, addonModuleNames) : null;
    }

    @Override
    public <T> T getComponent(Class<T> requiredType, String... addonModuleNames) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponent(requiredType, addonModuleNames) : null;
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String... addonModuleNames) {
        ComponentManager cm = delegate();
        return cm != null ? cm.getComponentsOfType(requiredType, addonModuleNames) : new HashMap<>();
    }

    @Override
    public void publishEvent(EventObject event) {
        ComponentManager cm = delegate();
        if (cm != null) cm.publishEvent(event);
    }

    @Override
    public void registerEventSubscriber(Object subscriber) {
        ComponentManager cm = delegate();
        if (cm != null) cm.registerEventSubscriber(subscriber);
    }

    @Override
    public void unregisterEventSubscriber(Object subscriber) {
        ComponentManager cm = delegate();
        if (cm != null) cm.unregisterEventSubscriber(subscriber);
    }

    @Override
    public void stop() {
        ComponentManager cm = delegate();
        if (cm != null) cm.stop();
    }

    @Override
    public void close() {
        ComponentManager cm = delegate();
        if (cm != null) cm.close();
    }

    @Override
    public ContainerConfiguration getContainerConfiguration() {
        ComponentManager cm = delegate();
        if (cm == null) {
            throw new IllegalStateException(
                "IsolatingComponentManager.DELEGATE is null on thread '" + Thread.currentThread().getName() +
                "' [id=" + Thread.currentThread().getId() + "]. " +
                "set() was not called on this thread, or was called on a different thread.");
        }
        ContainerConfiguration cc = cm.getContainerConfiguration();
        if (cc == null) {
            throw new IllegalStateException(
                "cm.getContainerConfiguration() returned null! cm=" + cm.getClass().getName());
        }
        return cc;
    }
}
