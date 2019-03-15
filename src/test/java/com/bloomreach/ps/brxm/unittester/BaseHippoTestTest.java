package com.bloomreach.ps.brxm.unittester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.bloomreach.ps.brxm.jcr.repository.utils.ImporterUtils;
import com.bloomreach.ps.brxm.unittester.demo.domain.NewsPage;
import com.bloomreach.ps.brxm.unittester.exception.SetupTeardownException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BaseHippoTestTest extends BaseHippoTest {


    private static final String COMPONENT_NAME = "myComponent";

    @Test
    public void testHstRequest() {
        request.addParameter("q", "value01");
        assertEquals("value01", request.getParameter("q"));
        assertNull(request.getParameter("test"));

        assertNotNull(request.getParameterMap());
        assertArrayEquals(new String[]{"value01"}, (String[]) request.getParameterMap().get("q"));

        assertEquals(requestContext, request.getRequestContext());
        request.setAttribute("test", "value");
        assertEquals("value", request.getAttribute("test"));

        assertNotNull(request.getAttributeMap());
        request.getAttributeMap().put("test", "value");
        assertEquals("value", request.getAttributeMap().get("test"));

    }

    @Test
    public void setComponentTest() {
        componentManager.addComponent(COMPONENT_NAME, 1);
        assertEquals((Object) 1, componentManager.getComponent(COMPONENT_NAME));
    }

    @Test
    public void nullComponentTest() {
        assertNull(componentManager.getComponent(COMPONENT_NAME));
    }

    @Test
    public void recalculateHippoPathsTest() throws RepositoryException {
        recalculateHippoPaths();
        Node node = rootNode.getNode("content/documents");
        testHippoPaths(node);
    }

    @Test
    public void recalculateHippoPathsWithPathTest() throws RepositoryException {
        recalculateHippoPaths("/content/documents/mychannel", true);
        Node node = rootNode.getNode("content/documents/mychannel");
        testHippoPaths(node);
        node = rootNode.getNode("content/documents");
        assertFalse(node.hasProperty(HIPPO_PATHS));
    }


    @Test
    public void recalculateHippoPathsNotSaveTest() throws RepositoryException, QueryException {
        recalculateHippoPaths(false);
        Node scope = rootNode.getNode("content/documents/mychannel");
        HstQuery query = HstQueryBuilder.create(scope)
                .ofTypes(NewsPage.class)
                .build();

        assertEquals(0, query.execute().getSize());
        rootNode.getSession().save();
        assertEquals(3, query.execute().getSize());
    }

    private void testHippoPaths(Node node) throws RepositoryException {
        Property property = node.getProperty(HIPPO_PATHS);
        for (Value value : property.getValues()) {
            assertEquals(node.getIdentifier(), value.getString());
            if (!node.isSame(rootNode)) {
                node = node.getParent();
            }
        }
    }

    @Before
    public void setup() {
        try {
            super.setup();
            registerNodeType("ns:NewsPage", "ns:AnotherType");
            URL newsResource = getClass().getResource("/com/bloomreach/ps/brxm/unittester/demo/news.yaml");
            ImporterUtils.importYaml(newsResource, rootNode, "/content/documents/mychannel", "hippostd:folder");
        } catch (RepositoryException e) {
            throw new SetupTeardownException(e);
        }
    }

    @Test
    public void printNodeStructureTest() throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            printNodeStructure("/content", new PrintStream(os));

            String expectedValue = IOUtils.toString(
                    getResourceAsStream("expected-node-structure.txt")
                    , UTF_8);

            String actualValue = new String(os.toByteArray(), UTF_8);

            assertEquals(getOsIndependantValue(expectedValue), getOsIndependantValue(actualValue));
        }
    }

    private String getOsIndependantValue(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    @Test
    public void hippoMirrorTest() {
        NewsPage hippoBean = (NewsPage) getHippoBean("/content/documents/mychannel/news/news3");
        assertEquals("news2", hippoBean.getRelatedNews().getName());
    }


    @Test(expected = IllegalArgumentException.class)
    public void setSiteContentBaseTestNull() {
        setSiteContentBase(null);
        Assert.fail("Expected a IllegalArgumentException");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSiteContentBaseTestRelative() {
        setSiteContentBase("a/b");
        Assert.fail("Expected a IllegalArgumentException");
    }

    @Test
    public void setSiteContentBaseTestAbsolute() {
        setSiteContentBase("/content/documents/mychannel/news/news3");
        Assert.assertEquals("content/documents/mychannel/news/news3", requestContext.getSiteContentBasePath());
        HippoBean siteContentBaseBean = requestContext.getSiteContentBaseBean();
        Assert.assertEquals("news3", siteContentBaseBean.getName());
    }

    @After
    public void teardown() {
        super.teardown();
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:com/bloomreach/ps/brxm/unittester/demo/domain/**/*.class";
    }

}