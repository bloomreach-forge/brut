package com.bloomreach.ps.brut.common.repository.utils;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.bloomreach.ps.brut.common.repository.BrxmTestingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImporterUtilsTest {

    private BrxmTestingRepository repository;
    private Session session;

    @BeforeEach
    public void setup() throws RepositoryException, IOException {
        this.repository = new BrxmTestingRepository();
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(session.getRootNode());
        InputStream hippoCnd = getResourceAsStream("com/bloomreach/ps/brut/common/repository/utils/hippo.cnd");
        InputStream hippoStdCnd = getResourceAsStream("com/bloomreach/ps/brut/common/repository/utils/hippostd.cnd");
        InputStream hippogalleryCnd = getResourceAsStream("com/bloomreach/ps/brut/common/repository/utils/hippogallery.cnd");
        ImporterUtils.registerNamespaces(hippoCnd, session);
        ImporterUtils.registerNamespaces(hippoStdCnd, session);
        ImporterUtils.registerNamespaces(hippogalleryCnd, session);
    }

    @Test
    public void testRegisterNamespace() throws Exception {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        assertTrue(nodeIteratorToStream(nodeTypeManager.getPrimaryNodeTypes())
                .anyMatch(nodeType -> nodeType.isNodeType("hippo:document")));

        assertTrue(nodeIteratorToStream(nodeTypeManager.getPrimaryNodeTypes())
                .anyMatch(nodeType -> nodeType.isNodeType("hippostd:folder")));
    }

    @Test
    public void testImportingImage() throws Exception {
        URL resource = getClass().getResource("/com/bloomreach/ps/brut/common/repository/utils/gallery.yaml");
        ImporterUtils.importYaml(resource, session.getRootNode(), "/content", "hippostd:folder");

        Node thumbnail = session.getNode("/content/gallery/myhippoproject/samples/viognier-grapes-188185_640.jpg/viognier-grapes-188185_640.jpg/hippogallery:thumbnail");
        assertNotNull(thumbnail, "Thumbnail node was null");
        assertEquals("hippogallery:image", thumbnail.getPrimaryNodeType().getName(), "Thumbnail primary type was incorrect");
        assertTrue(thumbnail.getProperty("jcr:data").getBinary().getSize() > 0, "Thumbnail has empty binary data");
    }

    @AfterEach
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
