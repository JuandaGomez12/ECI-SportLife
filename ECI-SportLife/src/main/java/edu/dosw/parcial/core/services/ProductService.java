package edu.dosw.parcial.core.services;

import edu.dosw.parcial.controller.dtos.request.ProductRequest;
import edu.dosw.parcial.controller.dtos.response.PageResponse;
import edu.dosw.parcial.controller.dtos.response.ProductResponse;
import edu.dosw.parcial.core.models.Category;
import edu.dosw.parcial.core.models.ProductStatus;
import edu.dosw.parcial.core.utils.ConflictException;
import edu.dosw.parcial.core.utils.ResourceNotFoundException;
import edu.dosw.parcial.persistence.entities.ProductEntity;
import edu.dosw.parcial.persistence.mappers.ProductMapper;
import edu.dosw.parcial.persistence.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // F-03: Retorna todos los productos activos paginados
    public PageResponse<ProductResponse> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.of(
                productRepository.findAllByStatus(ProductStatus.ACTIVE, pageable)
                        .map(productMapper::toResponse)
        );
    }

    // F-04: Filtra productos activos por categoría
    public PageResponse<ProductResponse> findByCategory(Category category, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.of(
                productRepository.findAllByStatusAndCategory(ProductStatus.ACTIVE, category, pageable)
                        .map(productMapper::toResponse)
        );
    }

    // F-05: Busca productos activos cuyo nombre contenga el término (insensible a mayúsculas)
    public PageResponse<ProductResponse> findByName(String name, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.of(
                productRepository.findAllByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, name, pageable)
                        .map(productMapper::toResponse)
        );
    }

    // F-06: Retorna el detalle de un producto activo por su ID
    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getActiveProduct(id));
    }

    // F-13 (crear): Valida nombre único y persiste el nuevo producto
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByNameAndStatus(request.name(), ProductStatus.ACTIVE)) {
            throw new ConflictException("Ya existe un producto activo con el nombre '" + request.name() + "'");
        }
        ProductEntity entity = productMapper.toEntity(request);
        return productMapper.toResponse(productRepository.save(entity));
    }

    // F-13 (actualizar): Reemplaza todos los campos de un producto existente
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        ProductEntity entity = getActiveProduct(id);
        productMapper.updateEntity(request, entity);
        return productMapper.toResponse(productRepository.save(entity));
    }

    // F-13 (eliminar): Inactiva el producto si tiene órdenes activas, lo elimina si no
    @Transactional
    public void delete(UUID id) {
        ProductEntity entity = getActiveProduct(id);
        entity.setStatus(ProductStatus.INACTIVE);
        productRepository.save(entity);
    }

    // Método privado reutilizable — lanza 404 si el producto no existe o está inactivo
    private ProductEntity getActiveProduct(UUID id) {
        return productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún producto con el ID indicado"));
    }
}
