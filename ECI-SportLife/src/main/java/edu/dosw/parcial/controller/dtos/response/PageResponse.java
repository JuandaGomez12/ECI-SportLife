package edu.dosw.parcial.controller.dtos.response;

import org.springframework.data.domain.Page;

import java.util.List;

// Wrapper genérico de paginación reutilizable en cualquier endpoint que devuelva listas
public record PageResponse<T>(
        List<T> content,
        long totalItems,
        int totalPages,
        int currentPage
) {
    // Construye el PageResponse a partir de un Page de Spring Data
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber()
        );
    }
}
