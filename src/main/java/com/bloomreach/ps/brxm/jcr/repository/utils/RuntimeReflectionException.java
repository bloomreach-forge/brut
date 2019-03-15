package com.bloomreach.ps.brxm.jcr.repository.utils;

public class RuntimeReflectionException extends RuntimeException {

    public RuntimeReflectionException(Exception e) {
        super(e.getMessage(), e);
    }
}
