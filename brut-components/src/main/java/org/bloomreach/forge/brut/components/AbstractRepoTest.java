package org.bloomreach.forge.brut.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
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

import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.common.repository.utils.NodeTypeUtils;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;

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
        try {
            registerBaseNodeTypes();
            importNodeStructure();
            setObjectConverter();
            setQueryManager();
        } catch (Exception e) {
            throw new SetupTeardownException(e);
        }
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
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException(e);
        }
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
        requestContext.setSession(this.rootNode.getSession());
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

    private void setObjectConverter() throws Exception {
        MetadataReaderClasspathResourceScanner resourceScanner = new MetadataReaderClasspathResourceScanner();
        resourceScanner.setResourceLoader(new PathMatchingResourcePatternResolver());
        ObjectConverterFactoryBean objectConverterFactory = new ObjectConverterFactoryBean();
        objectConverterFactory.setClasspathResourceScanner(resourceScanner);
        objectConverterFactory.setAnnotatedClassesResourcePath(getAnnotatedClassesResourcePath());
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
        registerNodeType("hippogallerypicker:imagelink");
        registerNodeType("hippolog:folder");
        registerNodeType("hipporeport:folder");
        registerNodeType("hippostd:document");
        registerNodeType("hippostd:folder");
        registerNodeType("hippostd:html");
        registerNodeType("hippostd:directory");
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
        registerNodeType("hippotranslation:id");
        registerNodeType("hippofacnav:facetnavigation");
        registerNodeType("hippotranslation:translations");
        registerNodeType("hipposysedit:namespacefolder");
        registerNodeType("hipposysedit:namespace");
        registerNodeType("hipposysedit:templatetype");
        registerNodeType("hipposysedit:nodetype");
        registerNodeType("hipposysedit:prototypeset");
        registerNodeType("hippostdpubwf:document");
        registerNodeType("hippogallery:stdAssetGallery");
        registerNodeType("hippogallery:stdImageGallery");
        registerNodeType("hippogallery:imageset");
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
        registerNodeType("hst:component");
        registerNodeType("hst:components");
        registerNodeType("hst:template");
        registerNodeType("hst:templates");
        registerNodeType("hst:sitemenus");
        registerNodeType("hst:sitemapitemhandlers");
        registerNodeType("hst:sitemapitem");
        registerNodeType("hst:sitemap");
        registerNodeType("selection:valuelist");
        registerNodeType("selection:listitem");

        registerNodeType("webfiles:webfiles");

        registerMixinType("hipposysedit:remodel");
        registerMixinType("hippotranslation:translated");
        registerMixinType("hipposys:implementation");
        registerMixinType("hippo:lockable");
        registerMixinType("hippo:harddocument");
        registerMixinType("hippo:named");
    }
}
