package org.bloomreach.forge.brut.resources;

import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

public class SpringComponentManager extends org.hippoecm.hst.site.container.SpringComponentManager {

    private static final String DEFAULT_SPRING_LOCATION_PATTERN = "/org/bloomreach/forge/brut/resources/hst/*.xml";
    private List<String> configurationResources = Arrays.asList(DEFAULT_SPRING_LOCATION_PATTERN);

    @Override
    public String[] getConfigurationResources() {
        return configurationResources.toArray(new String[0]);
    }

    public void setConfigurationResources(final List<String> configurationResources) {
        this.configurationResources = configurationResources;
    }

    /**
     * Returns the internal Spring ApplicationContext created during {@link #initialize()}.
     * <p>
     * Used by BRUT to register a {@code WebApplicationContext} on the test servlet context,
     * enabling customer HST components that use
     * {@code WebApplicationContextUtils.getWebApplicationContext(servletContext)}.
     */
    public ApplicationContext getInternalApplicationContext() {
        return this.applicationContext;
    }
}
