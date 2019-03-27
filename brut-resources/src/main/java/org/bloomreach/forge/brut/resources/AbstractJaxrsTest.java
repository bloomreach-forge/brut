package org.bloomreach.forge.brut.resources;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.springframework.mock.web.DelegatingServletInputStream;

public abstract class AbstractJaxrsTest extends AbstractResourceTest {

    private static final int DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE = 1024;

    public void init() {
        setupComponentManager();
    }

    @Override
    public void performValidation() {
        if (hstRequest.getRequestURI() == null || "".equals(hstRequest.getRequestURI())) {
            throw new IllegalStateException("Request URI was missing in hstRequest");
        }
        if (hstRequest.getMethod() == null) {
            throw new IllegalStateException(("Method name was missing hstRequest"));
        }
    }

    protected void setupHstRequest() {
        this.hstRequest = new MockHstRequest();
        hstRequest.setContextPath("/site");
        hstRequest.setHeader("Host", "localhost:8080");
        hstRequest.setHeader("X-Forwarded-Proto", "http");
        hstRequest.setInputStream(new DelegatingServletInputStream(new ByteArrayInputStream(new byte[getServletInputStreamSize()])));
        hstRequest.setScheme("http");
        hstRequest.setServerName("localhost:8080");
    }

    protected void setupComponentManager() {
        this.componentManager = new SpringComponentManager();
        includeAdditionalSpringConfigurations();
        includeAdditionalAddonModules();
        componentManager.initialize();
        HstServices.setComponentManager(componentManager);
    }

    private void includeAdditionalAddonModules() {
        if (contributeAddonModulePaths() != null) {
            List<ModuleDefinition> moduleDefinitions = contributeAddonModulePaths().stream()
                    .map(Utils::loadAddonModule)
                    .collect(Collectors.toList());
            this.componentManager.setAddonModuleDefinitions(moduleDefinitions);
        }
    }

    protected int getServletInputStreamSize() {
        return DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE;
    }
}
