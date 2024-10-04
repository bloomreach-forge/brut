package org.bloomreach.forge.brut.resources;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;

public class MockHstRequest extends org.hippoecm.hst.mock.core.component.MockHstRequest {

    private ServletContext servletContext;
    private ServletInputStream inputStream;

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

}
