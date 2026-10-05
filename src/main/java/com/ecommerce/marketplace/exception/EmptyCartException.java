package com.ecommerce.marketplace.exception;

import org.springframework.http.HttpStatus;

public class EmptyCartException extends BusinessException {

    public EmptyCartException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
