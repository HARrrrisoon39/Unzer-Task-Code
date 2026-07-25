package com.unzer.shop.payment.gateway;

import com.unzer.payment.BasePayment;
import com.unzer.payment.BaseTransaction;
import com.unzer.payment.Charge;
import com.unzer.payment.Unzer;
import com.unzer.payment.paymenttypes.Wero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;

/**
 * Wero (EPI wallet): charge-only, always redirect-based.
 * Flow: create Wero resource → charge → redirect customer → webhook confirms.
 */
@Slf4j
@Component("WERO")
@RequiredArgsConstructor
public class WeroPaymentGateway implements PaymentGateway {

    private final Unzer unzer;

    @Override
    public PaymentInitResult initiate(PaymentRequest request) {
        try {
            Wero wero = unzer.createPaymentType(new Wero());
            log.debug("Created Wero type resource: {}", wero.getId());

            Charge charge = unzer.charge(
                    request.getAmount(),
                    java.util.Currency.getInstance(request.getCurrency()),
                    wero.getId(),
                    new URL(request.getReturnUrl())
            );

            boolean pending = charge.getStatus() == BaseTransaction.Status.PENDING;
            String redirectUrl = charge.getRedirectUrl() != null
                    ? charge.getRedirectUrl().toString() : null;

            log.info("Wero charge for order {}: paymentId={}, status={}",
                    request.getOrderId(), charge.getPaymentId(), charge.getStatus());

            return PaymentInitResult.builder()
                    .unzerPaymentId(charge.getPaymentId())
                    .unzerTypeId(wero.getId())
                    .redirectUrl(redirectUrl)
                    .pending(pending)
                    .build();

        } catch (Exception e) {
            throw new PaymentGatewayException("Wero charge failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void refund(String unzerPaymentId, String unzerChargeId, BigDecimal amount, String currency) {
        try {
            unzer.cancelCharge(unzerPaymentId, unzerChargeId, amount);
            log.info("Wero refund issued for payment {}, amount={}", unzerPaymentId, amount);
        } catch (Exception e) {
            throw new PaymentGatewayException("Wero refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentState fetchState(String unzerPaymentId) {
        try {
            com.unzer.payment.Payment payment = unzer.fetchPayment(unzerPaymentId);
            return mapState(payment.getPaymentState());
        } catch (Exception e) {
            log.warn("Could not fetch Wero payment state for {}: {}", unzerPaymentId, e.getMessage());
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
