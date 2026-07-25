package com.unzer.shop.payment.gateway;

import com.unzer.shop.payment.model.PaymentMethod;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class PaymentRequest {
    UUID orderId;
    BigDecimal amount;
    String currency;
    PaymentMethod method;
    /** typeId from Unzer UI Component — only for CARD; null for redirect methods */
    String typeId;
    String returnUrl;
}
