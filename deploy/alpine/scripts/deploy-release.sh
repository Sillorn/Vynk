#!/bin/sh
set -eu

# Deploy a versioned backend JAR and Angular dist directory. Run as root:
# deploy-release.sh VERSION BACKEND_JAR FRONTEND_DIST
# Optional: VYNK_HEALTH_URL, VYNK_FRONTEND_ROOT, VYNK_RELEASE_ROOT.
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 VERSION BACKEND_JAR FRONTEND_DIST" >&2
    exit 2
fi
version=$1
jar=$2
dist=$3
case "$version" in ''|*[!A-Za-z0-9._-]*) echo "Invalid release version: $version" >&2; exit 2;; esac
[ -f "$jar" ] || { echo "Backend JAR not found: $jar" >&2; exit 1; }
[ -d "$dist" ] || { echo "Frontend dist directory not found: $dist" >&2; exit 1; }

release_root=${VYNK_RELEASE_ROOT:-/opt/vynk/releases}
frontend_root=${VYNK_FRONTEND_ROOT:-/var/www/vynk}
health_url=${VYNK_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}
backend_release="$release_root/$version"
frontend_release="$release_root/frontend-$version"
backend_link=/opt/vynk/backend/vynk.jar
frontend_link="$frontend_root"

[ ! -e "$backend_release" ] || { echo "Backend release already exists: $backend_release" >&2; exit 1; }
[ ! -e "$frontend_release" ] || { echo "Frontend release already exists: $frontend_release" >&2; exit 1; }
install -d -o root -g root -m 0755 "$release_root" "$(dirname "$backend_link")" "$(dirname "$frontend_link")"
install -d -o root -g root -m 0755 "$backend_release" "$frontend_release"
install -m 0644 "$jar" "$backend_release/vynk.jar"
cp -R "$dist"/. "$frontend_release"/

old_backend=$(readlink "$backend_link" 2>/dev/null || true)
old_frontend=$(readlink "$frontend_link" 2>/dev/null || true)
rollback() {
    echo "Deployment failed; restoring previous release" >&2
    if [ -n "$old_backend" ]; then ln -sfn "$old_backend" "$backend_link"; else rm -f "$backend_link"; fi
    if [ -n "$old_frontend" ]; then ln -sfn "$old_frontend" "$frontend_link"; else rm -f "$frontend_link"; fi
    rc-service vynk restart >/dev/null 2>&1 || true
}
ln -sfn "$backend_release/vynk.jar" "$backend_link"
ln -sfn "$frontend_release" "$frontend_link"
if ! rc-service vynk restart; then rollback; exit 1; fi
healthy=false
for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if wget -q -T 3 -O /dev/null "$health_url"; then healthy=true; break; fi
    sleep 3
done
if [ "$healthy" != true ]; then
    echo "Health check failed: $health_url" >&2
    rollback
    exit 1
fi
echo "Deployed Vynk release $version"
