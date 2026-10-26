#!/bin/sh
set -eu
# Create a transaction-consistent online H2 backup using H2's BACKUP command.
# Usage: backup-h2.sh [destination.zip]
config=${VYNK_CONFIG:-/etc/vynk/vynk.env}
if [ -r "$config" ]; then set -a; . "$config"; set +a; fi
url=${SPRING_DATASOURCE_URL:-jdbc:h2:file:${VYNK_HOME:-/var/lib/vynk}/vynk}
user=${SPRING_DATASOURCE_USERNAME:-sa}
password=${SPRING_DATASOURCE_PASSWORD:-}
destination=${1:-/var/backups/vynk/vynk-$(date -u +%Y%m%dT%H%M%SZ).zip}
case "$url" in jdbc:h2:file:*) db=${url#jdbc:h2:file:}; db=${db%%;*};; *) echo 'Only file-based H2 URLs are supported' >&2; exit 2;; esac
case "$destination" in /*) :;; *) echo 'Backup destination must be absolute' >&2; exit 2;; esac
jar=/opt/vynk/backend/vynk.jar
h2=$(unzip -Z1 "$jar" | grep '^BOOT-INF/lib/h2-.*\.jar$' | head -n 1)
[ -n "$h2" ] || { echo 'H2 driver not found in backend JAR' >&2; exit 1; }
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
unzip -p "$jar" "$h2" > "$tmp/h2.jar"
mkdir -p "$(dirname "$destination")"
chmod 0700 "$(dirname "$destination")"
# H2 BACKUP produces the archive atomically after capturing a consistent snapshot.
printf 'BACKUP TO %s;\n' "'$(printf '%s' "$destination" | sed "s/'/''/g")'" | \
  java -cp "$tmp/h2.jar" org.h2.tools.Shell -url "$url" -user "$user" -password "$password" -sql "BACKUP TO '$(printf '%s' "$destination" | sed "s/'/''/g")'"
chmod 0600 "$destination"
echo "H2 backup created: $destination"
