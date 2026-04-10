package edu.dosw.parcial.controller.dtos.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal total,
        int itemCount
) {}
