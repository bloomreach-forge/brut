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
package org.bloomreach.forge.brut.resources.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bloomreach.forge.brut.resources.MockHstRequest;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fluent builder for setting up and executing HST requests in tests.
 * Reduces request setup boilerplate from 5+ lines to a single fluent chain.
 *
 * <p>Example usage:
 * <pre>
 * brxm.request()
 *     .get("/site/api/hello/user")
 *     .withAccept(MediaType.APPLICATION_JSON)
 *     .assertBody("Hello, World! user");
 * </pre>
 */
public class RequestBuilder {

    private final MockHstRequest hstRequest;
    private final RequestExecutor executor;
    private final Map<String, String> queryParams = new LinkedHashMap<>();

    /**
     * Internal interface for executing requests - supports both JAX-RS and PageModel tests.
     */
    @FunctionalInterface
    public interface RequestExecutor {
        String invokeFilter();
    }

    public RequestBuilder(MockHstRequest hstRequest, RequestExecutor executor) {
        this.hstRequest = hstRequest;
        this.executor = executor;
    }

    /**
     * Sets up a GET request to the specified URI.
     *
     * @param uri request URI (e.g., "/site/api/hello/user")
     * @return this builder for chaining
     */
    public RequestBuilder get(String uri) {
        hstRequest.setRequestURI(uri);
        hstRequest.setMethod(HttpMethod.GET);
        return this;
    }

    /**
     * Sets up a POST request to the specified URI.
     *
     * @param uri request URI
     * @return this builder for chaining
     */
    public RequestBuilder post(String uri) {
        hstRequest.setRequestURI(uri);
        hstRequest.setMethod(HttpMethod.POST);
        return this;
    }

    /**
     * Sets up a PUT request to the specified URI.
     *
     * @param uri request URI
     * @return this builder for chaining
     */
    public RequestBuilder put(String uri) {
        hstRequest.setRequestURI(uri);
        hstRequest.setMethod(HttpMethod.PUT);
        return this;
    }

    /**
     * Sets up a DELETE request to the specified URI.
     *
     * @param uri request URI
     * @return this builder for chaining
     */
    public RequestBuilder delete(String uri) {
        hstRequest.setRequestURI(uri);
        hstRequest.setMethod(HttpMethod.DELETE);
        return this;
    }

    /**
     * Sets the Accept header (default: application/json for JAX-RS).
     *
     * @param mediaType media type (use MediaType.APPLICATION_JSON, etc.)
     * @return this builder for chaining
     */
    public RequestBuilder withAccept(String mediaType) {
        hstRequest.setHeader(HttpHeaders.ACCEPT, mediaType);
        return this;
    }

    /**
     * Sets a custom header.
     *
     * @param name header name
     * @param value header value
     * @return this builder for chaining
     */
    public RequestBuilder withHeader(String name, String value) {
        hstRequest.setHeader(name, value);
        return this;
    }

    /**
     * Adds a query parameter. Multiple calls accumulate parameters.
     *
     * @param name parameter name
     * @param value parameter value
     * @return this builder for chaining
     */
    public RequestBuilder queryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    /**
     * Sets up the request as an authenticated user with specified roles.
     * Sets both remoteUser and userPrincipal for maximum compatibility.
     *
     * @param username the authenticated username
     * @param roles roles assigned to the user
     * @return this builder for chaining
     */
    public RequestBuilder asUser(String username, String... roles) {
        hstRequest.setRemoteUser(username);
        hstRequest.setUserPrincipal(() -> username);
        if (roles.length > 0) {
            hstRequest.setUserRoleNames(new LinkedHashSet<>(Arrays.asList(roles)));
        }
        return this;
    }

    /**
     * Sets up roles for the request without specifying a username.
     * Useful for testing role-based access without full user context.
     *
     * @param roles roles to assign
     * @return this builder for chaining
     */
    public RequestBuilder withRole(String... roles) {
        hstRequest.setUserRoleNames(new LinkedHashSet<>(Arrays.asList(roles)));
        return this;
    }

    /**
     * Convenience method for PageModel API component-rendering requests.
     * Sets _hn:type=component-rendering and _hn:ref={ref}.
     *
     * @param ref component reference (e.g., "r5_r1_r1")
     * @return this builder for chaining
     */
    public RequestBuilder pageModelComponentRendering(String ref) {
        return queryParam("_hn:type", "component-rendering")
               .queryParam("_hn:ref", ref);
    }

    /**
     * Executes the request and returns the response body.
     *
     * @return response body as string
     */
    public String execute() {
        applyQueryParams();
        return executor.invokeFilter();
    }

    /**
     * Executes the request and parses the response as a PageModelResponse.
     * Convenience method for PageModel API requests.
     *
     * @return parsed PageModelResponse
     * @throws JsonProcessingException if JSON parsing fails
     */
    public PageModelResponse executeAsPageModel() throws JsonProcessingException {
        String json = execute();
        return PageModelResponse.parse(json);
    }

    /**
     * Executes the request and asserts the response body equals expected value.
     *
     * @param expected expected response body
     * @return this builder for chaining (allows further assertions)
     */
    public RequestBuilder assertBody(String expected) {
        String response = execute();
        assertEquals(expected, response, "Response body mismatch");
        return this;
    }

    private void applyQueryParams() {
        if (!queryParams.isEmpty()) {
            String queryString = queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            hstRequest.setQueryString(queryString);
        }
    }
}
