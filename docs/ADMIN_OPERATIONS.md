# Administrator operations

## Provision the first platform administrator

There is intentionally no public API for granting the `admin` role. The first platform administrator must be provisioned from a trusted deployment shell by an operator who has verified the person's identity out of band.

1. Configure the production `DATABASE_URL`, `APP_ENV=production`, and a strong `JWT_SECRET` using the deployment's secret manager.
2. Back up the database and apply the reviewed schema migration: `alembic upgrade head`.
3. Run the provisioning command from the repository root:

   ```bash
   python -m backend.scripts.provision_admin --email admin@example.com --display-name "StayEasy Admin"
   ```

4. Enter the password interactively when prompted. Use a unique password manager-generated value of at least 12 characters. Do not put the password in shell arguments, CI variables, logs, or tickets.
5. Sign in through the app and verify the **Review Owner access requests** screen is accessible. Ensure the account's authentication email/domain and the operator's identity are verified by the deployment administrator before provisioning.

The command refuses to overwrite an existing email, creates an active `admin` account with a scrypt password hash, marks the bootstrap account as verified based on the out-of-band operator process, and writes an audit event. It does not send email. The operator must retain account recovery and secret-rotation procedures separately.

## Owner access review

Only the server-authorized `admin` role can list or decide Owner access requests. Applicants remain customers while pending or rejected. An approval promotes the account server-side and increments `auth_version`, invalidating prior sessions; the applicant must sign in again. Rejection stores a review note and allows a customer to re-submit.

This is an operational bootstrap mechanism, **not** MFA. Enforce MFA through the chosen identity provider or deployment access controls before public production launch. Keep `/auth/dev-token` disabled by setting `APP_ENV=production`.
