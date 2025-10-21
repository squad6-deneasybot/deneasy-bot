package com.squad6.deneasybot.exception;

public class UserNotFoundByPhoneException extends RuntimeException {
    public UserNotFoundByPhoneException(String message) {
        super(message);
    }
}
