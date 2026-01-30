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
package org.bloomreach.forge.brut.resources.pagemodel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PageModelResponseTest {

    private static final String SAMPLE_PAGE_MODEL = """
            {
              "root": { "$ref": "/page/root" },
              "page": {
                "root": {
                  "id": "root",
                  "name": "root",
                  "type": "container",
                  "children": [
                    { "$ref": "/page/header" },
                    { "$ref": "/page/main" }
                  ]
                },
                "header": {
                  "id": "header",
                  "name": "header",
                  "type": "container",
                  "children": []
                },
                "main": {
                  "id": "main",
                  "name": "main",
                  "type": "container",
                  "children": [
                    { "$ref": "/page/hero-banner" }
                  ]
                },
                "hero-banner": {
                  "id": "hero-banner",
                  "name": "HeroBanner",
                  "label": "Hero Banner",
                  "type": "component",
                  "componentClass": "org.example.HeroBannerComponent",
                  "models": {
                    "document": { "$ref": "/content/banner-doc" }
                  }
                }
              },
              "content": {
                "banner-doc": {
                  "id": "banner-doc",
                  "name": "hero-banner-document",
                  "displayName": "Hero Banner Document",
                  "type": "myproject:banner",
                  "data": {
                    "title": "Welcome to Our Site",
                    "subtitle": "Discover Amazing Content",
                    "cta": {
                      "label": "Learn More",
                      "url": "/about"
                    }
                  }
                }
              },
              "channel": {
                "name": "test-channel"
              },
              "meta": {
                "version": "1.1"
              }
            }
            """;

    @Test
    void parse_validJson_returnsPageModelResponse() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        assertNotNull(response);
        assertNotNull(response.getPage());
        assertNotNull(response.getContent());
        assertNotNull(response.getRoot());
        assertEquals(4, response.getComponentCount());
    }

    @Test
    void getRootComponent_returnsRootFromPage() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        PageComponent root = response.getRootComponent();

        assertNotNull(root);
        assertEquals("root", root.getId());
        assertEquals("root", root.getName());
        assertTrue(root.isContainer());
    }

    @Test
    void findComponentByName_existingComponent_returnsComponent() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        Optional<PageComponent> banner = response.findComponentByName("HeroBanner");

        assertTrue(banner.isPresent());
        assertEquals("hero-banner", banner.get().getId());
        assertEquals("HeroBanner", banner.get().getName());
        assertEquals("Hero Banner", banner.get().getLabel());
        assertEquals("org.example.HeroBannerComponent", banner.get().getComponentClass());
    }

    @Test
    void findComponentByName_nonExistent_returnsEmpty() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        Optional<PageComponent> result = response.findComponentByName("NonExistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void findComponentsByType_matchingComponents_returnsList() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        List<PageComponent> containers = response.findComponentsByType("container");

        assertEquals(3, containers.size());
    }

    @Test
    void getComponentDocument_componentWithDocument_returnsContentItem() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        PageComponent banner = response.findComponentByName("HeroBanner").orElseThrow();

        Optional<ContentItem> document = response.getComponentDocument(banner);

        assertTrue(document.isPresent());
        assertEquals("banner-doc", document.get().getId());
        assertEquals("hero-banner-document", document.get().getName());
        assertEquals("myproject:banner", document.get().getType());
    }

    @Test
    void getChildComponents_containerComponent_returnsChildren() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        PageComponent root = response.getRootComponent();

        List<PageComponent> children = response.getChildComponents(root);

        assertEquals(2, children.size());
        assertEquals("header", children.get(0).getName());
        assertEquals("main", children.get(1).getName());
    }

    @Test
    void contentItem_as_convertsToCustomPojo() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        ContentItem document = response.getContent().get("banner-doc");

        BannerData bannerData = document.as(BannerData.class);

        assertNotNull(bannerData);
        assertEquals("Welcome to Our Site", bannerData.getTitle());
        assertEquals("Discover Amazing Content", bannerData.getSubtitle());
    }

    @Test
    void contentItem_getDataField_returnsFieldValue() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        ContentItem document = response.getContent().get("banner-doc");

        String title = document.getDataField("title");

        assertEquals("Welcome to Our Site", title);
    }

    @Test
    void contentItem_getNestedField_returnsNestedValue() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        ContentItem document = response.getContent().get("banner-doc");

        String ctaLabel = document.getNestedField("cta.label");
        String ctaUrl = document.getNestedField("cta.url");

        assertEquals("Learn More", ctaLabel);
        assertEquals("/about", ctaUrl);
    }

    @Test
    void pageComponent_getDocumentRef_returnsRef() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);
        PageComponent banner = response.findComponentByName("HeroBanner").orElseThrow();

        Optional<ContentRef> docRef = banner.getDocumentRef();

        assertTrue(docRef.isPresent());
        assertEquals("/content/banner-doc", docRef.get().getRef());
        assertEquals("banner-doc", docRef.get().getId());
        assertEquals("content", docRef.get().getSection());
    }

    @Test
    void contentRef_getId_extractsIdFromPath() {
        ContentRef ref = new ContentRef();
        ref.setRef("/page/component-id");

        assertEquals("component-id", ref.getId());
    }

    @Test
    void contentRef_getSection_extractsSectionFromPath() {
        ContentRef ref = new ContentRef();
        ref.setRef("/content/doc-id");

        assertEquals("content", ref.getSection());
    }

    @Test
    void hasContent_withContent_returnsTrue() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        assertTrue(response.hasContent());
    }

    @Test
    void getChannelProperty_existingProperty_returnsValue() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        String channelName = response.getChannelProperty("name");

        assertEquals("test-channel", channelName);
    }

    @Test
    void getMeta_existingKey_returnsValue() throws Exception {
        PageModelResponse response = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        String version = response.getMeta("version");

        assertEquals("1.1", version);
    }

    @Test
    void fullWorkflow_findComponentAndConvertDocument() throws Exception {
        PageModelResponse pm = PageModelResponse.parse(SAMPLE_PAGE_MODEL);

        // Simulates the usage pattern from the plan
        BannerData banner = pm.findComponentByName("HeroBanner")
                .flatMap(pm::getComponentDocument)
                .map(doc -> doc.as(BannerData.class))
                .orElseThrow();

        assertEquals("Welcome to Our Site", banner.getTitle());
        assertEquals("Discover Amazing Content", banner.getSubtitle());
    }

    /**
     * Test POJO for banner document conversion.
     */
    public static class BannerData {
        private String title;
        private String subtitle;
        private CtaData cta;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public CtaData getCta() {
            return cta;
        }

        public void setCta(CtaData cta) {
            this.cta = cta;
        }
    }

    public static class CtaData {
        private String label;
        private String url;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
