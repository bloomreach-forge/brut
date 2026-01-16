package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Example POJO for type-safe PageModel API response deserialization.
 *
 * Using POJOs provides:
 * - Compile-time type safety
 * - Better IDE autocomplete
 * - Clear contract definition
 * - Easier refactoring
 *
 * Usage:
 * <pre>
 * String response = brxm.request()
 *     .get("/site/resourceapi/news")
 *     .execute();
 *
 * PageModelResponse model = mapper.readValue(response, PageModelResponse.class);
 * assertThat(model.getPage()).isNotEmpty();
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageModelResponse {

    private Map<String, ComponentModel> page;
    private Map<String, Object> meta;

    public Map<String, ComponentModel> getPage() {
        return page;
    }

    public void setPage(Map<String, ComponentModel> page) {
        this.page = page;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    /**
     * Example component model structure.
     * Extend this based on your actual PageModel API response structure.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentModel {
        private String id;
        private String type;
        private String name;
        private Map<String, Object> meta;
        private Map<String, Object> models;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public Map<String, Object> getModels() {
            return models;
        }

        public void setModels(Map<String, Object> models) {
            this.models = models;
        }
    }
}
