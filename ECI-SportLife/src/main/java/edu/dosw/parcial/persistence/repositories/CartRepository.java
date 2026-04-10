package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.persistence.entities.CartEntity;
import edu.dosw.parcial.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {

    // Recupera el carrito del usuario autenticado
    Optional<CartEntity> findByUser(UserEntity user);
}
