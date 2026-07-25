package com.unzer.shop.payment.gateway;

import java.math.BigDecimal;

/**
 * Unified abstraction over all Unzer payment methods.
 * Adding a fourth method = one new implementation of this interface.
 */
public interface PaymentGateway {

    /** Initiates a payment. Returns redirectUrl if customer action is needed, null for immediate success. */
    PaymentInitResult initiate(PaymentRequest request);

    /** Refunds a previously charged payment (full or partial). */
    void refund(String unzerPaymentId, String unzerChargeId, BigDecimal amount, String currency);

    /** Fetches current payment state from Unzer (used for polling reconciliation). */
    PaymentState fetchState(String unzerPaymentId);
}
