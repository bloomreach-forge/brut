package org.bloomreach.forge.brut.resources;

import java.util.Arrays;
import java.util.List;

public class SpringComponentManager extends org.hippoecm.hst.site.container.SpringComponentManager {

    private static final String DEFAULT_SPRING_LOCATION_PATTERN = "/org/bloomreach/forge/brut/resources/hst/*.xml";
    private List<String> configurationResources = Arrays.asList(DEFAULT_SPRING_LOCATION_PATTERN);

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public String[] getConfigurationResources() {
        return configurationResources.toArray(new String[0]);
    }

    public void setConfigurationResources(final List<String> configurationResources) {
        this.configurationResources = configurationResources;
    }
}
