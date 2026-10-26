#!/bin/sh
set -eu

# Run as root after installing the packaged backend JAR at /opt/vynk/backend/vynk.jar.
getent group vynk >/dev/null 2>&1 || addgroup -S vynk
getent passwd vynk >/dev/null 2>&1 || adduser -S -D -H -h /var/lib/vynk -s /sbin/nologin -G vynk vynk
install -d -o root -g root -m 0755 /opt/vynk/backend
install -d -o root -g root -m 0750 /etc/vynk
install -d -o vynk -g vynk -m 0750 /var/lib/vynk
install -m 0755 "$(dirname "$0")/../openrc/vynk" /etc/init.d/vynk
install -m 0644 "$(dirname "$0")/../openrc/vynk.confd" /etc/conf.d/vynk
if [ ! -e /etc/vynk/vynk.env ]; then
    install -m 0600 "$(dirname "$0")/../env/vynk.env.example" /etc/vynk/vynk.env
    chown root:root /etc/vynk/vynk.env
    echo 'Review /etc/vynk/vynk.env and replace the example database password before starting.' >&2
fi
rc-update add vynk default
