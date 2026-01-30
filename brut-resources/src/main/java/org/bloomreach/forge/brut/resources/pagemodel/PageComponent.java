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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a component in PageModel API responses.
 * Provides access to component properties, models, and child references.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageComponent {

    private String id;
    private String name;
    private String label;
    private String componentClass;
    private String type;
    private List<ContentRef> children;
    private Map<String, Object> models;
    private Map<String, Object> meta;
    private Map<String, Link> links;

    /**
     * Checks if this component has a model with the given name.
     *
     * @param modelName model name to check
     * @return true if the model exists
     */
    public boolean hasModel(String modelName) {
        return models != null && models.containsKey(modelName);
    }

    /**
     * Gets a model value by name.
     *
     * @param <T>       expected type
     * @param modelName model name
     * @return model value cast to type T, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getModel(String modelName) {
        return models == null ? null : (T) models.get(modelName);
    }

    /**
     * Converts a model to a custom POJO type.
     *
     * @param <T>       target type
     * @param modelName model name
     * @param type      target class
     * @return converted object, or null if model not present
     */
    public <T> T getModelAs(String modelName, Class<T> type) {
        Object model = getModel(modelName);
        return model == null ? null : PageModelMapper.INSTANCE.convertValue(model, type);
    }

    /**
     * Gets the document reference from the models map.
     * Looks for a "document" model that contains a $ref.
     *
     * @return Optional containing the document ref, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public Optional<ContentRef> getDocumentRef() {
        if (models == null) {
            return Optional.empty();
        }
        Object docModel = models.get("document");
        if (docModel instanceof Map) {
            Map<String, Object> docMap = (Map<String, Object>) docModel;
            Object refValue = docMap.get("$ref");
            if (refValue instanceof String) {
                ContentRef ref = new ContentRef();
                ref.setRef((String) refValue);
                return Optional.of(ref);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if this component is a container (has children).
     *
     * @return true if component has child components
     */
    public boolean isContainer() {
        return children != null && !children.isEmpty();
    }

    /**
     * Gets a metadata value by key.
     *
     * @param <T> expected type
     * @param key metadata key
     * @return metadata value cast to type T, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key) {
        return meta == null ? null : (T) meta.get(key);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComponentClass() {
        return componentClass;
    }

    public void setComponentClass(String componentClass) {
        this.componentClass = componentClass;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ContentRef> getChildren() {
        return children == null ? Collections.emptyList() : children;
    }

    public void setChildren(List<ContentRef> children) {
        this.children = children;
    }

    public Map<String, Object> getModels() {
        return models;
    }

    public void setModels(Map<String, Object> models) {
        this.models = models;
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
