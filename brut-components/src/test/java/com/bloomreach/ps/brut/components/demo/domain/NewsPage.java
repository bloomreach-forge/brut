package com.bloomreach.ps.brut.components.demo.domain;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "ns:NewsPage")
public class NewsPage extends HippoDocument {

    public String getTitle() {
        return getProperty("ns:title");
    }

    public String getIntroduction() {
        return getProperty("ns:introduction");
    }

    public String getSubjecttags() {
        return getProperty("ns:subjecttags");
    }

    public String getBrowserTitle() {
        return getProperty("ns:browserTitle");
    }

    public NewsPage getRelatedNews() {
        return getLinkedBean("ns:relatedNews", NewsPage.class);
    }

}
