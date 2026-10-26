#!/bin/sh
set -eu
# Restore an H2 BACKUP archive only while Vynk is stopped.
# Usage: restore-h2.sh BACKUP.zip
[ "$#" -eq 1 ] || { echo "Usage: $0 BACKUP.zip" >&2; exit 2; }
archive=$1
[ -f "$archive" ] || { echo "Backup not found: $archive" >&2; exit 1; }
if rc-service vynk status >/dev/null 2>&1; then
    echo 'Stop Vynk before restoring: rc-service vynk stop' >&2
    exit 1
fi
config=${VYNK_CONFIG:-/etc/vynk/vynk.env}
if [ -r "$config" ]; then set -a; . "$config"; set +a; fi
url=${SPRING_DATASOURCE_URL:-jdbc:h2:file:${VYNK_HOME:-/var/lib/vynk}/vynk}
case "$url" in jdbc:h2:file:*) db=${url#jdbc:h2:file:}; db=${db%%;*};; *) echo 'Only file-based H2 URLs are supported' >&2; exit 2;; esac
case "$db" in /*) :;; *) echo 'Database path must be absolute' >&2; exit 2;; esac
[ -d "$(dirname "$db")" ] || { echo 'Database directory does not exist' >&2; exit 1; }
# Restore to a fresh sibling directory first. Preserve current files as a rollback point.
tmp=$(mktemp -d "$(dirname "$db")/.vynk-restore.XXXXXX")
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
unzip -q "$archive" -d "$tmp"
files=$(find "$tmp" -maxdepth 1 -type f -name '*.mv.db' -print)
[ -n "$files" ] || { echo 'Archive contains no H2 database' >&2; exit 1; }
# H2 BACKUP archives contain the database file; replace only after extraction succeeds.
set -- $files
[ "$#" -eq 1 ] || { echo 'Expected exactly one H2 database in archive' >&2; exit 1; }
restored=$1
old="$db.mv.db"
[ ! -e "$old" ] || cp -p "$old" "$old.pre-restore"
cp "$restored" "$old"
chown vynk:vynk "$old"
chmod 0600 "$old"
echo "Restored database offline. Previous database, if present, is at $old.pre-restore"
