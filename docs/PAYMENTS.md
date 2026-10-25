# StayEasy online payments

## Scope and payment provider

StayEasy uses Razorpay Standard Checkout for INR rent/deposit/food/utility/late-fee charges. Razorpay hosts the sensitive payment method fields; StayEasy stores only its own charge, order/payment/refund IDs, exact paise amounts, minimal status, and reconciliation metadata. Do not add PAN/card numbers, CVV, UPI PINs, bank credentials, or raw webhook bodies to StayEasy storage or logs.

The customer cannot submit an amount or mark a payment as paid. The server reads the open charge from the ledger, computes the remaining balance in paise, creates an order with partial payments disabled, and posts a payment ledger entry only after a signature-verified checkout callback is independently checked against Razorpay's payment API as captured, or after a valid signed webhook. Amount, currency and order ID must match the server-created order. The amount is rechecked while locking the charge so concurrent attempts cannot silently under-settle a charge; if an actual second capture exceeds what remains due, it is recorded and flagged captured_unapplied for reconciliation.

## Environment configuration

Set these as deployment secrets, never commit live values:

- RAZORPAY_KEY_ID: public checkout key ID from the Razorpay dashboard.
- RAZORPAY_KEY_SECRET: server-only API and checkout-signature secret.
- RAZORPAY_WEBHOOK_SECRET: a separate webhook signing secret configured on the Razorpay webhook for payment.authorized, payment.captured, payment.failed, refund.processed, and refund.failed.

Use Razorpay test-mode credentials in development/staging. Test mode must be confirmed from the provider dashboard. Production live mode must not be enabled until the legal entity, bank settlement account, account verification, payment disclosures, refund/cancellation terms, privacy notice and support contact have been approved. Configure the webhook URL as https://<api-host>/payments/webhooks/razorpay; use HTTPS only. Webhook signatures are checked against the raw request bytes and the separate webhook secret.

The app returns key_id, checkout_order_id, checkout_amount, and checkout_currency from POST /payments/orders for the Android Checkout SDK. The key ID is public and is not a secret; never embed RAZORPAY_KEY_SECRET or RAZORPAY_WEBHOOK_SECRET in the Android app.

## API flow

1. GET /payments/charges (customer) lists charges that belong to the signed-in tenant and still have an outstanding balance.
2. POST /payments/orders with {"charge_id": <ledger charge id>} and an Idempotency-Key header creates/reuses the server-owned Razorpay order. The request intentionally accepts no client-supplied price.
3. Open Razorpay Standard Checkout in the app using the returned key_id, checkout_order_id, checkout_amount, and checkout_currency.
4. After checkout, POST /payments/verify with payment_attempt_id, razorpay_payment_id, and razorpay_signature. StayEasy validates the checkout HMAC against the stored order ID, then queries Razorpay for the canonical payment status.
5. Razorpay calls POST /payments/webhooks/razorpay. StayEasy validates X-Razorpay-Signature on the unmodified raw body, de-duplicates events, and handles captured, failed, authorized and refund events idempotently.
6. GET /payments/mine returns the tenant's payment history; GET /payments/owner?property_id=<id> returns scoped payment/refund status to property finance operators.
7. POST /payments/{payment_attempt_id}/refund requests a full or partial provider refund. This endpoint is available only to an owner or finance-authorized manager/staff/admin. A refund is not reported as processed until Razorpay confirms it; unknown provider responses are intentionally held for reconciliation.

## Idempotency and accounting safeguards

- Every order request must carry an Idempotency-Key; reusing it for a different charge is rejected.
- A partial unique index allows one active order per charge. Indeterminate API outcomes remain blocked as unknown rather than risking duplicate charges.
- Provider order IDs, payment IDs, refund IDs, client idempotency keys and webhook event IDs are unique at database level.
- The provider callback cannot mark a payment complete by itself: its signature is checked and the canonical provider payment is fetched server-to-server.
- Webhook HMAC is verified over the raw body before JSON processing. Raw webhook bodies are not stored. Event ID + payload SHA-256 are retained for replay detection and audit.
- Captured payments create linked ledger payment records with exact amount_paise. Refund records retain the original capture and separate the refund movement.
- A network timeout after an order/refund request can be indeterminate. Such records are marked unknown; do not retry under a new key until an operator checks the provider dashboard/API.
- A database backup/restore, gateway reconciliation job, webhook retry monitoring, secrets rotation and periodic finance settlement reconciliation are required operational controls before production.

## Required checks before launch

- Add and test the Android Razorpay Standard Checkout SDK integration; do not use a webview or a fake payment success screen.
- Configure test-mode API keys and webhook secret in staging and verify order/create, success, failure, pending/authorized, duplicate webhook, amount mismatch, refund, partial refund, timeout and restore/replay scenarios.
- Verify gateway settlements against StayEasy's payment ledger and the property owner's settlement arrangement. Razorpay collection does not automatically mean StayEasy is authorized to hold or split rental funds; use a provider-approved marketplace/linked-account model if StayEasy will collect money on behalf of property owners.
- Obtain legal/accounting review for security deposit treatment, fee disclosure, refund policy, GST/tax invoices, KYC, payout timing and applicable payment regulations in every launch geography.
- Never call the app production-ready just because the test gateway is working. Production credentials, Android device acceptance, webhook delivery, finance reconciliation, backup/restore and signed release remain separate launch gates.

Provider references:
- Razorpay Orders API: https://razorpay.com/docs/api/orders/create/
- Checkout payment signature verification: https://razorpay.com/docs/payments/server-integration/standard/integration-steps/#step-5-verify-payment-signature
- Webhook signature validation: https://razorpay.com/docs/webhooks/validate-test/
