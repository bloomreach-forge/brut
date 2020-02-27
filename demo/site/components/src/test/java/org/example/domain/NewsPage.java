package org.example.domain;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "ns:NewsPage")
public class NewsPage extends HippoDocument {

    public String getTitle() {
        return getSingleProperty("ns:title");
    }

    public String getIntroduction() {
        return getSingleProperty("ns:introduction");
    }

    public String getSubjecttags() {
        return getSingleProperty("ns:subjecttags");
    }

    public String getBrowserTitle() {
        return getSingleProperty("ns:browserTitle");
    }

    public NewsPage getRelatedNews() {
        return getLinkedBean("ns:relatedNews", NewsPage.class);
    }

}
