package edu.dosw.parcial.controller.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record ApprovePaymentRequest(

        @NotBlank(message = "El ID de transacción es obligatorio")
        String transactionId
) {}
