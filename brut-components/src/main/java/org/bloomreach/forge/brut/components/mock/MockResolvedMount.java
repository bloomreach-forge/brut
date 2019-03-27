package org.bloomreach.forge.brut.components.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;

public class MockResolvedMount implements MutableResolvedMount {
    private Mount mount;
    private ResolvedVirtualHost resolvedVirtualHost;
    private String resolvedMountPath;
    private String matchingIgnoredPrefix;
    private int portNumber;
    private Map<String, ResolvedSiteMapItem> sitemaps = new HashMap<>();

    public void addSitemap(String siteMapPathInfo, ResolvedSiteMapItem siteMapItem) {
        sitemaps.put(siteMapPathInfo, siteMapItem);
    }

    @Override
    public String getNamedPipeline() {
        return mount.getNamedPipeline();
    }

    @Override
    public boolean isAuthenticated() {
        return mount.isAuthenticated();
    }

    @Override
    public Set<String> getRoles() {
        return mount.getRoles();
    }

    @Override
    public Set<String> getUsers() {
        return mount.getUsers();
    }

    @Override
    public boolean isSubjectBasedSession() {
        return mount.isSubjectBasedSession();
    }

    @Override
    public boolean isSessionStateful() {
        return mount.isSessionStateful();
    }

    @Override
    public String getFormLoginPage() {
        return mount.getFormLoginPage();
    }

    @Override
    public Mount getMount() {
        return mount;
    }

    @Override
    public void setMount(Mount mount) {
        this.mount = mount;
    }

    public void setResolvedVirtualHost(ResolvedVirtualHost resolvedVirtualHost) {
        this.resolvedVirtualHost = resolvedVirtualHost;
    }

    @Override
    public String getResolvedMountPath() {
        return resolvedMountPath;
    }

    public void setResolvedMountPath(String resolvedMountPath) {
        this.resolvedMountPath = resolvedMountPath;
    }

    @Override
    public String getMatchingIgnoredPrefix() {
        return matchingIgnoredPrefix;
    }

    @Override
    public ResolvedSiteMapItem matchSiteMapItem(String siteMapPathInfo) {
        return sitemaps.get(siteMapPathInfo);
    }

    public void setMatchingIgnoredPrefix(String matchingIgnoredPrefix) {
        this.matchingIgnoredPrefix = matchingIgnoredPrefix;
    }

    @Override
    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
}
