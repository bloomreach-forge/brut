package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.AbstractBaseOrderableValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * Valve that resolves and sets the site content base bean on the HstRequestContext.
 * Must run after contextResolvingValve (when mount is resolved) but before
 * componentRenderingValve (when components access getSiteContentBaseBean()).
 */
public class SiteContentBaseResolverValve extends AbstractBaseOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(SiteContentBaseResolverValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest request = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext)
            request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

        if (requestContext != null && requestContext.getSiteContentBaseBean() == null) {
            resolveSiteContentBaseBean(requestContext);
        }

        context.invokeNext();
    }

    private void resolveSiteContentBaseBean(HstRequestContext requestContext) {
        try {
            ResolvedMount resolvedMount = requestContext.getResolvedMount();
            if (resolvedMount == null || resolvedMount.getMount() == null) {
                return;
            }

            String contentPath = resolvedMount.getMount().getContentPath();
            if (contentPath == null || contentPath.isBlank()) {
                LOG.debug("No content path configured on mount");
                return;
            }

            String absolutePath = contentPath.startsWith("/") ? contentPath : "/" + contentPath;

            ObjectBeanManager obm = requestContext.getObjectBeanManager();
            if (obm == null) {
                LOG.warn("ObjectBeanManager not available");
                return;
            }

            HippoBean siteContentBaseBean = (HippoBean) obm.getObject(absolutePath);
            if (siteContentBaseBean == null) {
                LOG.warn("Could not resolve site content base bean at: {}", absolutePath);
                return;
            }

            setSiteContentBaseBeanViaReflection(requestContext, siteContentBaseBean, contentPath);
            LOG.debug("Resolved site content base bean at: {}", absolutePath);
        } catch (Exception e) {
            LOG.warn("Failed to resolve site content base bean", e);
        }
    }

    private void setSiteContentBaseBeanViaReflection(HstRequestContext ctx, HippoBean bean, String path) {
        try {
            Method setter = findMethod(ctx.getClass(), "setSiteContentBaseBean", HippoBean.class);
            if (setter != null) {
                setter.setAccessible(true);
                setter.invoke(ctx, bean);
            }

            String relativePath = path.startsWith("/") ? path.substring(1) : path;
            Method pathSetter = findMethod(ctx.getClass(), "setSiteContentBasePath", String.class);
            if (pathSetter != null) {
                pathSetter.setAccessible(true);
                pathSetter.invoke(ctx, relativePath);
            }
        } catch (Exception e) {
            LOG.warn("Failed to set site content base bean via reflection", e);
        }
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
