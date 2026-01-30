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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON reference ($ref) in PageModel API responses.
 * Used for component children and document references.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentRef {

    @JsonProperty("$ref")
    private String ref;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Extracts the ID portion from the reference path.
     * For "/page/component-id" returns "component-id".
     * For "/content/doc-id" returns "doc-id".
     *
     * @return the ID portion, or null if ref is null
     */
    public String getId() {
        return ref == null ? null : ref.substring(ref.lastIndexOf('/') + 1);
    }

    /**
     * Returns the section prefix from the reference path.
     * For "/page/component-id" returns "page".
     * For "/content/doc-id" returns "content".
     *
     * @return the section name, or null if ref is null or malformed
     */
    public String getSection() {
        if (ref == null || ref.length() < 2) {
            return null;
        }
        int start = ref.startsWith("/") ? 1 : 0;
        int end = ref.indexOf('/', start);
        return end > start ? ref.substring(start, end) : null;
    }

    @Override
    public String toString() {
        return ref;
    }
}
