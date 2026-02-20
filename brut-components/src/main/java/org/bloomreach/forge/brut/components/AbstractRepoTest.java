package org.bloomreach.forge.brut.components;

import org.apache.commons.lang3.StringUtils;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.common.repository.utils.NodeTypeUtils;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;
import org.hippoecm.hst.component.support.spring.util.MetadataReaderClasspathResourceScanner;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.search.HstQueryManagerFactoryImpl;
import org.hippoecm.hst.site.content.ObjectConverterFactoryBean;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.util.DateTools;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;

public abstract class AbstractRepoTest extends SimpleComponentTest {

    private static final String SLASH = "/";
    protected Node rootNode;
    protected ObjectConverter objectConverter;
    protected ObjectBeanManager objectBeanManager;
    protected HstQueryManager hstQueryManager;

    @Override
    public void setup() {
        super.setup();
        if (objectConverter == null) {
            try {
                if (shouldRegisterBaseNodeTypes()) {
                    registerBaseNodeTypes();
                }
                if (shouldImportNodeStructure()) {
                    importNodeStructure();
                }
                requestContext.setSession(this.rootNode.getSession());
                setObjectConverter();
                setQueryManager();
            } catch (Exception e) {
                throw new SetupTeardownException(e);
            }
        }
    }

    /**
     * Controls whether {@link #registerBaseNodeTypes()} runs during {@link #setup()}.
     * <p>
     * Return {@code false} when the repository already has the base node types registered
     * (e.g. a shared repository that was fully bootstrapped by a previous test instance)
     * to avoid ~50 redundant {@code hasNodeType()} lookups per test class.
     * The default implementation always returns {@code true}.
     * </p>
     */
    protected boolean shouldRegisterBaseNodeTypes() {
        return true;
    }

    /**
     * Controls whether {@link #importNodeStructure()} runs during {@link #setup()}.
     * <p>
     * Return {@code false} when the repository has already been bootstrapped by another test
     * instance (e.g. a shared repository scenario) to prevent duplicate-node errors.
     * The default implementation always returns {@code true}.
     * </p>
     */
    protected boolean shouldImportNodeStructure() {
        return true;
    }

    protected HippoBean getHippoBean(String path) {
        try {
            return (HippoBean) requestContext.getObjectBeanManager().getObject(path);
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException(e);
        }
    }

    protected void setContentBean(String path) {
        requestContext.setContentBean(getHippoBean(path));
    }

    /**
     * Set the SiteContentBasePath and the siteContentBaseBean on HstRequestContext
     *
     * @param path absolute path. It must not be null or empty and it must start with a /
     */
    protected void setSiteContentBase(String path) {
        if (StringUtils.isBlank(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("Parameter path must be a String that starts with /");
        }
        try {
            // here it must be absolute
            HippoBean hippoBean = (HippoBean) requestContext.getObjectBeanManager().getObject(path);
            // here it must be relative to root
            requestContext.setSiteContentBasePath(path.substring(1));
            requestContext.setSiteContentBaseBean(hippoBean);
            // Also configure the mount's content path for consistency
            mount.setContentPath(path.substring(1));
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException(e);
        }
    }

    /**
     * Auto-resolve the site content base bean from the mount's configured content path.
     * This bridges the gap between HST configuration and HstRequestContext.
     * <p>
     * Call this method after the mount has been configured with a content path,
     * typically via HCM/YAML configuration or manual {@code mount.setContentPath()}.
     * </p>
     *
     * @return true if auto-resolution succeeded, false if contentPath was null/empty or bean couldn't be resolved
     */
    protected boolean autoResolveSiteContentBase() {
        String contentPath = mount.getContentPath();
        if (StringUtils.isBlank(contentPath)) {
            return false;
        }
        String absolutePath = contentPath.startsWith("/") ? contentPath : "/" + contentPath;
        try {
            HippoBean hippoBean = (HippoBean) requestContext.getObjectBeanManager().getObject(absolutePath);
            if (hippoBean == null) {
                return false;
            }
            requestContext.setSiteContentBasePath(contentPath.startsWith("/") ? contentPath.substring(1) : contentPath);
            requestContext.setSiteContentBaseBean(hippoBean);
            return true;
        } catch (ObjectBeanManagerException e) {
            return false;
        }
    }

    /**
     * @return the site content base bean, or null if not configured
     */
    protected HippoBean getSiteContentBaseBeanOrNull() {
        return requestContext.getSiteContentBaseBean();
    }

    /**
     * @return true if the site content base bean is available
     */
    protected boolean hasSiteContentBaseBean() {
        return requestContext.getSiteContentBaseBean() != null;
    }

    /**
     * @return the site content base bean
     * @throws IllegalStateException if the site content base bean is not configured
     */
    protected HippoBean requireSiteContentBaseBean() {
        HippoBean baseBean = requestContext.getSiteContentBaseBean();
        if (baseBean == null) {
            String contentPath = mount.getContentPath();
            throw new IllegalStateException(
                "Site content base bean is null. Mount contentPath: '" + contentPath + "'. " +
                "Call setSiteContentBase() or ensure the content folder exists in your test YAML."
            );
        }
        return baseBean;
    }

    /**
     * Safely retrieves a bean relative to the site content base.
     *
     * @param relativePath path relative to the site content base (e.g., "banners/hero")
     * @return the resolved bean, or null if base bean is null or path doesn't resolve
     */
    protected HippoBean getRelativeBean(String relativePath) {
        HippoBean baseBean = getSiteContentBaseBeanOrNull();
        if (baseBean == null) {
            return null;
        }
        return baseBean.getBean(relativePath);
    }

    /**
     * Safely retrieves a typed bean relative to the site content base.
     *
     * @param relativePath path relative to the site content base
     * @param beanClass    the expected bean class
     * @param <T>          the bean type
     * @return the resolved bean, or null if base bean is null or path doesn't resolve
     */
    protected <T extends HippoBean> T getRelativeBean(String relativePath, Class<T> beanClass) {
        HippoBean baseBean = getSiteContentBaseBeanOrNull();
        if (baseBean == null) {
            return null;
        }
        return baseBean.getBean(relativePath, beanClass);
    }

    protected abstract String getAnnotatedClassesResourcePath();

    private void importNodeStructure() throws IOException, RepositoryException, JAXBException {
        String pathToResource = getPathToTestResource();
        if (pathToResource != null && (pathToResource.endsWith(".yaml") || pathToResource.endsWith(".yml"))) {
            URL resource = getClass().getResource(pathToResource);
            ImporterUtils.importYaml(resource, rootNode, "", "hippostd:folder");
        } else {
            throw new SetupTeardownException(new Exception("invalid import file format"));
        }
        moveSkeletonRootToRoot("root");
    }

    private void moveSkeletonRootToRoot(String rootNodeName) throws RepositoryException {
        Node skeletonRoot = this.rootNode.getNode(rootNodeName);
        NodeIterator it = skeletonRoot.getNodes();
        while (it.hasNext()) {
            Node current = it.nextNode();
            rootNode.getSession().move(current.getPath(), "/" + current.getPath().split(rootNodeName)[1].substring(1));
        }
        skeletonRoot.remove();
        rootNode.getSession().save();
    }


    private void setQueryManager() throws RepositoryException {
        hstQueryManager = new HstQueryManagerImpl(this.rootNode.getSession(), this.objectConverter, DateTools.Resolution.MILLISECOND);
        requestContext.setDefaultHstQueryManager(hstQueryManager);
        requestContext.setHstQueryManagerFactory(new HstQueryManagerFactoryImpl());
        HashMap<Session, HstQueryManager> map = new HashMap<>();
        map.put(this.rootNode.getSession(), hstQueryManager);
        requestContext.setNonDefaultHstQueryManagers(map);
    }

    protected void setObjectConverter() throws Exception {
        MetadataReaderClasspathResourceScanner resourceScanner = new MetadataReaderClasspathResourceScanner();
        resourceScanner.setResourceLoader(new PathMatchingResourcePatternResolver());
        ObjectConverterFactoryBean objectConverterFactory = new ObjectConverterFactoryBean();
        objectConverterFactory.setClasspathResourceScanner(resourceScanner);
        objectConverterFactory.setAnnotatedClassesResourcePath(getAnnotatedClassesResourcePath());
        objectConverterFactory.setGenerateDynamicBean(false); //disable dynamic beans feature
        objectConverterFactory.afterPropertiesSet();
        this.objectConverter = objectConverterFactory.getObject();

        this.objectBeanManager = new ObjectBeanManagerImpl(this.rootNode.getSession(), objectConverter);
        this.requestContext.setDefaultObjectBeanManager(objectBeanManager);
        HashMap<Session, ObjectBeanManager> map = new HashMap<>();
        map.put(this.rootNode.getSession(), objectBeanManager);
        this.requestContext.setNonDefaultObjectBeanManagers(map);

    }

    protected void registerMixinType(String mixinType) throws RepositoryException {
        NodeTypeUtils.createMixin(rootNode.getSession(), mixinType);
    }

    protected void registerNodeType(String nodeType) throws RepositoryException {
        NodeTypeUtils.createNodeType(rootNode.getSession(), nodeType);
    }

    protected void registerNodeType(String nodeType, String superType) throws RepositoryException {
        NodeTypeUtils.createNodeType(rootNode.getSession(), nodeType, superType);
    }

    protected InputStream getResourceAsStream(String pathToResource) {
        InputStream result;
        if (pathToResource.startsWith("/")) {
            result = this.getClass().getClassLoader().getResourceAsStream(PathUtils.normalizePath(pathToResource));
        } else {
            result = this.getClass().getResourceAsStream(pathToResource);
        }
        return result;
    }

    protected void recalculateHippoPaths() {
        recalculateHippoPaths(true);
    }

    protected void recalculateHippoPaths(boolean save) {
        recalculateHippoPaths("/content", save);
    }

    protected void recalculateHippoPaths(String absolutePath, boolean save) {
        try {
            validateAbsolutePath(absolutePath);
            Node node = rootNode.getNode(absolutePath.substring(1));
            calculateHippoPaths(node, getPathsForNode(node));
            if (save) {
                rootNode.getSession().save();
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    protected void printNodeStructure(String absolutePath) {
        printNodeStructure(absolutePath, System.out);
    }

    protected void printNodeStructure(String absolutePath, PrintStream printStream) {
        try {
            validateAbsolutePath(absolutePath);
            Node node;
            if (SLASH.equals(absolutePath)) {
                node = rootNode;
            } else {
                node = rootNode.getNode(absolutePath.substring(1));
            }
            printStream.println(node.getName());
            printSubNodes(printStream, node, "");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    private void printSubNodes(PrintStream ps, Node node, String prefix) throws RepositoryException {
        for (NodeIterator iterator = node.getNodes(); iterator.hasNext(); ) {
            Node subnode = iterator.nextNode();
            ps.println(prefix + "   |_" + subnode.getName());
            if (iterator.hasNext()) {
                printSubNodes(ps, subnode, prefix + "   |");
            } else {
                printSubNodes(ps, subnode, prefix + "    ");
            }
        }
    }

    private void validateAbsolutePath(String absolutePath) {
        if (!absolutePath.startsWith(SLASH)) {
            throw new IllegalArgumentException("The path is not absolute.");
        }
    }

    private LinkedList<String> getPathsForNode(Node node) throws RepositoryException {
        LinkedList<String> paths = new LinkedList<>();
        Node parentNode = node;
        do {
            parentNode = parentNode.getParent();
            paths.add(parentNode.getIdentifier());
        } while (!parentNode.isSame(rootNode));
        return paths;
    }

    @SuppressWarnings("unchecked")
    private void calculateHippoPaths(Node node, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, node.getIdentifier());
        setHippoPath(node, paths);
        for (NodeIterator nodes = node.getNodes(); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            if (!subnode.isNodeType("hippo:handle")) {
                if (!subnode.isNodeType("hippotranslation:translations")) {
                    calculateHippoPaths(subnode, (LinkedList<String>) paths.clone());
                }
            } else {
                setHandleHippoPaths(subnode, (LinkedList<String>) paths.clone());
            }
        }
    }

    private void setHippoPath(Node node, LinkedList<String> paths) throws RepositoryException {
        node.setProperty(HIPPO_PATHS, paths.toArray(new String[paths.size()]));
    }

    private void setHandleHippoPaths(Node handle, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, handle.getIdentifier());
        for (NodeIterator nodes = handle.getNodes(handle.getName()); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            paths.add(0, subnode.getIdentifier());
            setHippoPath(subnode, paths);
            paths.remove(0);
        }
    }

    /**
     * Override this method if you are importing cnds instead of manual registration!
     *
     * @throws Exception
     */

    protected void registerBaseNodeTypes() throws Exception {
        registerNodeType("frontend:application");
        registerNodeType("frontend:pluginconfig");
        registerNodeType("hippo:handle");
        registerNodeType("hippo:initializefolder");
        registerNodeType("hippo:mirror");
        registerNodeType("hippogallerypicker:imagelink", "hippo:facetselect");
        registerNodeType("hippolog:folder");
        registerNodeType("hipporeport:folder");
        registerNodeType("hippostd:document", "hippo:document");
        registerNodeType("hippostd:folder", "hippo:document");
        registerNodeType("hippostd:html");
        registerNodeType("hippostd:directory", "hippo:document");
        registerNodeType("hipposys:configuration");
        registerNodeType("hipposys:applicationfolder");
        registerNodeType("hipposys:modulefolder");
        registerNodeType("hipposys:derivativesfolder");
        registerNodeType("hipposys:temporaryfolder");
        registerNodeType("hipposys:queryfolder");
        registerNodeType("hipposys:workflowfolder");
        registerNodeType("hipposys:updaterfolder");
        registerNodeType("hipposys:resourcebundles");
        registerNodeType("hipposys:update");
        registerNodeType("hipposys:domainfolder");
        registerNodeType("hipposys:userfolder");
        registerNodeType("hipposys:groupfolder");
        registerNodeType("hipposys:rolefolder");
        registerNodeType("hipposys:securityfolder");
        registerNodeType("hipposys:securityprovider");
        registerNodeType("hipposys:userprovider");
        registerNodeType("hipposys:groupprovider");
        registerNodeType("hipposys:accessmanager");
        registerNodeType("hippostd:templatequery");
        registerNodeType("hippostd:templates");
        registerNodeType("hippofacnav:facetnavigation");
        registerNodeType("hippotranslation:translations");
        registerNodeType("hipposysedit:namespacefolder");
        registerNodeType("hipposysedit:namespace");
        registerNodeType("hipposysedit:templatetype");
        registerNodeType("hipposysedit:nodetype", "hippo:document");
        registerNodeType("hipposysedit:prototypeset");
        registerNodeType("hippostdpubwf:document");
        registerNodeType("hippostd:gallery", "hippostd:folder");
        registerNodeType("hippogallery:stdAssetGallery", "hippostd:gallery");
        registerNodeType("hippogallery:stdImageGallery", "hippostd:gallery");
        registerNodeType("hippogallery:imageset", "hippo:document");
        registerNodeType("hst:hst");
        registerNodeType("hst:formdatacontainer");
        registerNodeType("hst:configuration");
        registerNodeType("hst:configurations");
        registerNodeType("hst:pages");
        registerNodeType("hst:blueprints");
        registerNodeType("hst:channels");
        registerNodeType("hst:sites");
        registerNodeType("hst:virtualhosts");
        registerNodeType("hst:catalog");
        registerNodeType("hst:abstractcomponent");
        registerNodeType("hst:component", "hst:abstractcomponent");
        registerNodeType("hst:components");
        registerNodeType("hst:template");
        registerNodeType("hst:templates");
        registerNodeType("hst:sitemenus");
        registerNodeType("hst:sitemapitemhandlers");
        registerNodeType("hst:sitemapitem");
        registerNodeType("hst:sitemap");
        registerNodeType("selection:basedocument", "hippo:document");
        registerNodeType("selection:valuelist", "selection:basedocument");
        registerNodeType("selection:listitem", "hippo:compound");
        registerNodeType("webfiles:webfiles");

        registerMixinType("hipposysedit:remodel");
        registerMixinType("hippotranslation:translated");
        registerMixinType("hipposys:implementation");
        registerMixinType("hippo:lockable");
        registerMixinType("hippo:harddocument");
        registerMixinType("hippo:named");
    }
}
