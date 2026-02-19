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

    @Test
    @DisplayName("executeAsPageModel() throws AssertionError with diagnostic when response is null")
    void executeAsPageModel_nullResponse_throwsAssertionErrorWithDiagnostic() {
        RequestBuilder nullResponseBuilder = new RequestBuilder(mockRequest, () -> null);

        AssertionError error = assertThrows(AssertionError.class, () ->
            nullResponseBuilder.get("/site/fr/resourceapi").executeAsPageModel()
        );

        assertTrue(error.getMessage().contains("/site/fr/resourceapi"));
        assertTrue(error.getMessage().toLowerCase().contains("component"));
    }

    @Test
    @DisplayName("executeAsPageModel() throws AssertionError with diagnostic when response is empty")
    void executeAsPageModel_emptyResponse_throwsAssertionErrorWithDiagnostic() {
        RequestBuilder emptyResponseBuilder = new RequestBuilder(mockRequest, () -> "");

        AssertionError error = assertThrows(AssertionError.class, () ->
            emptyResponseBuilder.get("/site/fr/resourceapi").executeAsPageModel()
        );

        assertTrue(error.getMessage().toLowerCase().contains("component"));
    }

    @Test
    @DisplayName("executeAs() deserializes JSON response to specified type")
    void testExecuteAsDeserializesJson() throws JsonProcessingException {
        String jsonResponse = "{\"name\":\"John\",\"age\":30}";
        RequestBuilder jsonBuilder = new RequestBuilder(mockRequest, () -> jsonResponse);

        TestUser user = jsonBuilder.get("/api/user").executeAs(TestUser.class);

        assertEquals("John", user.name);
        assertEquals(30, user.age);
    }

    @Test
    @DisplayName("executeAs() ignores unknown properties")
    void testExecuteAsIgnoresUnknownProperties() throws JsonProcessingException {
        String jsonResponse = "{\"name\":\"Jane\",\"age\":25,\"unknownField\":\"ignored\"}";
        RequestBuilder jsonBuilder = new RequestBuilder(mockRequest, () -> jsonResponse);

        TestUser user = jsonBuilder.get("/api/user").executeAs(TestUser.class);

        assertEquals("Jane", user.name);
        assertEquals(25, user.age);
    }

    @Test
    @DisplayName("executeAs() throws JsonProcessingException for invalid JSON")
    void testExecuteAsThrowsOnInvalidJson() {
        String invalidJson = "not valid json";
        RequestBuilder jsonBuilder = new RequestBuilder(mockRequest, () -> invalidJson);

        assertThrows(JsonProcessingException.class, () ->
            jsonBuilder.get("/api/user").executeAs(TestUser.class)
        );
    }

    @Test
    @DisplayName("executeAs() works with nested objects")
    void testExecuteAsWithNestedObjects() throws JsonProcessingException {
        String jsonResponse = "{\"name\":\"Alice\",\"age\":28,\"address\":{\"city\":\"Amsterdam\"}}";
        RequestBuilder jsonBuilder = new RequestBuilder(mockRequest, () -> jsonResponse);

        TestUserWithAddress user = jsonBuilder.get("/api/user").executeAs(TestUserWithAddress.class);

        assertEquals("Alice", user.name);
        assertEquals(28, user.age);
        assertNotNull(user.address);
        assertEquals("Amsterdam", user.address.city);
    }

    @Test
    @DisplayName("withBody() sets request input stream")
    void testWithBodySetsInputStream() {
        builder.post("/api/users")
               .withBody("test body content")
               .execute();

        assertNotNull(mockRequest.getInputStream());
    }

    @Test
    @DisplayName("withJsonBody(String) sets body and Content-Type header")
    void testWithJsonBodyStringSetsBodyAndContentType() {
        builder.post("/api/users")
               .withJsonBody("{\"name\":\"John\"}")
               .execute();

        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(mockRequest.getInputStream());
    }

    @Test
    @DisplayName("withJsonBody(Object) serializes object and sets Content-Type")
    void testWithJsonBodyObjectSerializesAndSetsContentType() throws JsonProcessingException {
        TestUser user = new TestUser();
        user.name = "Jane";
        user.age = 25;

        builder.post("/api/users")
               .withJsonBody(user)
               .execute();

        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(mockRequest.getInputStream());
    }

    @Test
    @DisplayName("withBody() does not set Content-Type automatically")
    void testWithBodyDoesNotSetContentType() {
        builder.post("/api/users")
               .withBody("<xml>data</xml>")
               .execute();

        assertNull(mockRequest.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    @DisplayName("withBody() chains with other methods")
    void testWithBodyChaining() {
        builder.post("/api/users")
               .withHeader("X-Custom", "value")
               .withJsonBody("{\"name\":\"test\"}")
               .asUser("admin", "editor")
               .execute();

        assertEquals("admin", mockRequest.getRemoteUser());
        assertEquals("value", mockRequest.getHeader("X-Custom"));
        assertEquals(MediaType.APPLICATION_JSON, mockRequest.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    @DisplayName("executeWithStatus() returns Response with status code")
    void testExecuteWithStatusReturnsStatusAndBody() {
        int[] capturedStatus = {201};
        RequestBuilder statusBuilder = new RequestBuilder(
                mockRequest,
                () -> "created",
                () -> capturedStatus[0]
        );

        Response<String> response = statusBuilder.get("/api/users").executeWithStatus();

        assertEquals(201, response.status());
        assertEquals("created", response.body());
    }

    @Test
    @DisplayName("executeWithStatus(Class) returns Response with typed body")
    void testExecuteWithStatusTypedReturnsStatusAndBody() throws JsonProcessingException {
        String jsonResponse = "{\"name\":\"John\",\"age\":30}";
        RequestBuilder statusBuilder = new RequestBuilder(
                mockRequest,
                () -> jsonResponse,
                () -> 200
        );

        Response<TestUser> response = statusBuilder.get("/api/user").executeWithStatus(TestUser.class);

        assertEquals(200, response.status());
        assertEquals("John", response.body().name);
        assertEquals(30, response.body().age);
        assertEquals(jsonResponse, response.rawBody());
    }

    @Test
    @DisplayName("executeWithStatus() with error status code")
    void testExecuteWithStatusErrorCode() {
        RequestBuilder statusBuilder = new RequestBuilder(
                mockRequest,
                () -> "{\"error\":\"not found\"}",
                () -> 404
        );

        Response<String> response = statusBuilder.get("/api/missing").executeWithStatus();

        assertEquals(404, response.status());
        assertTrue(response.isClientError());
        assertFalse(response.isSuccessful());
    }

    @Test
    @DisplayName("executeWithStatus() defaults to 200 when no status supplier")
    void testExecuteWithStatusDefaultsTo200() {
        // Using constructor without status supplier
        RequestBuilder defaultBuilder = new RequestBuilder(mockRequest, () -> "ok");

        Response<String> response = defaultBuilder.get("/api/test").executeWithStatus();

        assertEquals(200, response.status());
    }

    // Test DTOs
    static class TestUser {
        public String name;
        public int age;
    }

    static class TestUserWithAddress {
        public String name;
        public int age;
        public TestAddress address;
    }

    static class TestAddress {
        public String city;
    }
}
