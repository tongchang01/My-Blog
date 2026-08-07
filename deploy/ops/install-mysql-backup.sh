#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly INSTALL_ROOT="${MYBLOG_INSTALL_ROOT:-}"

fail() {
  printf 'install-mysql-backup: %s\n' "$*" >&2
  exit 64
}

as_root_path() {
  printf '%s%s' "$INSTALL_ROOT" "$1"
}

[[ "$(id -u)" -eq 0 ]] || fail 'must run as root'

install -m 700 -o root -g root \
  "$SCRIPT_DIR/myblog-mysql-backup" \
  "$(as_root_path /usr/local/sbin/myblog-mysql-backup)"
install -m 644 -o root -g root \
  "$SCRIPT_DIR/systemd/myblog-mysql-backup.service" \
  "$(as_root_path /etc/systemd/system/myblog-mysql-backup.service)"
install -m 644 -o root -g root \
  "$SCRIPT_DIR/systemd/myblog-mysql-backup.timer" \
  "$(as_root_path /etc/systemd/system/myblog-mysql-backup.timer)"

if [[ -z "$INSTALL_ROOT" ]]; then
  systemctl daemon-reload
  systemctl enable --now myblog-mysql-backup.timer
fi

printf 'install-mysql-backup: installed weekly timer\n' >&2
