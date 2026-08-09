#!/usr/bin/env bash
set -euo pipefail

readonly CADDYFILE="${1:?usage: security-headers-contract-test.sh <Caddyfile>}"

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

require_once() {
  local needle="$1"
  local actual
  actual="$(grep -Fc -- "$needle" "$CADDYFILE" || true)"
  [[ "$actual" -eq 1 ]] || fail "expected once, found $actual: $needle"
}

[[ -f "$CADDYFILE" ]] || fail "missing Caddyfile: $CADDYFILE"

require_once '(security_headers) {'
require_once '?X-Content-Type-Options "nosniff"'
require_once '?Referrer-Policy "strict-origin-when-cross-origin"'
require_once '?X-Frame-Options "SAMEORIGIN"'
require_once '?Permissions-Policy "camera=(), microphone=(), geolocation=()"'

imports="$(grep -Fc -- 'import security_headers' "$CADDYFILE" || true)"
[[ "$imports" -eq 2 ]] || fail "expected two security header imports, found $imports"

printf 'security headers contract: PASS\n'
