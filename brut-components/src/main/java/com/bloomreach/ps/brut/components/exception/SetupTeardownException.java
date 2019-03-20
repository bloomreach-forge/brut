
package com.bloomreach.ps.brut.components.exception;

public class SetupTeardownException extends RuntimeException {

    public SetupTeardownException(Exception cause) {
        super(cause.getMessage(), cause);
    }
}
