
package com.bloomreach.ps.brxm.unittester.exception;

public class SetupTeardownException extends RuntimeException {

    public SetupTeardownException(Exception cause) {
        super(cause.getMessage(), cause);
    }
}
