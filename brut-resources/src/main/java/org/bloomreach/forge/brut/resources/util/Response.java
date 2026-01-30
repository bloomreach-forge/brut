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

import java.util.Objects;

/**
 * Wrapper for HTTP response containing status code and typed body.
 * Used with {@link RequestBuilder#executeWithStatus(Class)} to access
 * both the response body and HTTP status code.
 *
 * <p>Example usage:
 * <pre>
 * Response&lt;User&gt; response = brxm.request()
 *     .post("/site/api/users")
 *     .withJsonBody(newUser)
 *     .executeWithStatus(User.class);
 *
 * assertThat(response.status()).isEqualTo(201);
 * assertThat(response.body().getName()).isEqualTo("John");
 * </pre>
 *
 * @param <T> the type of the response body
 */
public final class Response<T> {

    private final int status;
    private final String rawBody;
    private final T body;

    private Response(int status, String rawBody, T body) {
        this.status = status;
        this.rawBody = rawBody;
        this.body = body;
    }

    /**
     * Creates a Response with status code and typed body.
     *
     * @param status HTTP status code
     * @param rawBody raw response body string
     * @param body deserialized body object
     * @param <T> body type
     * @return new Response instance
     */
    public static <T> Response<T> of(int status, String rawBody, T body) {
        return new Response<>(status, rawBody, body);
    }

    /**
     * Creates a Response with status code and raw body only (no deserialization).
     *
     * @param status HTTP status code
     * @param rawBody raw response body string
     * @return new Response instance with String body
     */
    public static Response<String> of(int status, String rawBody) {
        return new Response<>(status, rawBody, rawBody);
    }

    /**
     * Returns the HTTP status code.
     *
     * @return status code (e.g., 200, 201, 400, 404)
     */
    public int status() {
        return status;
    }

    /**
     * Returns the deserialized response body.
     *
     * @return body object, may be null if response was empty
     */
    public T body() {
        return body;
    }

    /**
     * Returns the raw response body as string.
     *
     * @return raw body string
     */
    public String rawBody() {
        return rawBody;
    }

    /**
     * Checks if the response indicates success (2xx status code).
     *
     * @return true if status is 200-299
     */
    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }

    /**
     * Checks if the response indicates a client error (4xx status code).
     *
     * @return true if status is 400-499
     */
    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    /**
     * Checks if the response indicates a server error (5xx status code).
     *
     * @return true if status is 500-599
     */
    public boolean isServerError() {
        return status >= 500 && status < 600;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response<?> response = (Response<?>) o;
        return status == response.status && Objects.equals(body, response.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, body);
    }

    @Override
    public String toString() {
        return "Response{status=" + status + ", body=" + body + "}";
    }
}
