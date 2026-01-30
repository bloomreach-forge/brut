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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.jcr.GuestCredentials;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.*;

class MockAuthenticationConfigTest {

    @AfterEach
    void tearDown() {
        MockAuthenticationConfig.reset();
    }

    @Test
    void shouldAccept_acceptsAllByDefault() {
        SimpleCredentials creds = new SimpleCredentials("anyuser", "anypassword".toCharArray());

        assertTrue(MockAuthenticationConfig.current().shouldAccept(creds));
    }

    @Test
    void rejectUser_rejectsSpecificUser() {
        MockAuthenticationConfig.current().rejectUser("baduser");

        SimpleCredentials rejected = new SimpleCredentials("baduser", "password".toCharArray());
        SimpleCredentials accepted = new SimpleCredentials("gooduser", "password".toCharArray());

        assertFalse(MockAuthenticationConfig.current().shouldAccept(rejected));
        assertTrue(MockAuthenticationConfig.current().shouldAccept(accepted));
    }

    @Test
    void rejectPassword_rejectsSpecificPassword() {
        MockAuthenticationConfig.current().rejectPassword("wrongpassword");

        SimpleCredentials rejected = new SimpleCredentials("user", "wrongpassword".toCharArray());
        SimpleCredentials accepted = new SimpleCredentials("user", "correctpassword".toCharArray());

        assertFalse(MockAuthenticationConfig.current().shouldAccept(rejected));
        assertTrue(MockAuthenticationConfig.current().shouldAccept(accepted));
    }

    @Test
    void chaining_allowsMultipleRules() {
        MockAuthenticationConfig.current()
            .rejectUser("user1")
            .rejectUser("user2")
            .rejectPassword("badpass");

        assertFalse(MockAuthenticationConfig.current().shouldAccept(
            new SimpleCredentials("user1", "anypass".toCharArray())));
        assertFalse(MockAuthenticationConfig.current().shouldAccept(
            new SimpleCredentials("user2", "anypass".toCharArray())));
        assertFalse(MockAuthenticationConfig.current().shouldAccept(
            new SimpleCredentials("anyuser", "badpass".toCharArray())));
        assertTrue(MockAuthenticationConfig.current().shouldAccept(
            new SimpleCredentials("gooduser", "goodpass".toCharArray())));
    }

    @Test
    void reset_clearsAllRules() {
        MockAuthenticationConfig.current().rejectUser("baduser");
        MockAuthenticationConfig.reset();

        SimpleCredentials creds = new SimpleCredentials("baduser", "password".toCharArray());
        assertTrue(MockAuthenticationConfig.current().shouldAccept(creds));
    }

    @Test
    void shouldAccept_acceptsGuestCredentials() {
        MockAuthenticationConfig.current().rejectUser("guest");

        // GuestCredentials should still be accepted (no username to check)
        assertTrue(MockAuthenticationConfig.current().shouldAccept(new GuestCredentials()));
    }

    @Test
    void hasRules_returnsFalseWhenEmpty() {
        assertFalse(MockAuthenticationConfig.current().hasRules());
    }

    @Test
    void hasRules_returnsTrueWhenConfigured() {
        MockAuthenticationConfig.current().rejectUser("baduser");
        assertTrue(MockAuthenticationConfig.current().hasRules());
    }
}
