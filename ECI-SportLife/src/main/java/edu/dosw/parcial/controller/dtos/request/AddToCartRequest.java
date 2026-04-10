package edu.dosw.parcial.controller.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(

        @NotNull(message = "El ID del producto es obligatorio")
        UUID productId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que 0")
        Integer quantity
) {}
