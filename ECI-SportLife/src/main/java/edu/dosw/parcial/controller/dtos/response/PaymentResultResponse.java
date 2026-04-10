package edu.dosw.parcial.controller.dtos.response;

import edu.dosw.parcial.core.models.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResultResponse(
        UUID orderId,
        OrderStatus status,
        String transactionId,
        String reason,
        BigDecimal total,
        String message
) {}
