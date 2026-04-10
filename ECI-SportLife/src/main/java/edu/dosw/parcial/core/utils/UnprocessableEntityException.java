package edu.dosw.parcial.core.utils;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends BusinessException {

    public UnprocessableEntityException(String message) {
        super(message, HttpStatus.valueOf(422));
    }
}
