package org.bloomreach.forge.brut.common.repository.utils;

public class RuntimeReflectionException extends RuntimeException {

    public RuntimeReflectionException(Exception e) {
        super(e.getMessage(), e);
    }
}
