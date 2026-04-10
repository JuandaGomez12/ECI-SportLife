package edu.dosw.parcial.controller.dtos.response;

import edu.dosw.parcial.core.models.Category;
import edu.dosw.parcial.core.models.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        Category category,
        BigDecimal price,
        Integer stock,
        List<String> images,
        ProductStatus status,
        LocalDateTime createdAt
) {}
