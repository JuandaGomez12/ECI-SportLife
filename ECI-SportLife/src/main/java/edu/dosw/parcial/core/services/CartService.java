package edu.dosw.parcial.core.services;

import edu.dosw.parcial.controller.dtos.request.AddToCartRequest;
import edu.dosw.parcial.controller.dtos.response.CartItemResponse;
import edu.dosw.parcial.controller.dtos.response.CartResponse;
import edu.dosw.parcial.core.models.ProductStatus;
import edu.dosw.parcial.core.utils.ResourceNotFoundException;
import edu.dosw.parcial.core.utils.UnprocessableEntityException;
import edu.dosw.parcial.persistence.entities.CartEntity;
import edu.dosw.parcial.persistence.entities.CartItemEntity;
import edu.dosw.parcial.persistence.entities.ProductEntity;
import edu.dosw.parcial.persistence.entities.UserEntity;
import edu.dosw.parcial.persistence.repositories.CartItemRepository;
import edu.dosw.parcial.persistence.repositories.CartRepository;
import edu.dosw.parcial.persistence.repositories.ProductRepository;
import edu.dosw.parcial.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // F-07: Agrega un producto al carrito o suma cantidad si ya existía
    @Transactional
    public CartItemResponse addItem(String email, AddToCartRequest request) {
        UserEntity user = getUser(email);
        ProductEntity product = getActiveProduct(request.productId());

        CartEntity cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(CartEntity.builder().user(user).build()));

        // Si el producto ya está en el carrito, se acumula la cantidad en lugar de duplicar el ítem
        CartItemEntity item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> CartItemEntity.builder()
                        .cart(cart)
                        .product(product)
                        .unitPrice(product.getPrice())
                        .quantity(0)
                        .build());

        int newQuantity = item.getQuantity() + request.quantity();

        // Valida que la cantidad total no supere el stock disponible
        if (newQuantity > product.getStock()) {
            throw new UnprocessableEntityException(
                    "Stock insuficiente. Disponible: " + product.getStock() + ", solicitado: " + newQuantity
            );
        }

        item.setQuantity(newQuantity);
        CartItemEntity saved = cartItemRepository.save(item);

        return toItemResponse(saved);
    }

    // F-08: Retorna el resumen completo del carrito con subtotales y total
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        UserEntity user = getUser(email);

        List<CartItemResponse> items = cartRepository.findByUser(user)
                .map(cart -> cart.getItems().stream().map(this::toItemResponse).toList())
                .orElse(List.of());

        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();

        return new CartResponse(items, total, itemCount);
    }

    // Convierte la entidad CartItem al DTO de respuesta
    private CartItemResponse toItemResponse(CartItemEntity item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    // Busca el usuario por email — lanzado desde el contexto de seguridad
    private UserEntity getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    // Busca el producto y valida que esté activo antes de agregarlo al carrito
    private ProductEntity getActiveProduct(java.util.UUID productId) {
        return productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún producto con el ID indicado"));
    }
}
