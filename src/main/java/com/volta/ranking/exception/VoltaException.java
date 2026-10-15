package com.volta.ranking.exception;

import org.springframework.http.HttpStatus;

public abstract class VoltaException extends RuntimeException {

    private final HttpStatus status;

    protected VoltaException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
