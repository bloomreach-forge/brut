package org.bloomreach.forge.brut.components.mock;

import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.components.BaseComponentTest;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MockHstLinkCreatorTest extends BaseComponentTest {

    private static final MockHstLink HST_LINK = new MockHstLink("https://www.bloomreach.com", "/diensten");

    @BeforeEach
    public void setup() {
        super.setup();
        try {
            registerNodeTypes();
            URL newsResource = getClass().getResource("/org/bloomreach/forge/brut/components/demo/news.yaml");
            ImporterUtils.importYaml(newsResource, rootNode, "/content/documents/mychannel", "hippostd:folder");
        } catch (RepositoryException ex) {
            throw new SetupTeardownException(new Exception("Error importing"));
        }

    }

    private void registerNodeTypes() throws RepositoryException {
        registerNodeType("ns:author");
        registerNodeType("ns:NewsPage");
        registerNodeType("ns:AnotherType");
    }

    @Test
    public void addHstLinkViaBean() throws RepositoryException {
        HippoBean hippoBean = getHippoBean("/content/documents/mychannel/news/news1");
        hstLinkCreator.addHstLink(hippoBean, HST_LINK);
        testLinkViaBeanAndNodeAndUuid(hippoBean);
    }

    @Test
    public void addHstLinkViaNode() throws RepositoryException {
        HippoBean hippoBean = getHippoBean("/content/documents/mychannel/news/news1");
        hstLinkCreator.addHstLink(hippoBean.getNode(), HST_LINK);
        testLinkViaBeanAndNodeAndUuid(hippoBean);
    }

    @Test
    public void addHstLinkViaHandleNode() throws RepositoryException {
        HippoBean hippoBean = getHippoBean("/content/documents/mychannel/news/news1");
        hstLinkCreator.addHstLink(hippoBean.getNode().getParent(), HST_LINK);
        testLinkViaBeanAndNodeAndUuid(hippoBean);
    }

    @Test
    public void addHstLinkViaUuid() throws RepositoryException {
        HippoBean hippoBean = getHippoBean("/content/documents/mychannel/news/news1");
        hstLinkCreator.addHstLink(hippoBean.getNode().getParent().getIdentifier(), HST_LINK);
        testLinkViaBeanAndNodeAndUuid(hippoBean);
    }

    @Test
    public void addHstLinkViaSiteMap() throws RepositoryException {
        HstSiteMapItem mapItem = Mockito.mock(HstSiteMapItem.class);
        hstLinkCreator.addHstLink(mapItem, HST_LINK);
        HstLink hstLink = hstLinkCreator.create(mapItem, mount);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));
    }

    @Test
    public void addHstLinkViaRefId() throws RepositoryException {
        hstLinkCreator.addHstLinkByRefId("refId", HST_LINK);
        HstLink hstLink = hstLinkCreator.createByRefId("refId", mount);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));
    }

    @Test
    public void createHstLinkViaPath() throws RepositoryException {
        HstLink hstLink = hstLinkCreator.create("/some/path", mount);
        assertEquals("/some/path/", hstLink.toUrlForm(requestContext, true));
    }

    private void testLinkViaBeanAndNodeAndUuid(HippoBean hippoBean) throws RepositoryException {
        HstLink hstLink = hstLinkCreator.create(hippoBean, requestContext);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        hstLink = hstLinkCreator.create(hippoBean.getNode(), requestContext);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        hstLink = hstLinkCreator.create(getHandleNode(hippoBean), requestContext);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        hstLink = hstLinkCreator.create(getHandleNode(hippoBean).getIdentifier(), rootNode.getSession(), requestContext);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        hstLink = hstLinkCreator.createCanonical(getHandleNode(hippoBean), requestContext);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        hstLink = hstLinkCreator.createCanonical(hippoBean.getNode(), requestContext, null);
        assertEquals("https://www.bloomreach.com/diensten", hstLink.toUrlForm(requestContext, true));

        List<HstLink> links = hstLinkCreator.createAllAvailableCanonicals(hippoBean.getNode(), requestContext);
        assertEquals("https://www.bloomreach.com/diensten", links.get(0).toUrlForm(requestContext, true));
    }

    private Node getHandleNode(HippoBean hippoBean) throws RepositoryException {
        return hippoBean.getNode().getParent();
    }

    @AfterEach
    public void teardown() {
        super.teardown();
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:org/bloomreach/forge/brut/components/demo/domain/**/*.class";
    }

}