package edu.dosw.parcial.controller.dtos.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
