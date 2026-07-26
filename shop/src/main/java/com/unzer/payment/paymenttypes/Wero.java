package com.unzer.payment.paymenttypes;

import com.unzer.payment.communication.json.ApiObject;

/**
 * Stub for Wero (EPI wallet) — not present in SDK 5.2.0.
 * WeroPaymentGateway is a designed-but-stubbed implementation per assignment scope.
 */
public class Wero extends BasePaymentType {

    public Wero() {}

    @Override
    protected String getResourceUrl() {
        return "types/wero";
    }

    @Override
    public PaymentType map(PaymentType paymentType, ApiObject apiObject) {
        return paymentType;
    }
}
