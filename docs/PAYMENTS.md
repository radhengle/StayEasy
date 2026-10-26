# StayEasy online payments

## Scope and payment provider

StayEasy uses Razorpay Standard Checkout for INR rent/deposit/food/utility/late-fee charges. Razorpay hosts the sensitive payment method fields; StayEasy stores only its own charge, order/payment/refund IDs, exact paise amounts, minimal status, and reconciliation metadata. Do not add PAN/card numbers, CVV, UPI PINs, bank credentials, or raw webhook bodies to StayEasy storage or logs.

The customer cannot submit an amount or mark a payment as paid. The server reads the open charge from the ledger, computes the remaining balance in paise, creates an order with partial payments disabled, and posts a payment ledger entry only after a signature-verified checkout callback is independently checked against Razorpay's payment API as captured, or after a valid signed webhook. Amount, currency and order ID must match the server-created order. The amount is rechecked while locking the charge so concurrent attempts cannot silently under-settle a charge; if an actual second capture exceeds what remains due, it is recorded and flagged captured_unapplied for reconciliation.

## Environment configuration

Set these as deployment secrets, never commit live values:

- RAZORPAY_KEY_ID: public checkout key ID from the Razorpay dashboard.
- RAZORPAY_KEY_SECRET: server-only API and checkout-signature secret.
- RAZORPAY_WEBHOOK_SECRET: a separate webhook signing secret configured on the Razorpay webhook for payment.authorized, payment.captured, payment.failed, refund.processed, refund.failed, Route transfer events, and `settlement.processed`.

Use Razorpay test-mode credentials in development/staging. Test mode must be confirmed from the provider dashboard. Production live mode must not be enabled until the legal entity, bank settlement account, account verification, payment disclosures, refund/cancellation terms, privacy notice and support contact have been approved. Configure the webhook URL as https://<api-host>/payments/webhooks/razorpay; use HTTPS only. Webhook signatures are checked against the raw request bytes and the separate webhook secret.

The app returns key_id, checkout_order_id, checkout_amount, and checkout_currency from POST /payments/orders for the Android Checkout SDK. The key ID is public and is not a secret; never embed RAZORPAY_KEY_SECRET or RAZORPAY_WEBHOOK_SECRET in the Android app.

## Readiness diagnostics

An owner/admin can request `GET /owner/integrations/status` or use **API connection & sync → Integration readiness** in Android. The response reports whether checkout and webhook secrets are configured and provides a test/live mode hint from the key ID prefix; it does not return any key or secret values. This is configuration visibility only. A successful order, verified capture, signed webhook and refund flow still require Razorpay test-mode acceptance against a configured staging webhook.

## Marketplace settlement to property owners

**Tenant checkout and owner settlement are separate steps.** A payment can be captured by the platform merchant account without reaching the PG owner. StayEasy supports an explicit Razorpay Route settlement mode for the marketplace flow; the route must remain disabled until Razorpay has enabled the product for the merchant account.

- `PAYMENT_SETTLEMENT_MODE=owner_route` selects owner-routed orders. Staging/production defaults to this mode.
- `RAZORPAY_ROUTE_ENABLED=true` is an operator-controlled feature gate. It must only be enabled after Route approval and successful test-mode onboarding.
- Each property owner must have a Razorpay Route Linked Account created and KYC-approved with Razorpay. StayEasy stores only the linked-account identifier and its status; it does not collect or persist owner bank details, VPA credentials, or KYC documents.
- An administrator can attach a provider-onboarded account with `PUT /payments/admin/owner-settlement-accounts/{owner_id}` and body `{"linked_account_id":"acc_...","status":"active"}`. The active status is accepted only while Route is enabled. `GET /payments/admin/owner-settlement-accounts` lists links for reconciliation; owners can view their own readiness through `GET /payments/owner/settlement-status`.
- In owner-route mode, order creation fails closed if Route is disabled or the property's owner has no active linked account. The server adds one transfer for the server-calculated payable amount to the order, verifies the returned transfer ID/recipient/amount/currency before giving Checkout to Android, and reconciles ambiguous order creation by fetching the canonical order. It never sends the tenant to checkout when the owner-transfer details cannot be verified.
- The signed webhook endpoint must subscribe to Route transfer events including `transfer.created`, `transfer.processed`, `transfer.failed`, and `transfer.reversed`. Owner payment summaries expose transfer state separately from the tenant's captured-payment state; `processed` is not labelled bank-settled unless the provider explicitly reports settlement.
- Refunds for routed payments use Razorpay's `reverse_all=true` path, which requests the linked-account transfer be reversed before returning funds to the tenant. This is supported for a single linked account; changes to multi-owner split payments require explicit partial-reversal allocation logic and are not enabled by this implementation.

For a local non-routed development flow, use `PAYMENT_SETTLEMENT_MODE=platform`; that is a payment into the StayEasy merchant account, **not** a promise of automatic payment to the owner. A production platform-collection model must be explicitly approved and must not be inferred from a missing Route configuration.

The provider dashboard/KYC work and real transactions cannot be completed by source code alone. The present Route order transfers the full server-calculated rent amount to the owner; this assumes the platform has enough available balance to cover Razorpay gateway fees, Route transfer fees and tax. Razorpay documents that insufficient balance can fail a transfer; agree who bears fees, confirm actual merchant pricing, and test the full rent flow before launch. After enabling Route, run a staging acceptance cycle that verifies the created order's `transfers` collection, a captured test payment, transfer webhooks, settlement reporting, partial/full refund reversal, duplicate webhook replay and retry/recovery before any live keys are used.

## API flow

1. GET /payments/charges (customer) lists charges that belong to the signed-in tenant and still have an outstanding balance.
2. POST /payments/orders with {"charge_id": <ledger charge id>} and an Idempotency-Key header creates/reuses the server-owned Razorpay order. The request intentionally accepts no client-supplied price.
3. Open Razorpay Standard Checkout in the app using the returned key_id, checkout_order_id, checkout_amount, and checkout_currency.
4. After checkout, POST /payments/verify with payment_attempt_id, razorpay_payment_id, and razorpay_signature. StayEasy validates the checkout HMAC against the stored order ID, then queries Razorpay for the canonical payment status. If the app was interrupted or provider verification was temporarily unavailable, POST /payments/{payment_attempt_id}/refresh reconciles the existing order/payment without charging again.
5. Razorpay calls POST /payments/webhooks/razorpay. StayEasy validates X-Razorpay-Signature on the unmodified raw body, de-duplicates events, and handles captured, failed, authorized and refund events idempotently.
6. GET /payments/mine returns the tenant's payment history; GET /payments/owner/summary?property_id=<id> returns scoped payment/refund status to property finance operators.
7. POST /payments/{payment_attempt_id}/refund requests a full or partial provider refund. This endpoint is available only to an owner or finance-authorized manager/staff/admin. A refund is not reported as processed until Razorpay confirms it; unknown provider responses are intentionally held for reconciliation.

## Idempotency and accounting safeguards

- Every order request must carry an Idempotency-Key; reusing it for a different charge is rejected.
- A partial unique index allows one active order per charge. Each provider order uses a stable unique receipt derived from the stored payment-attempt ID.
- If order creation times out before its response is persisted, retrying with the same Idempotency-Key—or retrying the same charge after an app restart—triggers a server-side Razorpay order lookup by receipt. StayEasy relaunches Checkout only when exactly one order is found with the expected amount/currency and `created` state.
- No result, ambiguous provider lookup, multiple matches, mismatched amount/currency, or a non-created order leaves the attempt blocked for reconciliation. Do not issue a new Idempotency-Key to get around an unresolved attempt; use the existing payment-status flow or finance/provider reconciliation.
- Provider order IDs, payment IDs, refund IDs, client idempotency keys and webhook event IDs are unique at database level.
- The provider callback cannot mark a payment complete by itself: its signature is checked and the canonical provider payment is fetched server-to-server.
- Webhook HMAC is verified over the raw body before JSON processing. Raw webhook bodies are not stored. Event ID + payload SHA-256 are retained for replay detection and audit.
- Captured payments create linked ledger payment records with exact amount_paise. Provider-confirmed refunds create linked ledger reversal rows with exact amount_paise, and the charge balance is recalculated so a refund cannot leave a charge falsely marked paid.
- A network timeout after an order/refund request can be indeterminate. Such records are marked unknown; do not retry under a new key until an operator checks the provider dashboard/API.
- A database backup/restore, gateway reconciliation job, webhook retry monitoring, secrets rotation and periodic finance settlement reconciliation are required operational controls before production.

## Required checks before launch

- The native Android Razorpay Standard Checkout SDK is wired to the tenant Rent & Payments screen; test it on real devices against a staging account. Do not use a webview or fake payment success screen.
- Configure test-mode API keys and webhook secret in staging and verify order/create, success, failure, pending/authorized, duplicate webhook, amount mismatch, refund, partial refund, timeout and restore/replay scenarios.
- For owner-routed collections, get Razorpay Route enabled, onboard and KYC-approve a test linked account, link it through the admin endpoint, and verify the order includes exactly the expected transfer. Confirm transfer state and settlement separately from payment capture; verify full/partial refunds reverse the transfer as provider-supported.
- Verify gateway settlements against StayEasy's payment ledger and the property owner's settlement arrangement. Razorpay collection does not automatically mean StayEasy is authorized to hold or split rental funds; use a provider-approved marketplace/linked-account model if StayEasy will collect money on behalf of property owners.
- Obtain legal/accounting review for security deposit treatment, fee disclosure, refund policy, GST/tax invoices, KYC, payout timing and applicable payment regulations in every launch geography.
- Never call the app production-ready just because the test gateway is working. Production credentials, Android device acceptance, webhook delivery, finance reconciliation, backup/restore and signed release remain separate launch gates.

Provider references:
- Razorpay Orders API: https://razorpay.com/docs/api/orders/create/
- Checkout payment signature verification: https://razorpay.com/docs/payments/server-integration/standard/integration-steps/#step-5-verify-payment-signature
- Webhook signature validation: https://razorpay.com/docs/webhooks/validate-test/
