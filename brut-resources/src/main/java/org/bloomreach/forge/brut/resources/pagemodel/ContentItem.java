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

import java.util.Map;

/**
 * Represents a content item (document) in PageModel API responses.
 * Provides type-safe access to document fields and conversion to custom POJOs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentItem {

    private String id;
    private String name;
    private String displayName;
    private String localeString;
    private String type;
    private Map<String, Object> data;
    private Map<String, Link> links;

    /**
     * Converts the document data to a custom POJO type.
     * Uses Jackson's convertValue for type-safe mapping.
     *
     * @param <T>  target type
     * @param type target class
     * @return converted object, or null if data is null
     */
    public <T> T as(Class<T> type) {
        return data == null ? null : PageModelMapper.INSTANCE.convertValue(data, type);
    }

    /**
     * Gets a field value from the data map.
     *
     * @param <T> expected type
     * @param key field name
     * @return field value cast to type T, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataField(String key) {
        return data == null ? null : (T) data.get(key);
    }

    /**
     * Gets a nested field value using dot notation.
     * Example: getNestedField("author.name")
     *
     * @param <T>  expected type
     * @param path dot-separated path (e.g., "author.name")
     * @return field value cast to type T, or null if path not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getNestedField(String path) {
        if (data == null || path == null) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return (T) current;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocaleString() {
        return localeString;
    }

    public void setLocaleString(String localeString) {
        this.localeString = localeString;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }
}
