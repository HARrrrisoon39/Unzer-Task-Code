package com.unzer.shop.payment.service;

import com.unzer.shop.common.UnzerProperties;
import com.unzer.shop.inventory.service.InventoryOrderFacade;
import com.unzer.shop.order.model.Order;
import com.unzer.shop.order.model.OrderStatus;
import com.unzer.shop.order.service.OrderService;
import com.unzer.shop.payment.gateway.*;
import com.unzer.shop.payment.model.*;
import com.unzer.shop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final InventoryOrderFacade inventoryOrderFacade;
    private final UnzerProperties unzerProperties;

    // Spring injects beans by name matching PaymentMethod enum names: CARD, WERO, OPEN_BANKING
    private final Map<String, PaymentGateway> gateways;

    /**
     * Initiates a payment for an order.
     * Idempotent: re-calling with the same orderId+method returns the existing payment
     * record rather than double-charging Unzer.
     */
    @Transactional
    public Payment initiate(UUID orderId, PaymentMethod method, String typeId) {
        String idempotencyKey = orderId + ":" + method;

        return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            Order order = orderService.get(orderId);

            PaymentGateway gateway = resolveGateway(method);
            String returnUrl = unzerProperties.getReturnUrlBase()
                    + "/api/checkout/return?orderId=" + orderId;

            PaymentRequest request = PaymentRequest.builder()
                    .orderId(orderId)
                    .amount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .method(method)
                    .typeId(typeId)
                    .returnUrl(returnUrl)
                    .build();

            PaymentInitResult result = gateway.initiate(request);

            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .unzerPaymentId(result.getUnzerPaymentId())
                    .unzerTypeId(result.getUnzerTypeId())
                    .method(method)
                    .status(result.isPending() ? PaymentStatus.AWAITING_CONFIRMATION : PaymentStatus.SUCCEEDED)
                    .amount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .idempotencyKey(idempotencyKey)
                    .redirectUrl(result.getRedirectUrl())
                    .build();

            Payment saved = paymentRepository.save(payment);
            orderService.transition(orderId, OrderStatus.AWAITING_PAYMENT,
                    "Payment initiated via " + method);

            log.info("Payment {} initiated for order {} via {}, pending={}",
                    saved.getId(), orderId, method, result.isPending());
            return saved;
        });
    }

    /**
     * Called by the webhook handler after confirming payment state from Unzer.
     * Idempotent: safe to call multiple times for the same payment.
     */
    @Transactional
    public void confirmSuccess(String unzerPaymentId) {
        Payment payment = paymentRepository.findByUnzerPaymentId(unzerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for unzerPaymentId: " + unzerPaymentId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.debug("Payment {} already confirmed — skipping (idempotent)", unzerPaymentId);
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        orderService.transition(payment.getOrderId(), OrderStatus.PAID,
                "Payment confirmed via webhook");

        // Permanently decrement reserved stock — confirms the reservation
        inventoryOrderFacade.confirmReservationsByOrder(payment.getOrderId());

        log.info("Payment {} confirmed for order {}", unzerPaymentId, payment.getOrderId());
    }

    @Transactional
    public void confirmFailure(String unzerPaymentId) {
        Payment payment = paymentRepository.findByUnzerPaymentId(unzerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for unzerPaymentId: " + unzerPaymentId));

        if (payment.getStatus() == PaymentStatus.FAILED) return; // idempotent

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        orderService.transition(payment.getOrderId(), OrderStatus.PAYMENT_FAILED,
                "Payment failed — notified via webhook");

        // Return reserved stock to available
        inventoryOrderFacade.releaseReservationsByOrder(payment.getOrderId());

        log.info("Payment {} failed for order {}", unzerPaymentId, payment.getOrderId());
    }

    @Transactional
    public void refund(UUID orderId, BigDecimal amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment for order: " + orderId));

        PaymentGateway gateway = resolveGateway(payment.getMethod());
        gateway.refund(payment.getUnzerPaymentId(), "s-chg-1", amount, payment.getCurrency());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        orderService.transition(orderId, OrderStatus.REFUNDED, "Refund processed");
    }

    private PaymentGateway resolveGateway(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method.name());
        if (gateway == null) {
            throw new IllegalArgumentException("No gateway registered for method: " + method);
        }
        return gateway;
    }
}
