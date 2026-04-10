package edu.dosw.parcial.controller.handlers;

import edu.dosw.parcial.controller.dtos.request.AddToCartRequest;
import edu.dosw.parcial.controller.dtos.response.CartItemResponse;
import edu.dosw.parcial.controller.dtos.response.CartResponse;
import edu.dosw.parcial.core.services.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Gestión del carrito de compras")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    // F-07: Agrega un producto al carrito del usuario autenticado
    @Operation(summary = "Agregar producto al carrito")
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addItem(userDetails.getUsername(), request));
    }

    // F-08: Retorna el resumen del carrito con items, subtotales y total
    @Operation(summary = "Ver resumen del carrito")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(userDetails.getUsername()));
    }
}
