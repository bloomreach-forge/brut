package org.bloomreach.forge.brut.common.repository;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

public class AcceptAllLoginModule implements LoginModule {

    private static final String DEFAULT_ANONYMOUS_ID = "configuser";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private String anonymousId = DEFAULT_ANONYMOUS_ID;
    private Credentials credentials;
    private Principal principal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        if (options != null) {
            Object configuredAnonymous = options.get("anonymousId");
            if (configuredAnonymous instanceof String && !((String) configuredAnonymous).isEmpty()) {
                this.anonymousId = (String) configuredAnonymous;
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }
        CredentialsCallback callback = new CredentialsCallback();
        try {
            callbackHandler.handle(new Callback[] { callback });
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Unable to obtain credentials: " + e.getMessage());
        }
        credentials = callback.getCredentials();
        principal = new PrincipalImpl(resolveUserId(credentials));
        return true;
    }

    @Override
    public boolean commit() {
        if (principal == null || subject == null) {
            return false;
        }
        subject.getPrincipals().add(principal);
        if (credentials != null) {
            subject.getPublicCredentials().add(credentials);
        }
        return true;
    }

    @Override
    public boolean abort() {
        return logout();
    }

    @Override
    public boolean logout() {
        if (subject != null && principal != null) {
            subject.getPrincipals().remove(principal);
        }
        if (subject != null && credentials != null) {
            subject.getPublicCredentials().remove(credentials);
        }
        principal = null;
        credentials = null;
        return true;
    }

    private String resolveUserId(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            return ((SimpleCredentials) credentials).getUserID();
        }
        if (credentials instanceof GuestCredentials) {
            return anonymousId;
        }
        if (credentials == null) {
            return anonymousId;
        }
        return credentials.getClass().getSimpleName();
    }
}
