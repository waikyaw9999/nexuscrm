#!/usr/bin/env bash
# HTTP healthcheck using bash /dev/tcp so the JRE image needs no wget, curl, or apk/apt.
set -euo pipefail

host="${HEALTH_HOST:-127.0.0.1}"
port="${HEALTH_PORT:-8080}"
path="${HEALTH_PATH:-/actuator/health}"

exec 3<>"/dev/tcp/${host}/${port}"
printf 'GET %s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n' "${path}" "${host}" >&3
response="$(timeout 4 cat <&3 || true)"
exec 3<&- 3>&- || true

[[ "${response}" == *"HTTP/1."*" 200"* ]] || exit 1
# Spring Actuator default JSON is {"status":"UP"} (with or without spaces).
[[ "${response}" == *'"status":"UP"'* || "${response}" == *'"status" : "UP"'* ]] || exit 1
exit 0
