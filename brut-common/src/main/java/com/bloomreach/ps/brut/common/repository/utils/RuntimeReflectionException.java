package com.bloomreach.ps.brut.common.repository.utils;

public class RuntimeReflectionException extends RuntimeException {

    public RuntimeReflectionException(Exception e) {
        super(e.getMessage(), e);
    }
}
