#!/bin/sh
set -eu

# Restore an earlier version already retained under /opt/vynk/releases.
# Usage: rollback-release.sh VERSION
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 VERSION" >&2
    exit 2
fi
version=$1
case "$version" in ''|*[!A-Za-z0-9._-]*) echo "Invalid release version: $version" >&2; exit 2;; esac
release_root=${VYNK_RELEASE_ROOT:-/opt/vynk/releases}
frontend_root=${VYNK_FRONTEND_ROOT:-/var/www/vynk}
backend_release="$release_root/$version/vynk.jar"
frontend_release="$release_root/frontend-$version"
[ -f "$backend_release" ] || { echo "Backend release not found: $backend_release" >&2; exit 1; }
[ -d "$frontend_release" ] || { echo "Frontend release not found: $frontend_release" >&2; exit 1; }
backend_link=/opt/vynk/backend/vynk.jar
old_backend=$(readlink "$backend_link" 2>/dev/null || true)
old_frontend=$(readlink "$frontend_root" 2>/dev/null || true)
ln -sfn "$backend_release" "$backend_link"
ln -sfn "$frontend_release" "$frontend_root"
if ! rc-service vynk restart; then
    [ -z "$old_backend" ] || ln -sfn "$old_backend" "$backend_link"
    [ -z "$old_frontend" ] || ln -sfn "$old_frontend" "$frontend_root"
    rc-service vynk restart >/dev/null 2>&1 || true
    echo "Rollback restart failed; restored previous links" >&2
    exit 1
fi
health_url=${VYNK_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}
for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if wget -q -T 3 -O /dev/null "$health_url"; then
        echo "Restored Vynk release $version"
        exit 0
    fi
    sleep 3
done
if [ -n "$old_backend" ]; then ln -sfn "$old_backend" "$backend_link"; fi
if [ -n "$old_frontend" ]; then ln -sfn "$old_frontend" "$frontend_root"; fi
rc-service vynk restart >/dev/null 2>&1 || true
echo "Rollback health check failed: $health_url" >&2
exit 1
