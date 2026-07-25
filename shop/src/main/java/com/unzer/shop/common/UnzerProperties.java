package com.unzer.shop.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "unzer")
public class UnzerProperties {
    private String privateKey;
    private String publicKey;
    private String returnUrlBase;
    private String webhookUrl;
}
