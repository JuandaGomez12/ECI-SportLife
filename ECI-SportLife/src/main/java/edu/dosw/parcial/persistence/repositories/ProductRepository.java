package edu.dosw.parcial.persistence.repositories;

import edu.dosw.parcial.core.models.Category;
import edu.dosw.parcial.core.models.ProductStatus;
import edu.dosw.parcial.persistence.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    // Retorna todos los productos con un estado específico (paginado) — usado en F-03
    Page<ProductEntity> findAllByStatus(ProductStatus status, Pageable pageable);

    // Filtra por estado y categoría — usado en F-04
    Page<ProductEntity> findAllByStatusAndCategory(ProductStatus status, Category category, Pageable pageable);

    // Búsqueda insensible a mayúsculas por nombre — usado en F-05
    Page<ProductEntity> findAllByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);

    // Busca un producto activo por ID — evita exponer productos inactivos a usuarios normales
    Optional<ProductEntity> findByIdAndStatus(UUID id, ProductStatus status);

    // Valida nombre duplicado antes de crear o actualizar un producto
    boolean existsByNameAndStatus(String name, ProductStatus status);
}
