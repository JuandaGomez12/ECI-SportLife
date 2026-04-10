package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // Busca un usuario por email para login y validación de duplicados en registro
    Optional<UserEntity> findByEmail(String email);

    // Verifica si ya existe una cuenta con ese email antes de crear una nueva
    boolean existsByEmail(String email);
}
