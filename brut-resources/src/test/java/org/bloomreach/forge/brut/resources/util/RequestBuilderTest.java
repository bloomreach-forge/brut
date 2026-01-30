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

import org.bloomreach.forge.brut.resources.MockHstRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RequestBuilder fluent API.
 */
class RequestBuilderTest {

    private MockHstRequest mockRequest;
    private RequestBuilder builder;
    private String executedResponse;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHstRequest();
        executedResponse = "test response";
        builder = new RequestBuilder(mockRequest, () -> executedResponse);
    }

    @Test
    @DisplayName("get() sets method to GET and URI")
    void testGet() {
        builder.get("/site/api/test");

        assertEquals(HttpMethod.GET, mockRequest.getMethod());
        assertEquals("/site/api/test", mockRequest.getRequestURI());
    }

    @Test
    @DisplayName("post() sets method to POST and URI")
    void testPost() {
        builder.post("/site/api/test");

        assertEquals(HttpMethod.POST, mockRequest.getMethod());
        assertEquals("/site/api/test", mockRequest.getRequestURI());
    }

    @Test
    @DisplayName("put() sets method to PUT and URI")
    void testPut() {
        builder.put("/site/api/test");

        assertEquals(HttpMethod.PUT, mockRequest.getMethod());
        assertEquals("/site/api/test", mockRequest.getRequestURI());
    }

    @Test
    @DisplayName("delete() sets method to DELETE and URI")
    void testDelete() {
        builder.delete("/site/api/test");

        assertEquals(HttpMethod.DELETE, mockRequest.getMethod());
        assertEquals("/site/api/test", mockRequest.getRequestURI());
    }

    @Test
    @DisplayName("withAccept() sets Accept header")
    void testWithAccept() {
        builder.get("/site/api/test")
               .withAccept(MediaType.APPLICATION_JSON);

        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.ACCEPT));
    }

    @Test
    @DisplayName("withHeader() sets custom header")
    void testWithHeader() {
        builder.get("/site/api/test")
               .withHeader("X-Custom-Header", "custom-value");

        assertEquals("custom-value", mockRequest.getHeader("X-Custom-Header"));
    }

    @Test
    @DisplayName("queryParam() builds query string with single parameter")
    void testSingleQueryParam() {
        builder.get("/site/api/test")
               .queryParam("name", "value")
               .execute();

        assertEquals("name=value", mockRequest.getQueryString());
    }

    @Test
    @DisplayName("queryParam() builds query string with multiple parameters")
    void testMultipleQueryParams() {
        builder.get("/site/api/test")
               .queryParam("first", "value1")
               .queryParam("second", "value2")
               .queryParam("third", "value3")
               .execute();

        assertEquals("first=value1&second=value2&third=value3", mockRequest.getQueryString());
    }

    @Test
    @DisplayName("pageModelComponentRendering() sets PageModel query params")
    void testPageModelComponentRendering() {
        builder.get("/site/resourceapi/news")
               .pageModelComponentRendering("r5_r1_r1")
               .execute();

        String queryString = mockRequest.getQueryString();
        assertTrue(queryString.contains("_hn:type=component-rendering"));
        assertTrue(queryString.contains("_hn:ref=r5_r1_r1"));
    }

    @Test
    @DisplayName("execute() calls executor and returns response")
    void testExecute() {
        String response = builder.get("/site/api/test").execute();

        assertEquals("test response", response);
    }

    @Test
    @DisplayName("assertBody() verifies response matches expected")
    void testAssertBodySuccess() {
        assertDoesNotThrow(() ->
            builder.get("/site/api/test")
                   .assertBody("test response")
        );
    }

    @Test
    @DisplayName("assertBody() fails when response doesn't match")
    void testAssertBodyFailure() {
        AssertionError error = assertThrows(AssertionError.class, () ->
            builder.get("/site/api/test")
                   .assertBody("wrong response")
        );

        assertTrue(error.getMessage().contains("Response body mismatch"));
    }

    @Test
    @DisplayName("Fluent chaining works correctly")
    void testFluentChaining() {
        builder.get("/site/api/test")
               .withAccept(MediaType.APPLICATION_JSON)
               .withHeader("X-Custom", "value")
               .queryParam("param", "value")
               .execute();

        assertEquals(HttpMethod.GET, mockRequest.getMethod());
        assertEquals("/site/api/test", mockRequest.getRequestURI());
        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.ACCEPT));
        assertEquals("value", mockRequest.getHeader("X-Custom"));
        assertEquals("param=value", mockRequest.getQueryString());
    }

    @Test
    @DisplayName("Query params only applied on execute")
    void testQueryParamsLazyApplication() {
        builder.get("/site/api/test")
               .queryParam("name", "value");

        assertNull(mockRequest.getQueryString(), "Query string should not be set until execute()");

        builder.execute();

        assertEquals("name=value", mockRequest.getQueryString());
    }

    @Test
    @DisplayName("asUser() sets remote user, principal, and roles")
    void testAsUserSetsRemoteUserAndRoles() {
        builder.asUser("john", "admin", "editor");

        assertEquals("john", mockRequest.getRemoteUser());
        assertNotNull(mockRequest.getUserPrincipal());
        assertEquals("john", mockRequest.getUserPrincipal().getName());
        assertTrue(mockRequest.isUserInRole("admin"));
        assertTrue(mockRequest.isUserInRole("editor"));
    }

    @Test
    @DisplayName("asUser() with username only sets remote user and principal without roles")
    void testAsUserWithoutRoles() {
        builder.asUser("jane");

        assertEquals("jane", mockRequest.getRemoteUser());
        assertNotNull(mockRequest.getUserPrincipal());
        assertEquals("jane", mockRequest.getUserPrincipal().getName());
        assertTrue(mockRequest.getUserRoleNames().isEmpty());
    }

    @Test
    @DisplayName("withRole() sets roles without username")
    void testWithRoleSetsRolesOnly() {
        builder.withRole("viewer", "commenter");

        assertNull(mockRequest.getRemoteUser());
        assertTrue(mockRequest.isUserInRole("viewer"));
        assertTrue(mockRequest.isUserInRole("commenter"));
    }

    @Test
    @DisplayName("asUser() chains with other methods")
    void testAsUserChaining() {
        builder.get("/api/protected")
               .asUser("jane", "admin")
               .withAccept(MediaType.APPLICATION_JSON);

        assertEquals("jane", mockRequest.getRemoteUser());
        assertEquals("jane", mockRequest.getUserPrincipal().getName());
        assertTrue(mockRequest.isUserInRole("admin"));
        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.ACCEPT));
    }

    @Test
    @DisplayName("withRole() chains with other methods")
    void testWithRoleChaining() {
        builder.get("/api/protected")
               .withRole("admin")
               .withHeader("X-Custom", "value");

        assertTrue(mockRequest.isUserInRole("admin"));
        assertEquals("value", mockRequest.getHeader("X-Custom"));
    }
}
