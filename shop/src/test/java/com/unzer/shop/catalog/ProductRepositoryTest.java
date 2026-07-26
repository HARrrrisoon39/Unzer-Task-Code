package com.unzer.shop.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @Test
    void savesAndFindsActiveProduct() {
        productRepository.save(Product.builder().sku("A1").name("Widget").active(true).build());

        assertThat(productRepository.findByActiveTrue()).hasSize(1);
    }
}
