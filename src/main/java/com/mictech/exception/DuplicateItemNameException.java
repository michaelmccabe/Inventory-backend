package com.mictech.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateItemNameException extends RuntimeException {
    public DuplicateItemNameException(String message) {
        super(message);
    }
}
