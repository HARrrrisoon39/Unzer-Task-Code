# Unzer E-Commerce Shop

Take-home assignment: full e-commerce backend with Unzer payment integration.

## Deliverables

| File | Description |
|---|---|
| [`architecture.md`](../architecture.md) | Full architecture document with Mermaid diagrams |
| [`shop/`](shop/) | Spring Boot vertical slice: checkout → payment → order confirmation |

---

## Quick Start (Zero dependencies — H2 in-memory DB)

The app runs out of the box with H2. You only need Java 21 and Maven.

```bash
cd shop

# Run without a real Unzer key (payment calls will fail gracefully)
./mvnw spring-boot:run

# Run with your Unzer sandbox keys (provided during the interview)
UNZER_PRIVATE_KEY=s-priv-xxx \
UNZER_PUBLIC_KEY=s-pub-xxx \
./mvnw spring-boot:run
```

Open `http://localhost:8080/checkout.html` for the demo checkout page.

H2 console (inspect DB): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:shopdb`
- Username: `sa` / Password: *(empty)*

---

## Webhook Setup (ngrok — required for Unzer to reach localhost)

Unzer needs a public HTTPS URL to send webhook events. Use ngrok:

```bash
# Terminal 1 — start the app
UNZER_PRIVATE_KEY=s-priv-xxx UNZER_PUBLIC_KEY=s-pub-xxx ./mvnw spring-boot:run

# Terminal 2 — expose localhost
ngrok http 8080
# ngrok gives you: https://abc123.ngrok.io

# Terminal 3 — restart app with webhook URL
UNZER_PRIVATE_KEY=s-priv-xxx \
UNZER_PUBLIC_KEY=s-pub-xxx \
UNZER_WEBHOOK_URL=https://abc123.ngrok.io \
UNZER_RETURN_URL_BASE=https://abc123.ngrok.io \
./mvnw spring-boot:run
```

On startup, the app calls Unzer to register `https://abc123.ngrok.io/api/webhooks/unzer` automatically.

---

## Docker Compose (PostgreSQL)

```bash
cd shop

# Create a .env file (never commit this)
cat > .env <<EOF
UNZER_PRIVATE_KEY=s-priv-xxx
UNZER_PUBLIC_KEY=s-pub-xxx
UNZER_RETURN_URL_BASE=https://abc123.ngrok.io
UNZER_WEBHOOK_URL=https://abc123.ngrok.io
JWT_SECRET=a-long-random-secret-at-least-32-characters
EOF

./mvnw clean package -DskipTests
docker compose up --build
```

---

## Test with cURL

### 1. Register & login
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secret123"}'
# → {"token":"eyJ..."}

TOKEN=eyJ...
```

### 2. Browse products
```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/aaaaaaaa-0000-0000-0000-000000000001/variants
```

### 3. Add to cart
```bash
SESSION=my-test-session-123

curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $SESSION" \
  -d '{
    "variantId": "bbbbbbbb-0000-0000-0000-000000000001",
    "sku": "WIDGET-001-STD",
    "name": "Premium Widget Standard",
    "quantity": 1,
    "unitPrice": 29.99,
    "currency": "EUR"
  }'
```

### 4. Initiate checkout
```bash
curl -s -X POST http://localhost:8080/api/checkout/initiate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $SESSION" \
  -d '{
    "street": "Musterstraße 1",
    "city": "Berlin",
    "country": "DE",
    "zip": "10115"
  }'
# → {"orderId":"<uuid>","total":29.99,"currency":"EUR","publicKey":"s-pub-xxx"}

ORDER_ID=<uuid from above>
```

### 5a. Pay with Credit Card (typeId from UI Component)
```bash
# typeId comes from the browser Unzer UI Component (checkout.html)
# For testing via cURL, you can create a card type directly with the SDK
# (requires PCI scope — only for testing, never in production)

curl -s -X POST http://localhost:8080/api/checkout/pay \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"method\": \"CARD\",
    \"typeId\": \"s-crd-xxx\"
  }"
# → {"action":"REDIRECT","redirectUrl":"https://3ds.bank.com/..."} — follow the redirect
# or {"action":"NONE"} — payment succeeded immediately (no 3DS)
```

### 5b. Pay with Wero
```bash
curl -s -X POST http://localhost:8080/api/checkout/pay \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"method\": \"WERO\"}"
# → {"action":"REDIRECT","redirectUrl":"https://payment.wero.de/..."}
# Open redirectUrl in browser to complete payment
```

### 5c. Pay with Open Banking
```bash
curl -s -X POST http://localhost:8080/api/checkout/pay \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"method\": \"OPEN_BANKING\"}"
# → {"action":"REDIRECT","redirectUrl":"https://..."}
```

### 6. Simulate webhook (local testing without ngrok)
```bash
# After payment completes, Unzer sends this to your webhook URL.
# You can send it manually to test the state machine:
curl -s -X POST http://localhost:8080/api/webhooks/unzer \
  -H "Content-Type: application/json" \
  -d '{
    "event": "payment.completed",
    "publicKey": "s-pub-xxx",
    "retrieveUrl": "https://sbx-api.unzer.com/v1/payments/s-pay-1",
    "paymentId": "s-pay-1"
  }'
```

### 7. Check order status
```bash
curl http://localhost:8080/api/checkout/status?orderId=$ORDER_ID
# → {"orderId":"...","status":"PAID"}
```

---

## Running Tests

```bash
cd shop
./mvnw test
```

Tests use H2 in-memory DB — no external services required.

---

## Project Structure

```
shop/
├── src/main/java/com/unzer/shop/
│   ├── catalog/          # Product & variant read model
│   ├── cart/             # Cart management
│   ├── inventory/        # Stock reservation (optimistic locking)
│   ├── order/            # Order lifecycle state machine
│   ├── payment/
│   │   ├── gateway/      # PaymentGateway interface + Card/Wero/OpenBanking impls
│   │   ├── model/        # Payment, PaymentEvent entities
│   │   ├── service/      # PaymentService (idempotency, confirmSuccess/Failure)
│   │   └── controller/   # CheckoutController
│   ├── customer/         # Auth (JWT), registration, login
│   ├── webhook/          # UnzerWebhookController, WebhookRegistrar
│   └── common/           # Security config, JWT filter, error handler
├── src/main/resources/
│   ├── application.yml
│   ├── application-postgres.yml
│   ├── db/migration/V1__initial_schema.sql
│   └── static/checkout.html
├── Dockerfile
└── docker-compose.yml
```

---

## Key Design Decisions

| Decision | Why |
|---|---|
| **Optimistic locking for stock** | `UPDATE inventory SET available = available - qty WHERE version = ? AND available >= qty` — prevents oversell without held locks |
| **Webhook-first confirmation** | Return URL never confirms payment; only the webhook (after fetching state from Unzer) drives order → PAID transition |
| **Idempotency key per payment** | `orderId:method` as unique key — duplicate payment attempts return existing record, never double-charge |
| **PaymentGateway interface** | Adding a 4th payment method = one new `@Component` class, zero changes to PaymentService |
| **H2 by default** | Zero-setup local run; switch to PostgreSQL with `--spring.profiles.active=postgres` |

See [`architecture.md`](../architecture.md) for the full design.

---

## What is Real vs. Stubbed

| Feature | Status |
|---|---|
| Credit Card checkout (3DS) | **Real** — Unzer SDK, requires sandbox key |
| Wero checkout (redirect) | **Real** — Unzer SDK, requires sandbox key |
| Open Banking checkout (redirect) | **Real** — Unzer SDK, requires sandbox key |
| Webhook receiver | **Real** — persists events, fetches state from Unzer |
| Inventory reservation (optimistic lock) | **Real** |
| Order state machine | **Real** |
| JWT authentication | **Real** |
| Stock release on payment failure | **Real** |
| Idempotency (duplicate webhooks) | **Real** |
| Email notifications | **Stubbed** (interface defined, no SES wiring) |
| Full admin UI | **Stubbed** (endpoints exist, no frontend) |
| Refund endpoint | **Real** (wired to Unzer cancelCharge) |

---

## Sandbox Test Cards

| Card | Number | Expiry | CVC |
|---|---|---|---|
| Visa (generic) | `4444333322221111` | `03/99` | `123` |
| Mastercard | `5188340000000016` | `12/2025` | `123` |

Never use real card data. Sandbox keys (`s-priv-`) never reach real networks.
