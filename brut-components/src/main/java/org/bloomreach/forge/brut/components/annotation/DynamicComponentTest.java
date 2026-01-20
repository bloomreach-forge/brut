package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.components.BaseComponentTest;
import org.bloomreach.forge.brut.components.mock.MockComponentManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.request.MockComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.request.MockResolvedSiteMapItem;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class DynamicComponentTest extends BaseComponentTest {

    private final ComponentTestConfig config;

    public DynamicComponentTest(ComponentTestConfig config) {
        this.config = config;
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return config.getAnnotatedClassesResourcePath();
    }

    @Override
    protected String getPathToTestResource() {
        String testResourcePath = config.getTestResourcePath();
        return testResourcePath != null ? testResourcePath : super.getPathToTestResource();
    }

    public MockHstRequest getHstRequest() {
        return request;
    }

    public MockHstResponse getHstResponse() {
        return response;
    }

    public MockHstRequestContext getHstRequestContext() {
        return requestContext;
    }

    public MockResolvedSiteMapItem getResolvedSiteMapItem() {
        return resolvedSiteMapItem;
    }

    public MockComponentConfiguration getComponentConfiguration() {
        return componentConfiguration;
    }

    public MockComponentManager getMockComponentManager() {
        return componentManager;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public HippoBean getHippoBean(String path) {
        return super.getHippoBean(path);
    }

    public void setSiteContentBasePath(String path) {
        setSiteContentBase(path);
    }

    public void setContentBean(String path) {
        setContentBean(getHippoBean(path));
    }

    public void setContentBean(HippoBean bean) {
        requestContext.setContentBean(bean);
    }

    public void registerNodeType(String nodeType) throws RepositoryException {
        super.registerNodeType(nodeType);
    }

    public void registerNodeType(String nodeType, String superType) throws RepositoryException {
        super.registerNodeType(nodeType, superType);
    }

    public void registerMixinType(String mixinType) throws RepositoryException {
        super.registerMixinType(mixinType);
    }

    public void recalculateRepositoryPaths() {
        recalculateHippoPaths();
    }

    public void addRequestParameter(String name, String value) {
        addPublicRequestParameter(name, value);
    }

    public void addRequestParameter(String name, String[] value) {
        addPublicRequestParameter(name, value);
    }

    public <T> T getRequestAttributeValue(String name) {
        return getRequestAttribute(name);
    }

    public void setComponentParameters(Object parameterInfo) {
        setComponentParameterInfo(parameterInfo);
    }
}
