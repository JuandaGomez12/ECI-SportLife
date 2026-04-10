package edu.dosw.parcial.core.utils;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, HttpStatus.valueOf(409));
    }
}
