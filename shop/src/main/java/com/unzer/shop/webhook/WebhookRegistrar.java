package com.unzer.shop.webhook;

import com.unzer.payment.Unzer;
import com.unzer.payment.webhook.Webhook;
import com.unzer.payment.webhook.WebhookEventEnum;
import com.unzer.shop.common.UnzerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Registers the webhook endpoint with Unzer on startup.
 * Requires UNZER_WEBHOOK_URL to be set (e.g. your ngrok URL during local dev).
 * Skipped if the URL is blank (useful in tests / CI).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRegistrar implements ApplicationRunner {

    private final Unzer unzer;
    private final UnzerProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String webhookUrl = properties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("UNZER_WEBHOOK_URL not set — skipping webhook registration. " +
                     "Set it to your ngrok URL for local testing.");
            return;
        }

        String fullUrl = webhookUrl + "/api/webhooks/unzer";
        try {
            Webhook webhook = new Webhook(fullUrl, WebhookEventEnum.ALL);
            unzer.registerSingleWebhook(webhook);
            log.info("Unzer webhook registered at: {}", fullUrl);
        } catch (Exception e) {
            // Non-fatal: app still starts; webhooks won't be received until registered
            log.error("Failed to register Unzer webhook at {}: {}", fullUrl, e.getMessage());
        }
    }
}
