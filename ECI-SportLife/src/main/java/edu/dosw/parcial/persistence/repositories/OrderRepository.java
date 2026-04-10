package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.core.models.OrderStatus;
import edu.dosw.parcial.persistence.entities.OrderEntity;
import edu.dosw.parcial.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    // Busca una orden por ID y estado — evita transiciones inválidas (ej: aprobar una orden ya pagada)
    Optional<OrderEntity> findByIdAndStatus(UUID id, OrderStatus status);

    // Verifica si el usuario tiene una orden pendiente activa
    boolean existsByUserAndStatus(UserEntity user, OrderStatus status);
}
