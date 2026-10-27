# Vynk operator handoff

## Local setup and checks

- Requirements: Node.js 22+, JDK 25, and Git.
- Start the API with `cd backend && ./mvnw spring-boot:run` and the UI with `cd frontend && npm ci && npm start`.
- Frontend checks: `cd frontend && npm run format:check && npm run build && npm run test:ci`.
- Backend verification: `cd backend && ./mvnw -B -ntp verify`.
- The backend uses a file-backed H2 database. Treat its data directory as persistent and private; do not run multiple application instances against the same database.

## Deployment

1. Build the backend JAR and frontend browser distribution using the commands above; transfer them securely to the Alpine host.
2. Install/configure the OpenRC service and NGINX/TLS using `deploy/alpine/`. Copy `deploy/alpine/env/vynk.env.example` to `/etc/vynk/vynk.env`, set a unique strong database password, and restrict the file to root (`chmod 0600`). Keep API docs disabled in production.
3. Ensure the backend binds only to loopback, NGINX serves the frontend and proxies same-origin `/api/` traffic, and SSE proxy buffering is disabled. Do not expose port 8080 publicly.
4. Deploy with `deploy/alpine/scripts/deploy-release.sh VERSION /path/to/vynk.jar /path/to/browser-dist`. Confirm the service health check and verify the site through HTTPS. Retain the prior release until the new one is confirmed.

## Backup and recovery

- Create a consistent online H2 backup with `deploy/alpine/scripts/backup-h2.sh [absolute-destination.zip]`. Store backups outside the application/data directory with restrictive permissions, and maintain a separate off-host copy.
- Restore only while the service is stopped: `rc-service vynk stop`, then `deploy/alpine/scripts/restore-h2.sh /absolute/path/to/backup.zip`. The script preserves the prior database as `.pre-restore`; verify the restored service and data before resuming normal operation.
- Before applying a release that changes the schema, take and verify a backup. Keep database recovery separate from application rollback; rolling back a JAR does not roll back database migrations.

## Security and operations

- Protect TLS certificates, `/etc/vynk/vynk.env`, database files, and backups. Never commit secrets or use real personal data in demo fixtures.
- Use HTTPS in production; session cookies and CSRF protection depend on the same-origin deployment setup. Do not enable API docs on a public deployment.
- Check service/NGINX logs and the local Actuator health endpoint after deployment. Avoid logging credentials, session cookies, or private user content.
- Deployment is manual; CI verifies code quality/builds and does not publish or deploy releases. Target-host Alpine/TLS/recovery verification remains an operator release responsibility.
