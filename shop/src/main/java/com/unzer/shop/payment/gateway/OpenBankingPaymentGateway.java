package com.unzer.shop.payment.gateway;

import com.unzer.payment.BasePayment;
import com.unzer.payment.BaseTransaction;
import com.unzer.payment.Charge;
import com.unzer.payment.Unzer;
import com.unzer.payment.paymenttypes.OpenBanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;

/**
 * Open Banking (instant bank transfer): charge-only, always redirect-based.
 * Conceptually identical to Wero; settlement may take up to 1 business day.
 */
@Slf4j
@Component("OPEN_BANKING")
@RequiredArgsConstructor
public class OpenBankingPaymentGateway implements PaymentGateway {

    private final Unzer unzer;

    @Override
    public PaymentInitResult initiate(PaymentRequest request) {
        try {
            OpenBanking openBanking = unzer.createPaymentType(new OpenBanking("DE"));
            log.debug("Created OpenBanking type resource: {}", openBanking.getId());

            Charge charge = unzer.charge(
                    request.getAmount(),
                    java.util.Currency.getInstance(request.getCurrency()),
                    openBanking.getId(),
                    new URL(request.getReturnUrl())
            );

            boolean pending = charge.getStatus() == BaseTransaction.Status.PENDING;
            String redirectUrl = charge.getRedirectUrl() != null
                    ? charge.getRedirectUrl().toString() : null;

            log.info("OpenBanking charge for order {}: paymentId={}, status={}",
                    request.getOrderId(), charge.getPaymentId(), charge.getStatus());

            return PaymentInitResult.builder()
                    .unzerPaymentId(charge.getPaymentId())
                    .unzerTypeId(openBanking.getId())
                    .redirectUrl(redirectUrl)
                    .pending(pending)
                    .build();

        } catch (Exception e) {
            throw new PaymentGatewayException("OpenBanking charge failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void refund(String unzerPaymentId, String unzerChargeId, BigDecimal amount, String currency) {
        try {
            unzer.cancelCharge(unzerPaymentId, unzerChargeId, amount);
            log.info("OpenBanking refund issued for payment {}, amount={}", unzerPaymentId, amount);
        } catch (Exception e) {
            throw new PaymentGatewayException("OpenBanking refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentState fetchState(String unzerPaymentId) {
        try {
            com.unzer.payment.Payment payment = unzer.fetchPayment(unzerPaymentId);
            return mapState(payment.getPaymentState());
        } catch (Exception e) {
            log.warn("Could not fetch OpenBanking payment state for {}: {}", unzerPaymentId, e.getMessage());
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
