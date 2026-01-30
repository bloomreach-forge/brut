/*
 * Copyright 2024 Bloomreach, Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.brut.resources.annotation;

import org.bloomreach.forge.brut.resources.AbstractPageModelTest;
import org.bloomreach.forge.brut.resources.MockHstRequest;
import org.bloomreach.forge.brut.resources.util.RequestBuilder;
import org.bloomreach.forge.brut.resources.util.RepositorySession;

import javax.jcr.Repository;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic PageModel test implementation that bridges annotation-based API to existing AbstractPageModelTest.
 * This adapter delegates configuration to TestConfig and exposes methods needed by the JUnit extension.
 */
public class DynamicPageModelTest extends AbstractPageModelTest implements DynamicTest {

    private final TestConfig config;

    public DynamicPageModelTest(TestConfig config) {
        this.config = config;
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return String.join("", config.getBeanPatterns());
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        List<String> springConfigs = config.getSpringConfigs();
        return springConfigs != null ? springConfigs : Collections.emptyList();
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return config.getHstRoot();
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return config.getAddonModules();
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public MockHstRequest getHstRequest() {
        return super.getHstRequest();
    }

    @Override
    public String invokeFilter() {
        return super.invokeFilter();
    }

    @Override
    public void setupForNewRequest() {
        super.setupForNewRequest();
    }

    @Override
    public RequestBuilder request() {
        return new RequestBuilder(
                getHstRequest(),
                this::invokeFilter,
                this::getResponseStatus
        );
    }

    private int getResponseStatus() {
        if (hstResponse == null) {
            return 200;
        }
        try {
            Method getStatus = hstResponse.getClass().getMethod("getStatus");
            return (int) getStatus.invoke(hstResponse);
        } catch (Exception e) {
            return 200;
        }
    }

    @Override
    public RepositorySession repository() {
        Repository repo = getComponentManager().getComponent(Repository.class);
        return RepositorySession.forRepository(repo);
    }
}
