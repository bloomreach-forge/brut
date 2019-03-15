package com.bloomreach.ps.brxm.jcr.repository.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bloomreach.ps.brxm.jcr.repository.InMemoryJcrRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImporterUtilsTest {

    private InMemoryJcrRepository repository;
    private Session session;

    @Before
    public void setup() throws RepositoryException, IOException {
        this.repository = new InMemoryJcrRepository();
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull("Root node was null", session.getRootNode());
        InputStream hippoCnd = getResourceAsStream("com/bloomreach/ps/brxm/jcr/repository/utils/hippo.cnd");
        InputStream hippoStdCnd = getResourceAsStream("com/bloomreach/ps/brxm/jcr/repository/utils/hippostd.cnd");
        InputStream hippogalleryCnd = getResourceAsStream("com/bloomreach/ps/brxm/jcr/repository/utils/hippogallery.cnd");
        ImporterUtils.registerNamespaces(hippoCnd, session);
        ImporterUtils.registerNamespaces(hippoStdCnd, session);
        ImporterUtils.registerNamespaces(hippogalleryCnd, session);
    }

    @Test
    public void testRegisterNamespace() throws Exception {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        assertTrue("hippo:document was not registered", nodeIteratorToStream(nodeTypeManager.getPrimaryNodeTypes())
                .anyMatch(nodeType -> nodeType.isNodeType("hippo:document")));

        assertTrue("hippostd:folder was not registered", nodeIteratorToStream(nodeTypeManager.getPrimaryNodeTypes())
                .anyMatch(nodeType -> nodeType.isNodeType("hippostd:folder")));
    }

    @Test
    public void testImportingImage() throws Exception {
        URL resource = getClass().getResource("/com/bloomreach/ps/brxm/jcr/repository/utils/gallery.yaml");
        ImporterUtils.importYaml(resource, session.getRootNode(), "/content", "hippostd:folder");

        Node thumbnail = session.getNode("/content/gallery/myhippoproject/samples/viognier-grapes-188185_640.jpg/viognier-grapes-188185_640.jpg/hippogallery:thumbnail");
        assertNotNull("Thumbnail node was null", thumbnail);
        assertEquals("Thumbnail primary type was incorrect", "hippogallery:image", thumbnail.getPrimaryNodeType().getName());
        assertTrue("Thumbnail has empty binary data", thumbnail.getProperty("jcr:data").getBinary().getSize() > 0);
    }

    @Test
    public void testImportingContentNodes() throws Exception {
//        ImporterUtils.importYaml();
    }


    @After
    public void tearDown() {
        if (session != null && session.isLive()) {
            session.logout();
        }
        repository.close();
    }


    private Stream<NodeType> nodeIteratorToStream(NodeTypeIterator sourceIterator) {
        @SuppressWarnings("unchecked") Iterable<NodeType> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    @Nullable
    private InputStream getResourceAsStream(String classpathLocation) {
        return getClass().getClassLoader().getResourceAsStream(classpathLocation);
    }


}
