package org.bloomreach.forge.brut.resources;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpSession;

import org.springframework.mock.web.MockHttpSession;

public class MockHstRequest extends org.hippoecm.hst.mock.core.component.MockHstRequest {

    private ServletContext servletContext;
    private ServletInputStream inputStream;
    private MockHttpSession session;

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public ServletInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(final ServletInputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (session == null && create) {
            session = new MockHttpSession(servletContext);
        }
        return session;
    }

    public void setSession(MockHttpSession session) {
        this.session = session;
    }

    public void invalidateSession() {
        if (session != null) {
            session.invalidate();
            session = null;
        }
    }

}
