package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.persistence.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
}
