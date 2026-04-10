package edu.dosw.parcial.persistence.mappers;

import edu.dosw.parcial.controller.dtos.request.ProductRequest;
import edu.dosw.parcial.controller.dtos.response.ProductResponse;
import edu.dosw.parcial.persistence.entities.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Convierte la entidad JPA al DTO de respuesta que recibe el cliente
    ProductResponse toResponse(ProductEntity entity);

    // Convierte el DTO de request a entidad JPA para persistir en base de datos
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ProductEntity toEntity(ProductRequest request);

    // Actualiza los campos de una entidad existente sin reemplazarla (usado en PUT)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget ProductEntity entity);
}
