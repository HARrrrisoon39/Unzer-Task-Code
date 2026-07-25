package com.unzer.shop.common;

import com.unzer.payment.Unzer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UnzerConfig {

    private final UnzerProperties properties;

    @Bean
    public Unzer unzer() {
        return new Unzer(properties.getPrivateKey());
    }
}
