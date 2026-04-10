package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.persistence.entities.CartEntity;
import edu.dosw.parcial.persistence.entities.CartItemEntity;
import edu.dosw.parcial.persistence.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    // Busca si el producto ya existe en el carrito para sumar cantidad en lugar de duplicar
    Optional<CartItemEntity> findByCartAndProduct(CartEntity cart, ProductEntity product);
}
