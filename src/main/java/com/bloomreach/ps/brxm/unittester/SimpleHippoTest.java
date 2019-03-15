package com.bloomreach.ps.brxm.unittester;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.container.MockContainerConfiguration;
import org.hippoecm.hst.mock.core.request.MockComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.request.MockResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;

import com.bloomreach.ps.brxm.unittester.exception.SetupTeardownException;
import com.bloomreach.ps.brxm.unittester.mock.DelegatingComponentManager;
import com.bloomreach.ps.brxm.unittester.mock.MockComponentManager;
import com.bloomreach.ps.brxm.unittester.mock.MockHstLinkCreator;
import com.bloomreach.ps.brxm.unittester.mock.MockMount;
import com.bloomreach.ps.brxm.unittester.mock.MockResolvedMount;

import static org.hippoecm.hst.utils.ParameterUtils.PARAMETERS_INFO_ATTRIBUTE;

//TODO release under com.bloomreach.ps

public class SimpleHippoTest {

    public static final String COMPONENT_REFERENCE_NAMESPACE = "r1_r2";

    private static DelegatingComponentManager delegatingComponentManager = new DelegatingComponentManager();

    protected MockHstResponse response = new MockHstResponse();
    protected MockHstRequest request = new MockHstRequest();
    protected MockHstRequestContext requestContext = new MockHstRequestContext();
    protected MockResolvedSiteMapItem resolvedSiteMapItem;
    protected MockResolvedMount resolvedMount;
    protected MockMount mount;
    protected MockHstLinkCreator hstLinkCreator = new MockHstLinkCreator();
    protected MockComponentConfiguration componentConfiguration = new MockComponentConfiguration();
    protected MockComponentManager componentManager = new MockComponentManager();
    protected MockContainerConfiguration containerConfiguration = componentManager.getContainerConfiguration();

    static {
        HstServices.setComponentManager(delegatingComponentManager);
    }


    public void teardown() {
        try {
            clearRequestContextProvider();
            delegatingComponentManager.remove();
        } catch (Exception e) {
            throw new SetupTeardownException(e);
        }
    }

    public void setup() {
        try {
            setupParameterAndAttributeMaps();

            initializedRequest();
            setRequestContextProvider();
            setResolvedSiteMapItem();
            setMount();
            request.setRequestContext(requestContext);
            setComponentManager(componentManager);
            setHstLinkCreator(hstLinkCreator);
        } catch (Exception e) {
            throw new SetupTeardownException(e);
        }
    }

    public static void setComponentManager(ComponentManager componentManager) {
        delegatingComponentManager.setComponentManager(componentManager);
    }

    protected String getPathToTestResource() {
        return "/skeleton.yaml";
    }

    protected String getComponentReferenceNamespace() {
        return COMPONENT_REFERENCE_NAMESPACE;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getRequestAttribute(String name) {
        return (T) request.getAttribute(name);
    }

    protected void addPublicRequestParameter(String name, String value) {
        Map<String, String[]> namespaceLessParameters = request.getParameterMap("");
        namespaceLessParameters.put(name, new String[]{value});
    }

    protected void addPublicRequestParameter(String name, String[] value) {
        Map<String, String[]> namespaceLessParameters = request.getParameterMap("");
        namespaceLessParameters.put(name, value);
    }

    protected void setChannelInfo(ChannelInfo channelInfo) {
        this.mount.setChannelInfo(channelInfo);
    }

    protected void setComponentParameterInfo(Object parameterInfo) {
        this.request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parameterInfo);
    }

    private void setupParameterAndAttributeMaps() {
        request.setAttributeMap("", new HashMap<>());
        request.setAttributeMap(COMPONENT_REFERENCE_NAMESPACE, new HashMap<>());
        request.setParameterMap("", new HashMap<>());
        request.setParameterMap(COMPONENT_REFERENCE_NAMESPACE, new HashMap<>());
    }

    private void initializedRequest() {
        request.setReferencePath(getComponentReferenceNamespace());
    }

    private void setMount() {
        this.mount = new MockMount();
        this.resolvedMount = new MockResolvedMount();
        this.resolvedMount.setMount(this.mount);
        this.requestContext.setResolvedMount(this.resolvedMount);
    }

    private void setResolvedSiteMapItem() {
        resolvedSiteMapItem = new MockResolvedSiteMapItem();
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    }

    private void setRequestContextProvider() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method set = RequestContextProvider.class.getDeclaredMethod("set", HstRequestContext.class);
        set.setAccessible(true);
        set.invoke(null, requestContext);
        set.setAccessible(false);
    }

    private void clearRequestContextProvider() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method set = RequestContextProvider.class.getDeclaredMethod("clear");
        set.setAccessible(true);
        set.invoke(null);
        set.setAccessible(false);
    }

    private void setHstLinkCreator(HstLinkCreator hstLinkCreator) {
        requestContext.setHstLinkCreator(hstLinkCreator);
    }

}
