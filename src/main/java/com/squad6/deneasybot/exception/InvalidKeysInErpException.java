package com.squad6.deneasybot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvalidKeysInErpException extends RuntimeException {
    public InvalidKeysInErpException(String message) {
        super(message);
    }
}