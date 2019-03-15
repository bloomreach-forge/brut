package com.bloomreach.ps.brxm.unittester;

import java.net.URL;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetNavigation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bloomreach.ps.brxm.jcr.repository.utils.ImporterUtils;

public class HippoRepositrySpecificTests extends BaseHippoTest {

    @Before
    public void setupScenario() throws RepositoryException {
        super.setup();
        registerNodeType("ns:account", "hippo:compound");
        registerNodeType("ns:basedocument", "hippo:document");
        registerNodeType("ns:author", "ns:basedocument");
        registerNodeType("ns:blogpost", "ns:basedocument");

        URL blogResource = getClass().getResource("/com/bloomreach/ps/brxm/unittester/blog.yaml");
        URL blogFacetResource = getClass().getResource("/com/bloomreach/ps/brxm/unittester/blogFacets.yaml");

        ImporterUtils.importYaml(blogResource, rootNode, "/content/documents/mychannel", "hippostd:folder");
        ImporterUtils.importYaml(blogFacetResource, rootNode, "/content/documents/mychannel", "hippostd:folder");

        setSiteContentBase("/content/documents/mychannel");

        recalculateHippoPaths();
    }

    @Test
    public void canonicalUUID() {
        HippoBean blogFolder = requestContext.getSiteContentBaseBean().getBean("blog");
        Assert.assertEquals("4143bbcf-784f-4385-9252-0dc47630a6ad", blogFolder.getCanonicalUUID());
        HippoDocument firstBlogPost = requestContext.getSiteContentBaseBean().getBean("blog/2019/02/first-blog-post");
        Assert.assertEquals("548aadde-d77e-4f8f-b878-c4e15d1b5c69", firstBlogPost.getCanonicalUUID());
        Assert.assertEquals("59f4668d-6cd4-448d-86fa-1d76abdc4657", firstBlogPost.getCanonicalHandleUUID());
    }

    @Test
    public void canonicalHandlePath() {
        HippoDocument firstBlogPost = requestContext.getSiteContentBaseBean().getBean("blog/2019/02/first-blog-post");
        Assert.assertEquals("/content/documents/mychannel/blog/2019/02/first-blog-post/first-blog-post", firstBlogPost.getCanonicalPath());
    }

    @Test
    @Ignore
    public void facetedNavigation() {
        HippoFacetNavigation facet = requestContext.getSiteContentBaseBean().getBean("blogFacets");
        Assert.assertEquals((Long) 2L, facet.getCount());
        Assert.assertEquals(2, facet.getDocumentSize());
        Assert.assertNotNull(facet.getResultSet());
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:com/bloomreach/ps/brxm/unittester/demo/domain/**/*.class";
    }

    @After
    public void teardown() {
        super.teardown();
    }
}
