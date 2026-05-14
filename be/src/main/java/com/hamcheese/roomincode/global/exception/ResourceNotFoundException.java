package com.hamcheese.roomincode.global.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "NOT_FOUND";
    }

}
