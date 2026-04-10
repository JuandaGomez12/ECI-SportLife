package edu.dosw.parcial.core.utils;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.valueOf(404));
    }
}
