package com.unzer.shop.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variant")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "product_id") private UUID productId;
    private String sku;
    private String name;
    @Column(precision = 19, scale = 4) private BigDecimal price;
    private String currency;
}
