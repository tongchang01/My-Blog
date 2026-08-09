#!/usr/bin/env bash
set -euo pipefail

readonly REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
readonly CADDYFILE="$REPO_ROOT/deploy/web/Caddyfile"
readonly CADDY_IMAGE="$(awk '/^FROM caddy:/ { print $2 }' "$REPO_ROOT/deploy/web/Dockerfile")"
readonly TEMP_DIR="$(mktemp -d)"
readonly SUFFIX="${GITHUB_RUN_ID:-local}-$$"
readonly NETWORK="myblog-caddy-security-$SUFFIX"
readonly API_CONTAINER="myblog-caddy-api-$SUFFIX"
readonly WEB_CONTAINER="myblog-caddy-web-$SUFFIX"

cleanup() {
  local status=$?
  set +e
  if [[ "$status" -ne 0 ]]; then
    docker logs "$API_CONTAINER" >&2
    docker logs "$WEB_CONTAINER" >&2
  fi
  docker rm --force "$WEB_CONTAINER" "$API_CONTAINER" >/dev/null 2>&1
  docker network rm "$NETWORK" >/dev/null 2>&1
  rm -rf -- "$TEMP_DIR"
  exit "$status"
}
trap cleanup EXIT

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

require_header() {
  local headers="$1"
  local expected="$2"
  grep --fixed-strings --ignore-case --quiet -- "$expected" <<<"$headers" ||
    fail "missing header: $expected"
}

reject_header() {
  local headers="$1"
  local name="$2"
  if grep --extended-regexp --ignore-case --quiet "^${name}:" <<<"$headers"; then
    fail "unexpected header: $name"
  fi
}

request_headers() {
  local host="$1"
  local path="$2"
  curl --fail --silent --show-error --dump-header - --output /dev/null \
    --header "Host: $host" \
    "http://127.0.0.1:${HOST_PORT}${path}" | tr -d '\r'
}

command -v docker >/dev/null || fail 'docker is required'
command -v curl >/dev/null || fail 'curl is required'
[[ -n "$CADDY_IMAGE" ]] || fail 'cannot resolve pinned Caddy image'

mkdir -p "$TEMP_DIR/blog" "$TEMP_DIR/admin"
printf '<!doctype html><title>blog</title>\n' >"$TEMP_DIR/blog/index.html"
printf '<!doctype html><title>admin</title>\n' >"$TEMP_DIR/admin/index.html"
cat >"$TEMP_DIR/api.Caddyfile" <<'CADDY'
:8080 {
	header {
		Content-Type "application/json"
		X-Content-Type-Options "nosniff"
		X-Frame-Options "DENY"
	}
	respond /api/* `{"source":"upstream"}` 200
}
CADDY

docker network create "$NETWORK" >/dev/null
docker run --detach --name "$API_CONTAINER" --network "$NETWORK" --network-alias api \
  --mount "type=bind,source=$TEMP_DIR/api.Caddyfile,target=/etc/caddy/Caddyfile,readonly" \
  "$CADDY_IMAGE" >/dev/null
docker run --detach --name "$WEB_CONTAINER" --network "$NETWORK" \
  --publish 127.0.0.1::8080 \
  --env BLOG_HOST=http://blog.test:8080 \
  --env WWW_HOST=http://www.test:8080 \
  --env ADMIN_HOST=http://admin.test:8080 \
  --mount "type=bind,source=$CADDYFILE,target=/etc/caddy/Caddyfile,readonly" \
  --mount "type=bind,source=$TEMP_DIR/blog,target=/srv/blog,readonly" \
  --mount "type=bind,source=$TEMP_DIR/admin,target=/srv/admin,readonly" \
  "$CADDY_IMAGE" >/dev/null

HOST_PORT="$(docker port "$WEB_CONTAINER" 8080/tcp | sed -n 's/.*:\([0-9][0-9]*\)$/\1/p' | head -n 1)"
readonly HOST_PORT
[[ -n "$HOST_PORT" ]] || fail 'cannot resolve published Caddy port'

ready=false
for _ in {1..30}; do
  if curl --fail --silent --output /dev/null --header 'Host: blog.test' \
    "http://127.0.0.1:${HOST_PORT}/"; then
    ready=true
    break
  fi
  sleep 1
done
[[ "$ready" == true ]] || fail 'Caddy did not become ready'

for host in blog.test www.test admin.test; do
  headers="$(request_headers "$host" /)"
  require_header "$headers" 'Strict-Transport-Security: max-age=31536000'
  require_header "$headers" 'X-Content-Type-Options: nosniff'
  require_header "$headers" 'Referrer-Policy: strict-origin-when-cross-origin'
  require_header "$headers" 'X-Frame-Options: DENY'
  require_header "$headers" 'Permissions-Policy: camera=(), microphone=(), geolocation=()'
done

api_headers="$(request_headers blog.test /api/contract)"
require_header "$api_headers" 'Strict-Transport-Security: max-age=31536000'
require_header "$api_headers" 'X-Content-Type-Options: nosniff'
require_header "$api_headers" 'X-Frame-Options: DENY'
reject_header "$api_headers" 'Referrer-Policy'
reject_header "$api_headers" 'Permissions-Policy'

api_body="$(curl --fail --silent --show-error --header 'Host: blog.test' \
  "http://127.0.0.1:${HOST_PORT}/api/contract")"
[[ "$api_body" == '{"source":"upstream"}' ]] || fail "unexpected proxy body: $api_body"

printf 'Caddy security header integration: PASS\n'
