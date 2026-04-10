package edu.dosw.parcial.controller.dtos.response;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {}
