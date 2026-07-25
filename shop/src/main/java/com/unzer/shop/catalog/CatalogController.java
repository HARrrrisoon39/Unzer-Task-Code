package com.unzer.shop.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class CatalogController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    @GetMapping
    public ResponseEntity<List<Product>> listProducts(
            @RequestParam(required = false) String search) {
        List<Product> products = (search != null && !search.isBlank())
                ? productRepository.findByNameContainingIgnoreCaseAndActiveTrue(search)
                : productRepository.findByActiveTrue();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID productId) {
        return productRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getVariants(@PathVariable UUID productId) {
        return ResponseEntity.ok(variantRepository.findByProductId(productId));
    }
}
