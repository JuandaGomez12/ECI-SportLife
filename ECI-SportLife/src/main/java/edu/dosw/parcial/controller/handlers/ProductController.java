package edu.dosw.parcial.controller.handlers;

import edu.dosw.parcial.controller.dtos.request.ProductRequest;
import edu.dosw.parcial.controller.dtos.response.PageResponse;
import edu.dosw.parcial.controller.dtos.response.ProductResponse;
import edu.dosw.parcial.core.models.Category;
import edu.dosw.parcial.core.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gestión y consulta del catálogo de productos")
public class ProductController {

    private final ProductService productService;

    // F-03: Lista todos los productos activos con paginación
    @Operation(summary = "Listar productos activos")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return ResponseEntity.ok(productService.findAll(page, size));
    }

    // F-04: Filtra productos activos por categoría
    @Operation(summary = "Filtrar por categoría")
    @GetMapping("/category/{category}")
    public ResponseEntity<PageResponse<ProductResponse>> listByCategory(
            @PathVariable Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.findByCategory(category, page, size));
    }

    // F-05: Busca productos activos cuyo nombre contenga el término
    @Operation(summary = "Buscar por nombre")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductResponse>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.findByName(name, page, size));
    }

    // F-06: Retorna el detalle completo de un producto por su ID
    @Operation(summary = "Ver detalle de producto")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    // F-13 (crear): Solo accesible por ADMIN — crea un nuevo producto en el catálogo
    @Operation(summary = "Crear producto", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    // F-13 (actualizar): Solo accesible por ADMIN — reemplaza todos los campos del producto
    @Operation(summary = "Actualizar producto", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    // F-13 (eliminar): Solo accesible por ADMIN — inactiva el producto en lugar de borrarlo físicamente
    @Operation(summary = "Eliminar producto", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
