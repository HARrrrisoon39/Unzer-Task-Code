package com.unzer.shop.payment.gateway;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentInitResult {
    String unzerPaymentId;
    String unzerTypeId;
    /** Non-null for charge-based methods (Wero, OpenBanking); null for Card (authorize flow). */
    String chargeId;
    /** Null means payment succeeded immediately (no redirect needed). */
    String redirectUrl;
    boolean pending;
}
