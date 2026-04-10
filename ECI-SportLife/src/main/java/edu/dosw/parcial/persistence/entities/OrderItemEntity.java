package edu.dosw.parcial.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    // Referencia al producto original (puede inactivarse después sin afectar la orden)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    // Snapshot del nombre al momento del checkout — no cambia si el producto se edita
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    // Snapshot del precio al momento del checkout
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
