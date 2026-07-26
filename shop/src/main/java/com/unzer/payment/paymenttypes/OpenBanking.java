package com.unzer.payment.paymenttypes;

import com.unzer.payment.communication.json.ApiObject;

/**
 * Stub for OpenBanking (instant bank transfer) — not present in SDK 5.2.0.
 * OpenBankingPaymentGateway is a designed-but-stubbed implementation per assignment scope.
 */
public class OpenBanking extends BasePaymentType {

    private String countryCode;

    public OpenBanking(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    @Override
    protected String getResourceUrl() {
        return "types/open-banking";
    }

    @Override
    public PaymentType map(PaymentType paymentType, ApiObject apiObject) {
        return paymentType;
    }
}
