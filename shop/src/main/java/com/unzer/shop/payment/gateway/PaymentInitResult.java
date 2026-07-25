package com.unzer.shop.payment.gateway;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentInitResult {
    String unzerPaymentId;
    String unzerTypeId;
    /** Null means payment succeeded immediately (no redirect needed). */
    String redirectUrl;
    boolean pending;
}
