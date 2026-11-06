# Vynk operations runbook (Alpine)

Run host commands as root unless noted. Deployment and recovery are manual; CI never deploys. Keep database backups and release artifacts off-host as well as on-host. Never put credentials or personal data in tickets or logs.

## Routine health checks

- Check service state: `rc-service vynk status`.
- Check the local health endpoint: `wget -qO- http://127.0.0.1:8080/actuator/health` (expect `UP`). Do not expose port 8080 publicly.
- Check NGINX: `rc-service nginx status`; verify the site and same-origin API over HTTPS from a client. Confirm SSE remains unbuffered in NGINX configuration.
- Review service logs (`/var/log/vynk/` when configured) and NGINX logs for startup, proxy, and TLS errors. Do not copy secrets, cookies, or private user content into incident reports.
- Confirm `/etc/vynk/vynk.env` is root-owned and mode 0600, TLS certificates are valid, and API docs are disabled in production.

## Backup

1. Ensure `/var/backups/vynk` is on protected storage with sufficient space. The backup script sets the destination directory to mode 0700 and archive to 0600; restrict its parent and any off-host copy too.
2. Run `deploy/alpine/scripts/backup-h2.sh` (or pass an absolute destination). The online H2 `BACKUP` command creates a consistent snapshot.
3. Confirm the command succeeded and the archive exists and is non-empty; copy it to separate off-host storage. Record the UTC time and release/schema version without recording user data.
4. Before every schema-changing release, take a fresh backup. Periodically perform the restore procedure below in a safe environment to verify backups.

## Deploy and application rollback

1. Build and verify artifacts using the repository CI-parity commands. Transfer them securely; verify their origin and checksums through the release process used by your organization.
2. Retain the current release and a known-good previous release. Deploy as root: `deploy/alpine/scripts/deploy-release.sh VERSION /path/to/vynk.jar /path/to/browser-dist`. The script switches versioned backend/frontend links, restarts OpenRC, checks local health for up to 30 seconds, and restores the prior links if restart/health fails.
3. Verify `rc-service vynk status`, the local health endpoint, and the public HTTPS site/API. Exercise login/session and a representative SSE connection after release; inspect logs.
4. If the release is unhealthy, use `deploy/alpine/scripts/rollback-release.sh VERSION` with the retained prior version, then repeat health and HTTPS checks. Preserve logs and the failed release for investigation.
5. Application rollback only changes code. It does **not** reverse Flyway migrations or restore data. If a schema migration is incompatible, stop and assess before attempting database recovery; do not assume an old JAR can safely use a migrated database.

## Database restore (data-loss operation)

1. Declare an incident/maintenance window and identify the exact backup and expected recovery point. Preserve the current database files and backup archive before proceeding.
2. Stop Vynk: `rc-service vynk stop`; verify it is stopped. Restore must never run against an active database.
3. Run `deploy/alpine/scripts/restore-h2.sh /absolute/path/to/backup.zip`. It extracts to a temporary sibling location, validates that exactly one H2 `.mv.db` file is present, preserves the old database as `.pre-restore`, and installs the restored file with service ownership/permissions.
4. Start the service: `rc-service vynk start`. Check service state and `http://127.0.0.1:8080/actuator/health`; inspect logs for Flyway/H2 errors. Verify expected records and essential HTTPS application flows with an authorized test account.
5. If the restored DB fails validation, stop the service immediately and preserve evidence. Do not repeatedly overwrite database files. The previous file is `$SPRING_DATASOURCE_URL`'s resolved database path plus `.mv.db.pre-restore` (default `/var/lib/vynk/vynk.mv.db.pre-restore`); coordinate a controlled recovery with a known-good backup. Escalate if uncertain.
6. Resume normal traffic only after checks pass. Record backup used, restore time, recovery point, and outcome without sensitive user information.

## Incident response

- For an outage, first check service/NGINX state, local health, disk space, and recent logs; avoid destructive actions until evidence and current data are preserved.
- For suspected credential/session compromise, restrict access at the edge, protect logs and configuration, and rotate exposed credentials/secrets. Do not share session cookies. Restore data only when corruption or loss is established.
- For TLS/proxy issues, validate NGINX configuration before reload and verify HTTPS, secure cookie behavior, API proxying, and SSE after recovery.
- Keep a concise incident record: UTC start/end, affected release, observable symptoms, actions, backup/recovery point, and follow-up items.
