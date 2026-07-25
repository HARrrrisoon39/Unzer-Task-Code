package com.unzer.shop.payment.gateway;

import com.unzer.payment.Authorization;
import com.unzer.payment.BaseTransaction;
import com.unzer.payment.Unzer;
import com.unzer.payment.BasePayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;

/**
 * Credit Card via Unzer: authorize (reserves funds), then charge on fulfillment.
 * 3DS may make the authorize PENDING — check getRedirectUrl() for non-null.
 * Raw card data never reaches this server — typeId is the token from Unzer UI Components.
 */
@Slf4j
@Component("CARD")
@RequiredArgsConstructor
public class CardPaymentGateway implements PaymentGateway {

    private final Unzer unzer;

    @Override
    public PaymentInitResult initiate(PaymentRequest request) {
        try {
            Authorization auth = unzer.authorize(
                    request.getAmount(),
                    java.util.Currency.getInstance(request.getCurrency()),
                    request.getTypeId(),
                    new URL(request.getReturnUrl())
            );

            boolean pending = auth.getStatus() == BaseTransaction.Status.PENDING;
            String redirectUrl = (auth.getRedirectUrl() != null)
                    ? auth.getRedirectUrl().toString() : null;

            log.info("Card authorize for order {}: paymentId={}, status={}",
                    request.getOrderId(), auth.getPaymentId(), auth.getStatus());

            return PaymentInitResult.builder()
                    .unzerPaymentId(auth.getPaymentId())
                    .unzerTypeId(request.getTypeId())
                    .redirectUrl(redirectUrl)
                    .pending(pending)
                    .build();

        } catch (Exception e) {
            throw new PaymentGatewayException("Card authorize failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void refund(String unzerPaymentId, String unzerChargeId, BigDecimal amount, String currency) {
        try {
            unzer.cancelCharge(unzerPaymentId, unzerChargeId, amount);
            log.info("Card refund issued for payment {}, amount={}", unzerPaymentId, amount);
        } catch (Exception e) {
            throw new PaymentGatewayException("Card refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentState fetchState(String unzerPaymentId) {
        try {
            com.unzer.payment.Payment payment = unzer.fetchPayment(unzerPaymentId);
            return mapState(payment.getPaymentState());
        } catch (Exception e) {
            log.warn("Could not fetch card payment state for {}: {}", unzerPaymentId, e.getMessage());
            return PaymentState.PENDING;
        }
    }

    private PaymentState mapState(BasePayment.State state) {
        if (state == null) return PaymentState.PENDING;
        return switch (state) {
            case COMPLETED -> PaymentState.COMPLETED;
            case CANCELED  -> PaymentState.CANCELLED;
            default        -> PaymentState.PENDING;
        };
    }
}
