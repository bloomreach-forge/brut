package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.components.BaseComponentTest;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;
import org.bloomreach.forge.brut.components.mock.MockComponentManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.request.MockComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.request.MockResolvedSiteMapItem;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.URL;

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

    public BrxmTestingRepository getRepository() {
        return super.getRepository();
    }

    public Session getSession() throws RepositoryException {
        return rootNode != null ? rootNode.getSession() : null;
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

    /**
     * Register multiple node types at once.
     * Wraps RepositoryException in SetupTeardownException for cleaner test code.
     *
     * @param nodeTypes varargs of node type names (e.g., "myproject:Article", "myproject:Author")
     */
    public void registerNodeTypes(String... nodeTypes) {
        try {
            for (String nodeType : nodeTypes) {
                super.registerNodeType(nodeType);
            }
        } catch (RepositoryException e) {
            throw new SetupTeardownException(e);
        }
    }

    /**
     * Register a node type with a specified supertype.
     * Wraps RepositoryException in SetupTeardownException for cleaner test code.
     *
     * @param nodeType  the node type to register (e.g., "myproject:Article")
     * @param superType the supertype for inheritance (e.g., "myproject:BaseDocument")
     */
    public void registerNodeTypeWithSupertype(String nodeType, String superType) {
        try {
            super.registerNodeType(nodeType, superType);
        } catch (RepositoryException e) {
            throw new SetupTeardownException(e);
        }
    }

    /**
     * Import stubbed YAML content into the repository.
     * Uses "hippostd:folder" as the default folder type.
     *
     * @param resourcePath classpath resource path (e.g., "/test-content.yaml")
     * @param targetPath   target path in repository (e.g., "/content/documents")
     */
    public void importYaml(String resourcePath, String targetPath) {
        importYaml(resourcePath, targetPath, "hippostd:folder");
    }

    /**
     * Import YAML content into the repository with custom folder type.
     *
     * @param resourcePath classpath resource path (e.g., "/test-content.yaml")
     * @param targetPath   target path in repository (e.g., "/content/documents")
     * @param folderType   JCR node type for folders (e.g., "hippostd:folder")
     */
    public void importYaml(String resourcePath, String targetPath, String folderType) {
        importYaml(resourcePath, targetPath, folderType, getClass());
    }

    /**
     * Import YAML content into the repository using a specific class's classloader.
     *
     * @param resourcePath classpath resource path (e.g., "/test-content.yaml")
     * @param targetPath   target path in repository (e.g., "/content/documents")
     * @param folderType   JCR node type for folders (e.g., "hippostd:folder")
     * @param resourceClass class to use for resource loading
     */
    public void importYaml(String resourcePath, String targetPath, String folderType, Class<?> resourceClass) {
        try {
            URL resource = resourceClass.getResource(resourcePath);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            ImporterUtils.importYaml(resource, getRootNode(), targetPath, folderType);
        } catch (Exception e) {
            throw new SetupTeardownException(e);
        }
    }
}
