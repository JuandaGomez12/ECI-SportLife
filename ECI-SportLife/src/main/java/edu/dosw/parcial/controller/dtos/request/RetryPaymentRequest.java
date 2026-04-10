package edu.dosw.parcial.controller.dtos.request;

import edu.dosw.parcial.core.models.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RetryPaymentRequest(

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        @NotBlank(message = "El token de pago es obligatorio")
        String paymentToken
) {}
