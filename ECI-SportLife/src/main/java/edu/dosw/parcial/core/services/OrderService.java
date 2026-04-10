package edu.dosw.parcial.core.services;

import edu.dosw.parcial.controller.dtos.request.ApprovePaymentRequest;
import edu.dosw.parcial.controller.dtos.request.CheckoutRequest;
import edu.dosw.parcial.controller.dtos.request.RejectPaymentRequest;
import edu.dosw.parcial.controller.dtos.request.RetryPaymentRequest;
import edu.dosw.parcial.controller.dtos.response.OrderResponse;
import edu.dosw.parcial.controller.dtos.response.PaymentResultResponse;
import edu.dosw.parcial.core.models.OrderStatus;
import edu.dosw.parcial.core.models.PaymentStatus;
import edu.dosw.parcial.core.utils.ConflictException;
import edu.dosw.parcial.core.utils.ResourceNotFoundException;
import edu.dosw.parcial.core.utils.UnprocessableEntityException;
import edu.dosw.parcial.persistence.entities.*;
import edu.dosw.parcial.persistence.repositories.CartRepository;
import edu.dosw.parcial.persistence.repositories.OrderRepository;
import edu.dosw.parcial.persistence.repositories.PaymentRepository;
import edu.dosw.parcial.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    // F-09: Valida el carrito, genera la orden y registra el primer intento de pago
    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        UserEntity user = getUser(email);

        CartEntity cart = cartRepository.findByUser(user)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new UnprocessableEntityException("No puedes iniciar el pago con el carrito vacío"));

        // Valida stock actualizado para cada item — puede haber cambiado desde que se agregó al carrito
        for (CartItemEntity item : cart.getItems()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new UnprocessableEntityException(
                        "El producto '" + item.getProduct().getName() +
                        "' ya no tiene stock suficiente. Disponible: " + item.getProduct().getStock()
                );
            }
        }

        // Calcula el total sumando los subtotales de cada item
        BigDecimal total = cart.getItems().stream()
                .map(CartItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crea la orden como snapshot inmutable del carrito
        OrderEntity order = OrderEntity.builder()
                .user(user)
                .total(total)
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItemEntity> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItemEntity.builder()
                        .order(order)
                        .product(cartItem.getProduct())
                        .productName(cartItem.getProduct().getName())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .build())
                .toList();

        order.getItems().addAll(orderItems);
        OrderEntity saved = orderRepository.save(order);

        // Registra el intento de pago asociado a la orden
        paymentRepository.save(PaymentEntity.builder()
                .order(saved)
                .method(request.paymentMethod())
                .paymentToken(request.paymentToken())
                .status(PaymentStatus.PENDING)
                .build());

        return toOrderResponse(saved);
    }

    // F-10: Aprueba el pago, descuenta stock y vacía el carrito
    @Transactional
    public PaymentResultResponse approve(String email, UUID orderId, ApprovePaymentRequest request) {
        OrderEntity order = getPendingOrder(orderId);
        validateOrderOwner(order, email);

        // Descuenta el stock de cada producto — segunda validación por si cambió entre checkout y aprobación
        for (OrderItemEntity item : order.getItems()) {
            ProductEntity product = item.getProduct();
            if (item.getQuantity() > product.getStock()) {
                throw new UnprocessableEntityException(
                        "El producto '" + product.getName() + "' se agotó antes de confirmar el pago"
                );
            }
            product.setStock(product.getStock() - item.getQuantity());
        }

        order.setStatus(OrderStatus.PAID);

        // Marca el pago pendiente como aprobado y guarda el ID de transacción de la pasarela
        order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(PaymentStatus.APPROVED);
                    p.setTransactionId(request.transactionId());
                });

        orderRepository.save(order);

        // Vacía el carrito tras confirmación exitosa
        cartRepository.findByUser(order.getUser()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });

        return new PaymentResultResponse(
                order.getId(), OrderStatus.PAID, request.transactionId(),
                null, order.getTotal(), "Tu compra fue exitosa. ¡Gracias por comprar en SportLife!"
        );
    }

    // F-11: Rechaza el pago — no afecta stock ni carrito para permitir reintento
    @Transactional
    public PaymentResultResponse reject(String email, UUID orderId, RejectPaymentRequest request) {
        OrderEntity order = getPendingOrder(orderId);
        validateOrderOwner(order, email);

        order.setStatus(OrderStatus.REJECTED);

        order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .ifPresent(p -> p.setStatus(PaymentStatus.REJECTED));

        orderRepository.save(order);

        return new PaymentResultResponse(
                order.getId(), OrderStatus.REJECTED, null,
                request.reason(), order.getTotal(), "Tu pago no pudo procesarse. Puedes intentarlo de nuevo."
        );
    }

    // F-12: Reintenta el pago sobre una orden rechazada — valida stock nuevamente
    @Transactional
    public OrderResponse retry(String email, UUID orderId, RetryPaymentRequest request) {
        getUser(email);

        OrderEntity order = orderRepository.findByIdAndStatus(orderId, OrderStatus.REJECTED)
                .orElseThrow(() -> new ConflictException(
                        "Solo se puede reintentar el pago de una orden rechazada"
                ));

        validateOrderOwner(order, email);

        // Revalida stock antes de permitir el reintento
        for (OrderItemEntity item : order.getItems()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new UnprocessableEntityException(
                        "El producto '" + item.getProduct().getName() + "' ya no tiene stock disponible"
                );
            }
        }

        // Vuelve la orden a PENDING y crea un nuevo intento de pago
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        paymentRepository.save(PaymentEntity.builder()
                .order(order)
                .method(request.paymentMethod())
                .paymentToken(request.paymentToken())
                .status(PaymentStatus.PENDING)
                .build());

        return toOrderResponse(order);
    }

    // Busca una orden en estado PENDING — lanza 409 si ya fue procesada con otro estado
    private OrderEntity getPendingOrder(UUID orderId) {
        return orderRepository.findById(orderId).map(order -> {
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new ConflictException(
                        "La orden ya fue procesada con estado: " + order.getStatus()
                );
            }
            return order;
        }).orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna orden con el ID indicado"));
    }

    // Garantiza que el usuario solo pueda operar sobre sus propias órdenes
    private void validateOrderOwner(OrderEntity order, String email) {
        if (!order.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("No se encontró ninguna orden con el ID indicado");
        }
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private OrderResponse toOrderResponse(OrderEntity order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), order.getCreatedAt());
    }
}
