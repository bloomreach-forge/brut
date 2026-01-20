package org.bloomreach.forge.brut.resources;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpSession;
import org.springframework.mock.web.MockHttpSession;

/**
 * Extended mock HST request with HTTP session support.
 * Enables testing of login flows and session-dependent features.
 *
 * @since 5.1.0
 */
public class MockHstRequest extends org.hippoecm.hst.mock.core.component.MockHstRequest {

    private ServletContext servletContext;
    private ServletInputStream inputStream;
    private HttpSession session;

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

    /**
     * Get the HTTP session. Creates a default mock session if none set.
     *
     * @return the HTTP session
     * @since 5.1.0
     */
    @Override
    public HttpSession getSession() {
        if (session == null) {
            session = new MockHttpSession(servletContext);
        }
        return session;
    }

    /**
     * Get the HTTP session. Creates a default mock session if create=true and none exists.
     *
     * @param create if true, creates a new session if one doesn't exist
     * @return the HTTP session, or null if create=false and no session exists
     * @since 5.1.0
     */
    @Override
    public HttpSession getSession(final boolean create) {
        if (session == null && create) {
            session = new MockHttpSession(servletContext);
        }
        return session;
    }

    /**
     * Set the HTTP session for this request.
     *
     * @param session the session to set
     * @since 5.1.0
     */
    public void setSession(final HttpSession session) {
        this.session = session;
    }

}
