#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly INSTALL_ROOT="${MYBLOG_INSTALL_ROOT:-}"
readonly DEPLOY_ROOT="${MYBLOG_DEPLOY_ROOT:-/opt/myblog-v2}"

fail() {
  printf 'install-github-cd: %s\n' "$*" >&2
  exit 64
}

as_root_path() {
  printf '%s%s' "$INSTALL_ROOT" "$1"
}

require_root() {
  [[ "$(id -u)" -eq 0 ]] || fail 'must run as root'
}

require_public_key() {
  local key_file="$1"
  [[ -f "$key_file" ]] || fail 'public key file is missing'
  local key
  key="$(<"$key_file")"
  [[ "$key" != *$'\n'* ]] || fail 'public key file must contain exactly one line'
  [[ "$key" =~ ^(ssh-ed25519|ssh-rsa)[[:space:]][^[:space:]]+([[:space:]].*)?$ ]] \
    || fail 'public key must be one ssh-ed25519 or ssh-rsa line'
  printf '%s' "$key"
}

[[ "$#" -eq 2 && "$1" == '--public-key-file' ]] || fail 'usage: install-github-cd.sh --public-key-file <path>'
require_root
PUBLIC_KEY="$(require_public_key "$2")"

if ! id deploy >/dev/null 2>&1; then
  useradd --create-home --shell /bin/bash deploy
fi

DEPLOY_HOME="$(as_root_path /home/deploy)"
DEPLOY_SSH="$DEPLOY_HOME/.ssh"
AUTHORIZED_KEYS="$DEPLOY_SSH/authorized_keys"
SUDOERS_FILE="$(as_root_path /etc/sudoers.d/myblog-release)"
RELEASE_TARGET="$(as_root_path /usr/local/sbin/myblog-release)"
ENTRYPOINT_TARGET="$(as_root_path /usr/local/sbin/myblog-cd-entrypoint)"

install -d -m 700 -o deploy -g deploy "$DEPLOY_SSH"
temporary_key="$(mktemp)"
trap 'rm -f "$temporary_key"' EXIT
printf 'restrict,command="/usr/local/sbin/myblog-cd-entrypoint" %s\n' "$PUBLIC_KEY" >"$temporary_key"
install -m 600 -o deploy -g deploy "$temporary_key" "$AUTHORIZED_KEYS"

install -m 755 -o root -g root "$SCRIPT_DIR/myblog-release" "$RELEASE_TARGET"
install -m 755 -o root -g root "$SCRIPT_DIR/myblog-cd-entrypoint" "$ENTRYPOINT_TARGET"

temporary_sudoers="$(mktemp)"
trap 'rm -f "$temporary_key" "$temporary_sudoers"' EXIT
printf '%s\n' 'deploy ALL=(root) NOPASSWD: /usr/local/sbin/myblog-release *' >"$temporary_sudoers"
install -m 440 -o root -g root "$temporary_sudoers" "$SUDOERS_FILE"
visudo -cf "$SUDOERS_FILE"

mkdir -p "$DEPLOY_ROOT"
chown -R deploy:deploy "$DEPLOY_ROOT"
printf 'install-github-cd: installed deploy entrypoint\n' >&2
