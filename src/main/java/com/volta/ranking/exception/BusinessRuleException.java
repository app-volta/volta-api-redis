package com.volta.ranking.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends VoltaException {

    public BusinessRuleException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
