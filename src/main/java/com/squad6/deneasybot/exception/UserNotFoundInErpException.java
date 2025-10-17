package com.squad6.deneasybot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundInErpException extends RuntimeException {
    public UserNotFoundInErpException(String message) {
        super(message);
    }
}