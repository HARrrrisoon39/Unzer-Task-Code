# Unzer E-Commerce Shop

Spring Boot backend for an online shop with Unzer payment integration (Credit Card, Wero, Open Banking).

- **Architecture:** [`architecture.md`](../architecture.md)
- **Vertical slice:** checkout → stock reservation → Unzer payment → webhook confirmation

---

## Run Locally 

Requires Java 21 and Maven. Uses H2 in-memory DB — no Docker needed.

```bash
cd shop
mvn spring-boot:run
```

- Checkout page: `http://localhost:8080/checkout.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:shopdb`, user: `sa`, password: empty)

---

## Setup

**Step 1 — Start ngrok** (Terminal 1, leave it running)
```bash
ngrok http 8080
# → copy the https URL e.g. https://abc123.ngrok.io
```

**Step 2 — Add your keys** to `src/main/resources/application-local.yml` (gitignored, never committed)
```yaml
unzer:
  private-key: s-priv-xxx       # given at the interview
  public-key: s-pub-xxx         # given at the interview
  webhook-url: https://abc123.ngrok.io      # your ngrok URL
  return-url-base: https://abc123.ngrok.io  # your ngrok URL
```

**Step 3 — Start the app** (Terminal 2)
```powershell
cd shop
mvn spring-boot:run
```

The app auto-loads `application-local.yml` and registers the webhook with Unzer on startup. Open `http://localhost:8080/checkout.html` to demo.

---

## Demo Screenshots

![Checkout Page](docs/images/1.png)
![Payment Flow](docs/images/2.png)
![API Collection (Postman)](docs/images/test.png)

---


## Sandbox Test Cards

| Card | Number | Expiry | CVC |
|---|---|---|---|
| Visa | `4444333322221111` | `03/99` | `123` |
| Mastercard | `5188340000000016` | `12/2025` | `123` |

Never use real card data. Sandbox keys (`s-priv-`) never reach real networks.
