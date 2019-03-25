package com.bloomreach.ps.brut.components.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;

import static org.hippoecm.hst.platform.linking.DefaultHstLinkCreator.BINARIES_PREFIX;
import static org.hippoecm.hst.platform.linking.DefaultHstLinkCreator.BINARIES_START_PATH;

public class MockHstLinkCreator implements HstLinkCreator {

    private static final MockHstLink NOT_FOUND_LINK = new MockHstLink("/", "error");
    private Map<String, HstLink> uuidToLink = new HashMap<>();
    private Map<String, HstLink> refIdToLink = new HashMap<>();
    private Map<HstSiteMapItem, HstLink> siteMapToLink = new HashMap<>();

    @Override
    public HstLink create(String uuid, Session session, HstRequestContext requestContext) {
        return getMockLink(uuid);
    }

    @Override
    public HstLink create(Node node, HstRequestContext requestContext) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback, boolean navigationStateful) {
        return getMockLink(node);
    }

    @Override
    public HstLink createCanonical(Node node, HstRequestContext requestContext) {
        return getMockLink(node);
    }

    @Override
    public HstLink createCanonical(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem) {
        return getMockLink(node);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext) {
        return getMockLinks(node);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type) {
        return getMockLinks(node);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type, String hostGroupName) {
        return getMockLinks(node);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, Mount mount, String type, String hostGroupName) {
        return getMockLinks(node);
    }

    @Override
    public HstLink create(Node node, Mount mount) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(final Node node, final Mount mount, final boolean crossMount) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(Node node, Mount mount, HstSiteMapItem preferredItem, boolean fallback) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(Node node, HstRequestContext requestContext, String mountAlias) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(Node node, HstRequestContext requestContext, String mountAlias, String type) {
        return getMockLink(node);
    }

    @Override
    public HstLink create(HippoBean bean, HstRequestContext requestContext) {
        try {
            return getMockLink(getUuid(bean.getNode()));
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    @Override
    public HstLink create(HstSiteMapItem toHstSiteMapItem, Mount mount) {
        return getMockLink(toHstSiteMapItem);
    }


    @Override
    public HstLink createByRefId(String siteMapItemRefId, Mount mount) {
        return getMockLinkByRefId(siteMapItemRefId);
    }

    @Override
    public HstLink create(String path, Mount mount) {
        return new MockHstLink(path);
    }

    @Override
    public HstLink create(String path, Mount mount, boolean containerResource) {
        return new MockHstLink(path);
    }

    @Override
    public List<HstLink> createAll(Node node, HstRequestContext requestContext, boolean crossMount) {
        return getMockLinks(node);
    }

    @Override
    public List<HstLink> createAll(Node node, HstRequestContext requestContext, String hostGroupName, String type, boolean crossMount) {
        return getMockLinks(node);
    }

    @Override
    public List<HstLink> createAll(final Node node, final Mount mount, final String hostGroupName, final String type, final boolean crossMount) {
        return getMockLinks(node);
    }

    @Override
    public HstLink createPageNotFoundLink(Mount mount) {
        return NOT_FOUND_LINK;
    }

    @Override
    public boolean isBinaryLocation(String path) {
        return path.startsWith(BINARIES_START_PATH);
    }

    @Override
    public String getBinariesPrefix() {
        return BINARIES_PREFIX;
    }

    @Override
    public List<LocationResolver> getLocationResolvers() {
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        // nothing to do here
    }

    private HstLink getMockLink(Node node) {
        try {
            return getMockLink(getUuid(node));
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    private String getUuid(Node node) throws RepositoryException {
        boolean isParentNodeHandle = !node.isNodeType(HippoNodeType.NT_HANDLE) && node.getParent() != null
                && node.getParent().isNodeType(HippoNodeType.NT_HANDLE);
        return isParentNodeHandle ? node.getParent().getIdentifier() : node.getIdentifier();
    }

    private List<HstLink> getMockLinks(Node node) {
        return Collections.singletonList(getMockLink(node));
    }

    private HstLink getMockLink(String uuid) {
        return uuidToLink.getOrDefault(uuid, NOT_FOUND_LINK);
    }

    private HstLink getMockLink(HstSiteMapItem siteMap) {
        return siteMapToLink.getOrDefault(siteMap, NOT_FOUND_LINK);
    }

    private HstLink getMockLinkByRefId(String refId) {
        return refIdToLink.getOrDefault(refId, NOT_FOUND_LINK);
    }

    public void addHstLink(String uuid, HstLink link) {
        uuidToLink.put(uuid, link);
    }

    public void addHstLink(HippoBean bean, HstLink link) {
        try {
            addHstLink(getUuid(bean.getNode()), link);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    public void addHstLink(Node node, HstLink link) {
        try {
            addHstLink(getUuid(node), link);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    public void addHstLink(HstSiteMapItem siteMapItem, HstLink link) {
        siteMapToLink.put(siteMapItem, link);
    }

    public void addHstLinkByRefId(String refId, HstLink link) {
        refIdToLink.put(refId, link);
    }

}
