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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main wrapper for PageModel API responses.
 * Provides navigation and search APIs for components and content.
 *
 * <p>Example usage:
 * <pre>
 * PageModelResponse pm = PageModelResponse.parse(jsonString);
 *
 * // Find component by name
 * Optional&lt;PageComponent&gt; banner = pm.findComponentByName("HeroBanner");
 *
 * // Get component's document and convert to POJO
 * HeroBannerData data = banner
 *     .flatMap(pm::getComponentDocument)
 *     .map(doc -> doc.as(HeroBannerData.class))
 *     .orElseThrow();
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageModelResponse {

    private Map<String, PageComponent> page;
    private Map<String, ContentItem> content;
    private ContentRef root;
    private Map<String, Object> channel;
    private Map<String, Object> meta;
    private Map<String, Link> links;

    /**
     * Parses a JSON string into a PageModelResponse.
     *
     * @param json JSON string from PageModel API
     * @return parsed PageModelResponse
     * @throws JsonProcessingException if JSON is invalid
     */
    public static PageModelResponse parse(String json) throws JsonProcessingException {
        return PageModelMapper.INSTANCE.readValue(json, PageModelResponse.class);
    }

    /**
     * Gets the root component of the page.
     *
     * @return root component, or null if not present
     */
    public PageComponent getRootComponent() {
        if (root == null || page == null) {
            return null;
        }
        return page.get(root.getId());
    }

    /**
     * Resolves a ContentRef to its PageComponent.
     *
     * @param ref reference to resolve
     * @return resolved component, or null if not found
     */
    public PageComponent resolveComponent(ContentRef ref) {
        if (ref == null || page == null) {
            return null;
        }
        return page.get(ref.getId());
    }

    /**
     * Resolves a ContentRef to its ContentItem.
     * Handles both v1.0 (page section) and v1.1 (content section) formats.
     *
     * @param ref reference to resolve
     * @return resolved content item, or null if not found
     */
    public ContentItem resolveContent(ContentRef ref) {
        return (ref == null || content == null) ? null : content.get(ref.getId());
    }

    /**
     * Gets all child components of a parent component.
     *
     * @param parent parent component
     * @return list of child components (never null)
     */
    public List<PageComponent> getChildComponents(PageComponent parent) {
        if (parent == null || !parent.isContainer()) {
            return Collections.emptyList();
        }
        return parent.getChildren().stream()
                .map(this::resolveComponent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Finds a component by name, searching the entire component tree.
     *
     * @param name component name to find
     * @return Optional containing the component, or empty if not found
     */
    public Optional<PageComponent> findComponentByName(String name) {
        if (page == null || name == null) {
            return Optional.empty();
        }
        return page.values().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    /**
     * Finds all components matching a given type.
     *
     * @param type component type to match
     * @return list of matching components (never null)
     */
    public List<PageComponent> findComponentsByType(String type) {
        if (page == null || type == null) {
            return Collections.emptyList();
        }
        return page.values().stream()
                .filter(c -> type.equals(c.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Gets the document associated with a component.
     * Resolves the component's document reference to a ContentItem.
     *
     * @param component component to get document for
     * @return Optional containing the document, or empty if not found
     */
    public Optional<ContentItem> getComponentDocument(PageComponent component) {
        if (component == null) {
            return Optional.empty();
        }
        return component.getDocumentRef()
                .map(this::resolveContent);
    }

    /**
     * Checks if this response has content items.
     *
     * @return true if content map is non-empty
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * Gets the total number of components in the page.
     *
     * @return component count
     */
    public int getComponentCount() {
        return page == null ? 0 : page.size();
    }

    /**
     * Gets a channel property value.
     *
     * @param <T> expected type
     * @param key property key
     * @return property value cast to type T, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getChannelProperty(String key) {
        return channel == null ? null : (T) channel.get(key);
    }

    /**
     * Gets a metadata value.
     *
     * @param <T> expected type
     * @param key metadata key
     * @return metadata value cast to type T, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key) {
        return meta == null ? null : (T) meta.get(key);
    }

    public Map<String, PageComponent> getPage() {
        return page;
    }

    public void setPage(Map<String, PageComponent> page) {
        this.page = page;
    }

    public Map<String, ContentItem> getContent() {
        return content;
    }

    public void setContent(Map<String, ContentItem> content) {
        this.content = content;
    }

    public ContentRef getRoot() {
        return root;
    }

    public void setRoot(ContentRef root) {
        this.root = root;
    }

    public Map<String, Object> getChannel() {
        return channel;
    }

    public void setChannel(Map<String, Object> channel) {
        this.channel = channel;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }
}
