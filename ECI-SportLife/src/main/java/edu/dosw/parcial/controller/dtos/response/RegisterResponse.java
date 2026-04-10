package edu.dosw.parcial.controller.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt
) {}
