package edu.dosw.parcial.core.utils;

import org.springframework.http.HttpStatus;

// Excepción base del dominio — todas las excepciones de negocio heredan de esta
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // Retorna el código HTTP asociado para que el GlobalExceptionHandler lo use en la respuesta
    public HttpStatus getStatus() {
        return status;
    }
}
