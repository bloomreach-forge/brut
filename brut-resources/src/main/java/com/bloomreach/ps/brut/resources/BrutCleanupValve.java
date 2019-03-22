package com.bloomreach.ps.brut.resources;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.AbstractBaseOrderableValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrutCleanupValve extends AbstractBaseOrderableValve {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrutCleanupValve.class);

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContext rc = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        try {
            Session session = rc.getSession();
            if (session != null && session.isLive()) {
                session.logout();
            }
        } catch (RepositoryException ex) {
            LOGGER.warn("Exception in BrutCleanupValve: {}", ex.getLocalizedMessage());
        }
        // continue
        context.invokeNext();
    }
}
