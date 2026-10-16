package com.volta.ranking.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends VoltaException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
