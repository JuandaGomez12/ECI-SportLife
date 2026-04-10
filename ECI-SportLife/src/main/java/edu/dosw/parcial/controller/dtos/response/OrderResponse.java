package edu.dosw.parcial.controller.dtos.response;

import edu.dosw.parcial.core.models.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt
) {}
