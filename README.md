# Unzer E-Commerce Shop

> A Spring Boot e-commerce backend with full Unzer payment integration — Credit Card, Wero, and Open Banking — built as a clean vertical-slice demo.

---

## What's inside

| Layer | Technology |
|---|---|
| Backend | Java 21 · Spring Boot 3 · Maven |
| Database | H2 in-memory (dev) · Flyway migrations |
| Payments | Unzer SDK — Credit Card, Wero, Open Banking |
| Webhooks | ngrok tunnel → Unzer event confirmation |
| CI/CD | GitHub Actions → Docker → AWS ECR → ECS |

---

## Payment flow

```
checkout → stock reservation → Unzer payment → webhook confirmation
```

---

## Quick links

- **Setup & run:** [`shop/README.md`](shop/README.md)
- **Architecture deep-dive:** [`architecture.md`](architecture.md)

---

## Sandbox test cards

| Card | Number | Expiry | CVC |
|---|---|---|---|
| Visa | `4444333322221111` | `03/99` | `123` |
| Mastercard | `5188340000000016` | `12/2025` | `123` |

> Never use real card data. Sandbox keys (`s-priv-`) never reach real networks.
