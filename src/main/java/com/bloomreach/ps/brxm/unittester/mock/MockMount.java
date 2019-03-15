package com.bloomreach.ps.brxm.unittester.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.onehippo.cms7.services.hst.Channel;

public class MockMount implements ContextualizableMount {

    private VirtualHost virtualHost;
    private Mount parent;
    private String alias = "randomAlias" + UUID.randomUUID().toString();
    private String identifier = "randomIdentifier" + UUID.randomUUID().toString();
    private String name;
    private String namedPipeline;
    private Map<String, Mount> childs = new HashMap<>();
    private String mountPath;
    private ChannelInfo channelInfo;
    private ChannelInfo previewChannelInfo;
    private HstSite previewHstSite;
    private String previewChannelPath;
    private Channel previewChannel;
    private boolean mapped;
    private String mountPoint;
    private String contentPath;
    private String canonicalContentPath;
    private Map<String, Mount> childMounts = new HashMap<>();
    private HstSite hstSite;
    private boolean contextPathInUrl;
    private boolean portInUrl;
    private boolean site;
    private int port;
    private String onlyForContextPath;
    private String contextPath;
    private String homePage;
    private String pageNotFound;
    private String scheme;
    private boolean schemeAgnostic;
    private boolean containsMultipleSchemes;
    private int schemeNotMatchingResponseCode;
    private boolean preview;
    private boolean versionInPreviewHeader;
    private String locale;
    private HstSiteMapMatcher hstSiteMapMatcher;
    private boolean authenticated;
    private Set<String> roles = new HashSet<>();
    private Set<String> users = new HashSet<>();
    private boolean subjectBasedSession;
    private boolean sessionStateful;
    private String formLoginPage;
    private Map<String, String> mountProperties = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();
    private String channelPath;
    private Channel channel;
    private String[] defaultSiteMapItemHandlerIds;
    private boolean cacheable;
    private String defaultResourceBundleId;
    private String[] defaultResourceBundleIds;
    private String cmsLocation;
    private List<String> cmsLocations;
    private String type;
    private List<String> types;
    private boolean noChannelInfo;
    private boolean finalPipeline;
    private boolean explicit;
    private Map<String, String> responseHeaders = new HashMap<>();

    @Override
    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(VirtualHost virtualHost) {
        this.virtualHost = virtualHost;
    }

    @Override
    public Mount getParent() {
        return parent;
    }

    public void setParent(Mount parent) {
        this.parent = parent;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }


    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getNamedPipeline() {
        return namedPipeline;
    }

    @Override
    public boolean isFinalPipeline() {
        return finalPipeline;
    }

    public void setNamedPipeline(String namedPipeline) {
        this.namedPipeline = namedPipeline;
    }

    public Map<String, Mount> getChilds() {
        return childs;
    }

    public void setChilds(Map<String, Mount> childs) {
        this.childs = childs;
    }

    @Override
    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo() {
        return (T) channelInfo;
    }

    @Override
    public <T extends ChannelInfo> T getPreviewChannelInfo() {
        return (T) previewChannelInfo;
    }

    public void setPreviewChannelInfo(ChannelInfo previewChannelInfo) {
        this.previewChannelInfo = previewChannelInfo;
    }

    @Override
    public HstSite getPreviewHstSite() {
        return previewHstSite;
    }

    public void setPreviewHstSite(HstSite previewHstSite) {
        this.previewHstSite = previewHstSite;
    }

    @Override
    public String getPreviewChannelPath() {
        return previewChannelPath;
    }

    public void setPreviewChannelPath(String previewChannelPath) {
        this.previewChannelPath = previewChannelPath;
    }


    @Override
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String[] getDefaultSiteMapItemHandlerIds() {
        return defaultSiteMapItemHandlerIds;
    }

    public void setDefaultSiteMapItemHandlerIds(String[] defaultSiteMapItemHandlerIds) {
        this.defaultSiteMapItemHandlerIds = defaultSiteMapItemHandlerIds;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    @Override
    public String getDefaultResourceBundleId() {
        return defaultResourceBundleId;
    }

    public void setDefaultResourceBundleId(String defaultResourceBundleId) {
        this.defaultResourceBundleId = defaultResourceBundleId;
    }

    @Override
    public String[] getDefaultResourceBundleIds() {
        return defaultResourceBundleIds;
    }

    public void setDefaultResourceBundleIds(String[] defaultResourceBundleIds) {
        this.defaultResourceBundleIds = defaultResourceBundleIds;
    }

    @Override
    public String getCmsLocation() {
        return cmsLocation;
    }

    public void setCmsLocation(String cmsLocation) {
        this.cmsLocation = cmsLocation;
    }

    @Override
    public List<String> getCmsLocations() {
        return cmsLocations;
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public boolean isExplicit() {
        return explicit;
    }

    public void setCmsLocations(List<String> cmsLocations) {
        this.cmsLocations = cmsLocations;
    }

    @Override
    public Channel getPreviewChannel() {
        return previewChannel;
    }

    public void setPreviewChannel(Channel previewChannel) {
        this.previewChannel = previewChannel;
    }

    @Override
    public boolean isMapped() {
        return mapped;
    }

    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }

    @Override
    public String getMountPoint() {
        return mountPoint;
    }

    @Override
    public boolean hasNoChannelInfo() {
        return noChannelInfo;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    @Override
    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    @Override
    public String getCanonicalContentPath() {
        return canonicalContentPath;
    }

    public void setCanonicalContentPath(String canonicalContentPath) {
        this.canonicalContentPath = canonicalContentPath;
    }

    @Override
    public List<Mount> getChildMounts() {
        List<Mount> result = new ArrayList<>();
        result.addAll(childMounts.values());
        return result;
    }

    @Override
    public Mount getChildMount(String name) {
        return childMounts.get(name);
    }

    public void setChildMounts(Map<String, Mount> childMounts) {
        this.childMounts = childMounts;
    }

    @Override
    public HstSite getHstSite() {
        return hstSite;
    }

    public void setHstSite(HstSite hstSite) {
        this.hstSite = hstSite;
    }


    @Override
    public boolean isPortInUrl() {
        return portInUrl;
    }

    public void setPortInUrl(boolean portInUrl) {
        this.portInUrl = portInUrl;
    }

    @Override
    public boolean isSite() {
        return site;
    }

    public void setSite(boolean site) {
        this.site = site;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String onlyForContextPath() {
        return onlyForContextPath;
    }

    public void setOnlyForContextPath(String onlyForContextPath) {
        this.onlyForContextPath = onlyForContextPath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    @Override
    public String getPageNotFound() {
        return pageNotFound;
    }

    public void setPageNotFound(String pageNotFound) {
        this.pageNotFound = pageNotFound;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public boolean isSchemeAgnostic() {
        return schemeAgnostic;
    }

    public void setSchemeAgnostic(boolean schemeAgnostic) {
        this.schemeAgnostic = schemeAgnostic;
    }

    @Override
    public boolean containsMultipleSchemes() {
        return containsMultipleSchemes;
    }

    public void setContainsMultipleSchemes(boolean containsMultipleSchemes) {
        this.containsMultipleSchemes = containsMultipleSchemes;
    }

    @Override
    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    public void setSchemeNotMatchingResponseCode(int schemeNotMatchingResponseCode) {
        this.schemeNotMatchingResponseCode = schemeNotMatchingResponseCode;
    }

    @Override
    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    @Override
    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    public void setVersionInPreviewHeader(boolean versionInPreviewHeader) {
        this.versionInPreviewHeader = versionInPreviewHeader;
    }

    @Override
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return hstSiteMapMatcher;
    }

    public void setHstSiteMapMatcher(HstSiteMapMatcher hstSiteMapMatcher) {
        this.hstSiteMapMatcher = hstSiteMapMatcher;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    @Override
    public boolean isSubjectBasedSession() {
        return subjectBasedSession;
    }

    public void setSubjectBasedSession(boolean subjectBasedSession) {
        this.subjectBasedSession = subjectBasedSession;
    }

    @Override
    public boolean isSessionStateful() {
        return sessionStateful;
    }

    public void setSessionStateful(boolean sessionStateful) {
        this.sessionStateful = sessionStateful;
    }

    @Override
    public String getFormLoginPage() {
        return formLoginPage;
    }

    public void setFormLoginPage(String formLoginPage) {
        this.formLoginPage = formLoginPage;
    }

    @Override
    public String getProperty(String name) {
        return mountProperties.get(name);
    }

    @Override
    public List<String> getPropertyNames() {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(mountProperties.keySet());
        return result;
    }

    @Override
    public Map<String, String> getMountProperties() {
        return mountProperties;
    }

    public void addMountProperties(String name, String value) {
        this.mountProperties.put(name, value);
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    public void addParameters(String name, String value) {
        this.parameters.put(name, value);
    }

    @Override
    public String getChannelPath() {
        return channelPath;
    }

    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    public void setChannelInfo(ChannelInfo info, ChannelInfo previewInfo) {
        this.channelInfo = info;
        this.previewChannelInfo = previewInfo;
    }

    public void setChannel(Channel channel, Channel previewChannel) {
        this.channel = channel;
        this.previewChannel = previewChannel;
    }

    @Override
    public void addMount(MutableMount mount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public void setContextPathInUrl(boolean contextPathInUrl) {
        this.contextPathInUrl = contextPathInUrl;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    @Override
    public boolean isOfType(String type) {
        return types != null ? types.contains(type) : false;
    }


    public void setNoChannelInfo(final boolean noChannelInfo) {
        this.noChannelInfo = noChannelInfo;
    }

    public void setFinalPipeline(final boolean finalPipeline) {
        this.finalPipeline = finalPipeline;
    }

    public void setExplicit(final boolean explicit) {
        this.explicit = explicit;
    }

    public void setResponseHeaders(final Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
}
