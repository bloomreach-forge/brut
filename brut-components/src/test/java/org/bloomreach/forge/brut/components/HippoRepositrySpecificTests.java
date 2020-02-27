package org.bloomreach.forge.brut.components;

import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.RepositoryException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HippoRepositrySpecificTests extends BaseComponentTest {

    @BeforeEach
    public void setupScenario() throws RepositoryException {
        super.setup();
        registerNodeType("ns:account", "hippo:compound");
        registerNodeType("ns:basedocument", "hippo:document");
        registerNodeType("ns:author", "ns:basedocument");
        registerNodeType("ns:blogpost", "ns:basedocument");

        URL blogResource = getClass().getResource("/org/bloomreach/forge/brut/components/blog.yaml");
        URL blogFacetResource = getClass().getResource("/org/bloomreach/forge/brut/components/blogFacets.yaml");

        ImporterUtils.importYaml(blogResource, rootNode, "/content/documents/mychannel", "hippostd:folder");
        ImporterUtils.importYaml(blogFacetResource, rootNode, "/content/documents/mychannel", "hippostd:folder");

        setSiteContentBase("/content/documents/mychannel");

        recalculateHippoPaths();
    }

    @Test
    public void canonicalUUID() {
        HippoBean blogFolder = requestContext.getSiteContentBaseBean().getBean("blog");
        assertEquals("4143bbcf-784f-4385-9252-0dc47630a6ad", blogFolder.getCanonicalUUID());
        HippoDocument firstBlogPost = requestContext.getSiteContentBaseBean().getBean("blog/2019/02/first-blog-post");
        assertEquals("548aadde-d77e-4f8f-b878-c4e15d1b5c69", firstBlogPost.getCanonicalUUID());
        assertEquals("59f4668d-6cd4-448d-86fa-1d76abdc4657", firstBlogPost.getCanonicalHandleUUID());
    }

    @Test
    public void canonicalHandlePath() {
        HippoDocument firstBlogPost = requestContext.getSiteContentBaseBean().getBean("blog/2019/02/first-blog-post");
        assertEquals("/content/documents/mychannel/blog/2019/02/first-blog-post/first-blog-post", firstBlogPost.getCanonicalPath());
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:org/bloomreach/forge/brut/components/demo/domain/**/*.class";
    }

    @AfterEach
    public void teardown() {
        super.teardown();
    }
}
