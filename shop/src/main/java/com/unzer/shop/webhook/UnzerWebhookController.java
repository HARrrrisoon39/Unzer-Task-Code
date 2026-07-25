package com.unzer.shop.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unzer.payment.BasePayment;
import com.unzer.payment.Payment;
import com.unzer.payment.Unzer;
import com.unzer.shop.payment.model.PaymentEvent;
import com.unzer.shop.payment.repository.PaymentEventRepository;
import com.unzer.shop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Unzer webhook events.
 *
 * Design rules:
 * 1. Persist the raw payload FIRST — before any processing — so we never lose an event.
 * 2. Always fetch the payment state from Unzer (retrieveUrl) to confirm truth.
 *    Never trust the event name alone.
 * 3. Respond 200 OK within 20 seconds or Unzer will retry.
 * 4. All processing is idempotent — duplicate webhooks are safe.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class UnzerWebhookController {

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentService paymentService;
    private final Unzer unzer;
    private final ObjectMapper objectMapper;

    @PostMapping("/unzer")
    @Transactional
    public ResponseEntity<Void> handleUnzerWebhook(@RequestBody String rawPayload) {
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            String event       = node.path("event").asText();
            String paymentId   = node.path("paymentId").asText();
            String retrieveUrl = node.path("retrieveUrl").asText();

            log.info("Webhook received: event={}, paymentId={}", event, paymentId);

            // Step 1: deduplicate — if already seen, skip processing
            if (paymentEventRepository.existsByUnzerPaymentIdAndEventType(paymentId, event)) {
                log.info("Duplicate webhook ignored: event={}, paymentId={}", event, paymentId);
                return ResponseEntity.ok().build();
            }

            // Step 2: persist raw event for audit trail
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .unzerPaymentId(paymentId)
                    .eventType(event)
                    .rawPayload(rawPayload)
                    .retrieveUrl(retrieveUrl)
                    .processed(false)
                    .build();
            paymentEventRepository.save(paymentEvent);

            // Step 3: fetch authoritative state from Unzer — never trust event name alone
            Payment payment = unzer.fetchPayment(paymentId);
            BasePayment.State state = payment.getPaymentState();
            log.info("Fetched Unzer payment {} state: {}", paymentId, state);

            // Step 4: act on confirmed state
            if (state == BasePayment.State.COMPLETED) {
                paymentService.confirmSuccess(paymentId);
            } else if (state == BasePayment.State.CANCELED) {
                paymentService.confirmFailure(paymentId);
            } else {
                log.debug("Payment {} still in state {} — no action yet", paymentId, state);
            }

            // Step 5: mark processed
            paymentEvent.setProcessed(true);
            paymentEventRepository.save(paymentEvent);

        } catch (Exception e) {
            // Always return 200 — Unzer retries on non-200, risking duplicate processing loops.
            // The unprocessed event row enables manual / scheduled reprocessing.
            log.error("Error processing Unzer webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }
}
