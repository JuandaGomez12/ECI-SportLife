package edu.dosw.parcial.controller.handlers;

import edu.dosw.parcial.controller.dtos.response.ErrorResponse;
import edu.dosw.parcial.core.utils.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja todas las excepciones de negocio (404, 409, 422) con el código HTTP que traen
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Devuelve siempre 401 con mensaje genérico para no revelar si el email existe o no
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity
                .status(HttpStatus.valueOf(401))
                .body(ErrorResponse.of(401, "Unauthorized", "Credenciales inválidas", req.getRequestURI()));
    }

    // Maneja errores de validación de campos (@NotBlank, @Email, etc.) y lista todos los campos inválidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(400, "Bad Request", message, req.getRequestURI()));
    }

    // Maneja parámetros de URL con tipo incorrecto (ej: texto donde se espera UUID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(400, "Bad Request", message, req.getRequestURI()));
    }

    // Captura cualquier excepción no controlada para evitar exponer detalles internos del servidor
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(500, "Internal Server Error", "Error inesperado del servidor", req.getRequestURI()));
    }
}
