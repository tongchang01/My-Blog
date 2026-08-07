#!/usr/bin/env bash
set -euo pipefail

readonly SUT_DIR="${SUT_DIR:?SUT_DIR is required}"
readonly BACKUP="$SUT_DIR/myblog-mysql-backup"
readonly INSTALLER="$SUT_DIR/install-mysql-backup.sh"

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

prepare_fake_commands() {
  local fakebin="$1"
  local calls="$2"
  mkdir -p "$fakebin"

  cat >"$fakebin/id" <<'SCRIPT'
#!/usr/bin/env bash
[[ "${1:-}" == '-u' ]] && { printf '0\n'; exit 0; }
/usr/bin/id "$@"
SCRIPT

  cat >"$fakebin/docker" <<'SCRIPT'
#!/usr/bin/env bash
printf 'docker %s\n' "$*" >>"$CALLS"
[[ "$1" == 'exec' ]] && printf 'CREATE TABLE backup_contract_test (id INT);\n'
SCRIPT

  cat >"$fakebin/aws" <<'SCRIPT'
#!/usr/bin/env bash
printf 'aws %s\n' "$*" >>"$CALLS"
SCRIPT

  cat >"$fakebin/date" <<'SCRIPT'
#!/usr/bin/env bash
case "$*" in
  *'%Y/%m'*) printf '2026/08\n' ;;
  *) printf '20260807T033000Z\n' ;;
esac
SCRIPT

  cat >"$fakebin/install" <<'SCRIPT'
#!/usr/bin/env bash
directory=false
mode=''
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    -d) directory=true; shift ;;
    -m) mode="$2"; shift 2 ;;
    -o|-g) shift 2 ;;
    -*) shift ;;
    *) break ;;
  esac
done
if [[ "$directory" == true ]]; then
  mkdir -p "$@"
  [[ -z "$mode" ]] || chmod "$mode" "$@"
  exit 0
fi
source="${@: -2:1}"
destination="${@: -1}"
mkdir -p "$(dirname "$destination")"
cp "$source" "$destination"
[[ -z "$mode" ]] || chmod "$mode" "$destination"
SCRIPT

  chmod +x "$fakebin"/*
}

run_backup_contract() {
  local temp
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' RETURN

  local fakebin="$temp/bin"
  local calls="$temp/calls"
  local runtime="$temp/runtime.env"
  local bad_runtime="$temp/bad-runtime.env"
  local backup_root="$temp/backup"
  : >"$calls"
  printf 'MYBLOG_STORAGE_S3_BUCKET=tyb-blog-s3\nOTHER=preserve\n' >"$runtime"
  chmod 600 "$runtime"
  printf 'MYBLOG_STORAGE_S3_BUCKET=tyb-blog-s3\n' >"$bad_runtime"
  chmod 640 "$bad_runtime"
  prepare_fake_commands "$fakebin" "$calls"

  assert_status 64 env PATH="$fakebin:$PATH" MYBLOG_RUNTIME_ENV="$bad_runtime" MYBLOG_BACKUP_ROOT="$backup_root" "$BACKUP"
  env PATH="$fakebin:$PATH" CALLS="$calls" MYBLOG_RUNTIME_ENV="$runtime" MYBLOG_BACKUP_ROOT="$backup_root" "$BACKUP"

  grep -Fqx 'docker exec myblog-v2-mysql-1 sh -c exec mysqldump --protocol=socket --user=root --password="$MYSQL_ROOT_PASSWORD" --single-transaction --routines --events --databases "$MYSQL_DATABASE"' "$calls" \
    || fail 'mysqldump command is missing'
  grep -F 'aws s3 cp --only-show-errors ' "$calls" | grep -F 's3://tyb-blog-s3/recovery/mysql/2026/08/myblog-v2-20260807T033000Z.sql.gz' >/dev/null \
    || fail 'archive upload target is wrong'
  grep -F 'aws s3 cp --only-show-errors ' "$calls" | grep -F 's3://tyb-blog-s3/recovery/mysql/2026/08/myblog-v2-20260807T033000Z.sql.gz.sha256' >/dev/null \
    || fail 'checksum upload target is wrong'
  [[ -z "$(find "$backup_root" -type f -print -quit)" ]] || fail 'temporary backup was not removed'
}

run_install_contract() {
  local temp
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' RETURN

  local fakebin="$temp/bin"
  local calls="$temp/calls"
  local install_root="$temp/install-root"
  : >"$calls"
  prepare_fake_commands "$fakebin" "$calls"

  env PATH="$fakebin:$PATH" CALLS="$calls" MYBLOG_INSTALL_ROOT="$install_root" "$INSTALLER"

  [[ "$(stat -c '%a' "$install_root/usr/local/sbin/myblog-mysql-backup")" == 700 ]] \
    || fail 'backup script mode is not 700'
  [[ -f "$install_root/etc/systemd/system/myblog-mysql-backup.service" ]] \
    || fail 'service unit is missing'
  [[ -f "$install_root/etc/systemd/system/myblog-mysql-backup.timer" ]] \
    || fail 'timer unit is missing'
}

[[ -x "$BACKUP" ]] || fail "missing executable: $BACKUP"
[[ -x "$INSTALLER" ]] || fail "missing executable: $INSTALLER"

run_backup_contract
run_install_contract
printf 'mysql backup contract: PASS\n'
