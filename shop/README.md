# Unzer E-Commerce Shop

Spring Boot backend for an online shop with Unzer payment integration (Credit Card, Wero, Open Banking).

- **Architecture:** [`architecture.md`](../architecture.md)
- **Vertical slice:** checkout → stock reservation → Unzer payment → webhook confirmation

---

## Run Locally (no payments — just verify the app starts)

Requires Java 21 and Maven. Uses H2 in-memory DB — no Docker needed.

```bash
cd shop
mvn spring-boot:run
```

- Checkout page: `http://localhost:8080/checkout.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:shopdb`, user: `sa`, password: empty)

---

## Interview Setup (full payment flow)

**Step 1 — Start ngrok** (Terminal 1, leave it running)
```bash
ngrok http 8080
# → copy the https URL e.g. https://abc123.ngrok.io
```

**Step 2 — Start the app** (Terminal 2, replace keys and ngrok URL)
```powershell
cd shop
$env:UNZER_PRIVATE_KEY="s-priv-xxx"
$env:UNZER_PUBLIC_KEY="s-pub-xxx"
$env:UNZER_WEBHOOK_URL="https://abc123.ngrok.io"
$env:UNZER_RETURN_URL_BASE="https://abc123.ngrok.io"
mvn spring-boot:run
```

The app auto-registers the webhook with Unzer on startup. Open `http://localhost:8080/checkout.html` to demo.

---

## End-to-End Flow (PowerShell)

```powershell
# 1. Register
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{\"email\":\"test@example.com\",\"password\":\"secret123\"}'
# → {"token":"eyJ..."}

# 2. Browse products
curl http://localhost:8080/api/products

# 3. Add to cart
curl -s -X POST http://localhost:8080/api/cart/items -H "Content-Type: application/json" -H "X-Session-Token: my-session-123" -d '{\"variantId\":\"bbbbbbbb-0000-0000-0000-000000000001\",\"sku\":\"WIDGET-001-STD\",\"name\":\"Premium Widget Standard\",\"quantity\":1,\"unitPrice\":29.99,\"currency\":\"EUR\"}'

# 4. Initiate checkout
curl -s -X POST http://localhost:8080/api/checkout/initiate -H "Content-Type: application/json" -H "X-Session-Token: my-session-123" -d '{\"street\":\"Musterstrasse 1\",\"city\":\"Berlin\",\"country\":\"DE\",\"zip\":\"10115\"}'
# → {"orderId":"<uuid>","total":29.99,"currency":"EUR","publicKey":"s-pub-xxx"}

# 5. Pay with Credit Card (replace <uuid> and s-crd-xxx with real values)
curl -s -X POST http://localhost:8080/api/checkout/pay -H "Content-Type: application/json" -d '{\"orderId\":\"<uuid>\",\"method\":\"CARD\",\"typeId\":\"s-crd-xxx\"}'
# → {"action":"REDIRECT","redirectUrl":"..."} or {"action":"NONE"}

# 6. Simulate webhook (without ngrok)
curl -s -X POST http://localhost:8080/api/webhooks/unzer -H "Content-Type: application/json" -d '{\"event\":\"payment.completed\",\"publicKey\":\"s-pub-xxx\",\"retrieveUrl\":\"https://sbx-api.unzer.com/v1/payments/s-pay-1\",\"paymentId\":\"s-pay-1\"}'

# 7. Check order status
curl http://localhost:8080/api/checkout/status?orderId=<uuid>
# → {"status":"PAID"}
```

---

## Tests

```powershell
cd shop
mvn test
```

Uses H2 — no external services required.

---

## What is Real vs. Stubbed

| Feature | Status |
|---|---|
| Credit Card (3DS) | **Real** — Unzer SDK |
| Wero (redirect) | **Stubbed** — designed, SDK class absent in v5.2.0 |
| Open Banking (redirect) | **Stubbed** — designed, SDK class absent in v5.2.0 |
| Webhook receiver + idempotency | **Real** |
| Stock reservation (optimistic lock) | **Real** |
| Order state machine | **Real** |
| JWT auth + guest checkout | **Real** |
| Stock release on payment failure | **Real** |
| Refunds (Wero / Open Banking) | **Real** — Card needs a capture step first |
| Email notifications | **Stubbed** |
| Product categories / search | **Stubbed** |
| Full order lifecycle (FULFILLING → SHIPPED → COMPLETED) | **Stubbed** — states defined, not wired |
| Admin role enforcement | **Stubbed** — role in JWT, `@PreAuthorize` guards not applied |

---

## Sandbox Test Cards

| Card | Number | Expiry | CVC |
|---|---|---|---|
| Visa | `4444333322221111` | `03/99` | `123` |
| Mastercard | `5188340000000016` | `12/2025` | `123` |

Never use real card data. Sandbox keys (`s-priv-`) never reach real networks.
