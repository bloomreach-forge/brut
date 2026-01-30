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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    @DisplayName("of(status, rawBody) creates Response with String body")
    void testOfWithRawBody() {
        Response<String> response = Response.of(200, "test body");

        assertEquals(200, response.status());
        assertEquals("test body", response.body());
        assertEquals("test body", response.rawBody());
    }

    @Test
    @DisplayName("of(status, rawBody, body) creates Response with typed body")
    void testOfWithTypedBody() {
        TestUser user = new TestUser("John", 30);
        Response<TestUser> response = Response.of(201, "{\"name\":\"John\"}", user);

        assertEquals(201, response.status());
        assertEquals(user, response.body());
        assertEquals("{\"name\":\"John\"}", response.rawBody());
    }

    @Test
    @DisplayName("isSuccessful() returns true for 2xx status codes")
    void testIsSuccessful() {
        assertTrue(Response.of(200, "").isSuccessful());
        assertTrue(Response.of(201, "").isSuccessful());
        assertTrue(Response.of(204, "").isSuccessful());
        assertTrue(Response.of(299, "").isSuccessful());

        assertFalse(Response.of(199, "").isSuccessful());
        assertFalse(Response.of(300, "").isSuccessful());
        assertFalse(Response.of(400, "").isSuccessful());
        assertFalse(Response.of(500, "").isSuccessful());
    }

    @Test
    @DisplayName("isClientError() returns true for 4xx status codes")
    void testIsClientError() {
        assertTrue(Response.of(400, "").isClientError());
        assertTrue(Response.of(401, "").isClientError());
        assertTrue(Response.of(404, "").isClientError());
        assertTrue(Response.of(499, "").isClientError());

        assertFalse(Response.of(399, "").isClientError());
        assertFalse(Response.of(500, "").isClientError());
        assertFalse(Response.of(200, "").isClientError());
    }

    @Test
    @DisplayName("isServerError() returns true for 5xx status codes")
    void testIsServerError() {
        assertTrue(Response.of(500, "").isServerError());
        assertTrue(Response.of(502, "").isServerError());
        assertTrue(Response.of(503, "").isServerError());
        assertTrue(Response.of(599, "").isServerError());

        assertFalse(Response.of(499, "").isServerError());
        assertFalse(Response.of(600, "").isServerError());
        assertFalse(Response.of(200, "").isServerError());
    }

    @Test
    @DisplayName("equals() compares status and body")
    void testEquals() {
        Response<String> r1 = Response.of(200, "body");
        Response<String> r2 = Response.of(200, "body");
        Response<String> r3 = Response.of(201, "body");
        Response<String> r4 = Response.of(200, "different");

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertNotEquals(r1, r4);
    }

    @Test
    @DisplayName("toString() includes status and body")
    void testToString() {
        Response<String> response = Response.of(200, "test");

        String str = response.toString();
        assertTrue(str.contains("200"));
        assertTrue(str.contains("test"));
    }

    static class TestUser {
        public String name;
        public int age;

        TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestUser user = (TestUser) o;
            return age == user.age && name.equals(user.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + age;
        }
    }
}
