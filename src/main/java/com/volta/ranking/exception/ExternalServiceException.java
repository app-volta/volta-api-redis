package com.volta.ranking.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends VoltaException {

    public ExternalServiceException(String service) {
        super(service + " is unavailable or returned an invalid response", HttpStatus.BAD_GATEWAY);
    }

    public ExternalServiceException(String service, String detail) {
        super(service + " returned an incomplete response: " + detail, HttpStatus.BAD_GATEWAY);
    }
}
