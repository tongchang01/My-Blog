#!/usr/bin/env bash
set -euo pipefail

readonly SUT_DIR="${SUT_DIR:?SUT_DIR is required}"
readonly RELEASE="$SUT_DIR/myblog-release"
readonly ENTRYPOINT="$SUT_DIR/myblog-cd-entrypoint"
readonly SHA="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_status() {
  local expected="$1"
  shift
  set +e
  "$@"
  local actual="$?"
  set -e
  [[ "$actual" -eq "$expected" ]] || fail "expected exit $expected, got $actual: $*"
}

assert_contains() {
  local needle="$1"
  local file="$2"
  grep -Fqx -- "$needle" "$file" >/dev/null || fail "missing call: $needle"
}

prepare_fake_commands() {
  local fakebin="$1"
  local calls="$2"
  mkdir -p "$fakebin"

  cat >"$fakebin/git" <<'SCRIPT'
#!/usr/bin/env bash
printf 'git %s\n' "$*" >>"$CALLS"
SCRIPT

  cat >"$fakebin/docker" <<'SCRIPT'
#!/usr/bin/env bash
printf 'docker %s\n' "$*" >>"$CALLS"
SCRIPT

cat >"$fakebin/runuser" <<'SCRIPT'
#!/usr/bin/env bash
if [[ "$1" == "-u" ]]; then shift 2; fi
if [[ "$1" == "--" ]]; then shift; fi
"$@"
SCRIPT

  cat >"$fakebin/id" <<'SCRIPT'
#!/usr/bin/env bash
if [[ "${1:-}" == "-u" ]]; then
  printf '0\n'
  exit 0
fi
/usr/bin/id "$@"
SCRIPT

  cat >"$fakebin/install" <<'SCRIPT'
#!/usr/bin/env bash
source="${@: -2:1}"
destination="${@: -1}"
cp "$source" "$destination"
SCRIPT

  chmod +x "$fakebin"/*
}

run_release_contract() {
  local temp
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' RETURN

  local root="$temp/repo"
  local runtime="$temp/runtime.env"
  local calls="$temp/calls"
  local fakebin="$temp/bin"
  mkdir -p "$root"
  printf 'IMAGE_TAG=old\nOTHER=preserve\n' >"$runtime"
  chmod 600 "$runtime"
  : >"$calls"
  prepare_fake_commands "$fakebin" "$calls"

  local common_env=(
    "PATH=$fakebin:$PATH"
    "CALLS=$calls"
    "MYBLOG_DEPLOY_ROOT=$root"
    "MYBLOG_RUNTIME_ENV=$runtime"
    "MYBLOG_COMPOSE_BIN=docker compose"
  )

  assert_status 64 env "${common_env[@]}" "$RELEASE" not-a-sha
  [[ ! -s "$calls" ]] || fail "invalid SHA invoked commands"

  env "${common_env[@]}" "$RELEASE" "$SHA"

  assert_contains "git -C $root fetch --prune origin" "$calls"
  assert_contains "git -C $root merge-base --is-ancestor $SHA origin/main" "$calls"
  assert_contains "git -C $root checkout --detach $SHA" "$calls"
  assert_contains "docker compose --env-file $runtime config --quiet" "$calls"
  assert_contains "docker compose --env-file $runtime pull" "$calls"
  assert_contains "docker compose --env-file $runtime up -d --wait --wait-timeout 180" "$calls"
  assert_contains "docker exec myblog-v2-api-1 curl --fail --silent http://127.0.0.1:8080/actuator/health" "$calls"
  [[ "$(grep '^IMAGE_TAG=' "$runtime")" == "IMAGE_TAG=$SHA" ]] || fail "IMAGE_TAG was not updated"
  [[ "$(grep '^OTHER=' "$runtime")" == "OTHER=preserve" ]] || fail "runtime.env content changed unexpectedly"
}

run_entrypoint_contract() {
  local temp
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' RETURN

  local fakebin="$temp/bin"
  local calls="$temp/calls"
  mkdir -p "$fakebin"
  : >"$calls"

  cat >"$fakebin/sudo" <<'SCRIPT'
#!/usr/bin/env bash
printf 'sudo %s\n' "$*" >>"$CALLS"
SCRIPT
  chmod +x "$fakebin/sudo"

  assert_status 64 env PATH="$fakebin:$PATH" CALLS="$calls" SSH_ORIGINAL_COMMAND='' "$ENTRYPOINT"
  assert_status 64 env PATH="$fakebin:$PATH" CALLS="$calls" SSH_ORIGINAL_COMMAND="deploy short" "$ENTRYPOINT"
  assert_status 64 env PATH="$fakebin:$PATH" CALLS="$calls" SSH_ORIGINAL_COMMAND="shell $SHA" "$ENTRYPOINT"
  assert_status 64 env PATH="$fakebin:$PATH" CALLS="$calls" SSH_ORIGINAL_COMMAND="deploy $SHA extra" "$ENTRYPOINT"
  env PATH="$fakebin:$PATH" CALLS="$calls" SSH_ORIGINAL_COMMAND="deploy $SHA" "$ENTRYPOINT"
  assert_contains "sudo /usr/local/sbin/myblog-release $SHA" "$calls"
}

[[ -x "$RELEASE" ]] || fail "missing executable: $RELEASE"
[[ -x "$ENTRYPOINT" ]] || fail "missing executable: $ENTRYPOINT"

run_release_contract
run_entrypoint_contract
printf 'release contract: PASS\n'
