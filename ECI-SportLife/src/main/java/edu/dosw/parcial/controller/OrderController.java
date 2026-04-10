package edu.dosw.parcial.controller;

import edu.dosw.parcial.controller.dtos.request.ApprovePaymentRequest;
import edu.dosw.parcial.controller.dtos.request.CheckoutRequest;
import edu.dosw.parcial.controller.dtos.request.RejectPaymentRequest;
import edu.dosw.parcial.controller.dtos.request.RetryPaymentRequest;
import edu.dosw.parcial.controller.dtos.response.OrderResponse;
import edu.dosw.parcial.controller.dtos.response.PaymentResultResponse;
import edu.dosw.parcial.core.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // F-09: Inicia el proceso de pago creando una orden desde el carrito activo
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(user.getUsername(), request);
    }

    // F-10: Confirma el pago, descuenta stock y vacía el carrito
    @PatchMapping("/{id}/approve")
    public PaymentResultResponse approve(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody ApprovePaymentRequest request) {
        return orderService.approve(user.getUsername(), id, request);
    }

    // F-11: Rechaza el pago sin afectar stock para permitir reintento
    @PatchMapping("/{id}/reject")
    public PaymentResultResponse reject(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody RejectPaymentRequest request) {
        return orderService.reject(user.getUsername(), id, request);
    }

    // F-12: Reintenta el pago sobre una orden previamente rechazada
    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse retry(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody RetryPaymentRequest request) {
        return orderService.retry(user.getUsername(), id, request);
    }
}
