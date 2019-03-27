
package org.bloomreach.forge.brut.components.exception;

public class SetupTeardownException extends RuntimeException {

    public SetupTeardownException(Exception cause) {
        super(cause.getMessage(), cause);
    }
}
