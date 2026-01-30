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
package org.bloomreach.forge.brut.common.repository;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.HashSet;
import java.util.Set;

/**
 * Thread-local configuration for mock authentication behavior.
 * Allows tests to configure which credentials should be rejected.
 */
public class MockAuthenticationConfig {

    private static final ThreadLocal<MockAuthenticationConfig> CURRENT =
        ThreadLocal.withInitial(MockAuthenticationConfig::new);

    private final Set<String> rejectedUsers = new HashSet<>();
    private final Set<String> rejectedPasswords = new HashSet<>();

    /**
     * Gets the current thread's authentication config.
     */
    public static MockAuthenticationConfig current() {
        return CURRENT.get();
    }

    /**
     * Resets authentication config for the current thread.
     */
    public static void reset() {
        CURRENT.remove();
    }

    /**
     * Configures a username to be rejected during authentication.
     *
     * @param username the username that should fail authentication
     * @return this config for chaining
     */
    public MockAuthenticationConfig rejectUser(String username) {
        rejectedUsers.add(username);
        return this;
    }

    /**
     * Configures a password to be rejected during authentication.
     *
     * @param password the password that should fail authentication
     * @return this config for chaining
     */
    public MockAuthenticationConfig rejectPassword(String password) {
        rejectedPasswords.add(password);
        return this;
    }

    /**
     * Checks if the given credentials should be accepted.
     *
     * @param credentials the credentials to validate
     * @return true if credentials should be accepted, false if rejected
     */
    public boolean shouldAccept(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials simple = (SimpleCredentials) credentials;
            if (rejectedUsers.contains(simple.getUserID())) {
                return false;
            }
            String password = new String(simple.getPassword());
            if (rejectedPasswords.contains(password)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if any rejection rules are configured.
     */
    public boolean hasRules() {
        return !rejectedUsers.isEmpty() || !rejectedPasswords.isEmpty();
    }
}
