#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
readonly JAVA_HOME="${JAVA_HOME:?JAVA_HOME is required}"
readonly KEYTOOL="$JAVA_HOME/bin/keytool"
mkdir -p "$SCRIPT_DIR/../../target"
readonly TARGET_DIR="$(cd -- "$SCRIPT_DIR/../../target" && pwd)"
readonly TEST_DIR="$(mktemp -d "$TARGET_DIR/java-trust-test.XXXXXX")"
[[ "$TEST_DIR" == "$TARGET_DIR"/java-trust-test.* ]] || exit 1
trap 'rm -rf -- "$TEST_DIR"' EXIT

"$JAVA_HOME/bin/javac" -d "$SCRIPT_DIR" "$SCRIPT_DIR/CheckCerts.java"
readonly STORE="$TEST_DIR/cacerts"

fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
expect_failure() {
  if "$@" >"$TEST_DIR/expected-failure.log" 2>&1; then
    fail 'Expected a nonzero exit status'
  fi
}
add_root() {
  # 每次生成离线测试证书，不依赖开发机或上游是否已移除真实根证书。
  "$KEYTOOL" -genkeypair -noprompt -alias "$1" -dname "CN=$2" \
    -keyalg EC -groupname secp256r1 -ext bc=ca:true -validity 2 \
    -keystore "$TEST_DIR/fixture.p12" -storepass changeit -keypass changeit >/dev/null 2>&1
  "$KEYTOOL" -exportcert -alias "$1" -keystore "$TEST_DIR/fixture.p12" \
    -storepass changeit -file "$TEST_DIR/root.cer" >/dev/null 2>&1
  "$KEYTOOL" -importcert -noprompt -alias "$1" -keystore "$STORE" \
    -storepass changeit -file "$TEST_DIR/root.cer" >/dev/null 2>&1
}

add_root 'unrelated-root' 'MyBlog Test Root'
add_root 'emsignrootcag1 [jdk]' 'emSign Root CA - G1'
add_root 'emsignrootcag2 [jdk]' 'emSign Root CA - G2'
add_root 'emsigneccrootcag3 [jdk]' 'emSign ECC Root CA - G3'
add_root 'comodoaaaca [jdk]' 'AAA Certificate Services'
add_root 'cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' 'emSign ECC Root CA - G3'

expect_failure "$JAVA_HOME/bin/java" "-Djavax.net.ssl.trustStore=$STORE" \
  -Djavax.net.ssl.trustStorePassword=changeit -cp "$SCRIPT_DIR" CheckCerts
"$KEYTOOL" -exportcert -alias unrelated-root -keystore "$STORE" -storepass changeit \
  -file "$TEST_DIR/normal-before.cer" >/dev/null 2>&1
bash "$SCRIPT_DIR/distrust-emudhra.sh" "$STORE"
"$KEYTOOL" -exportcert -alias unrelated-root -keystore "$STORE" -storepass changeit \
  -file "$TEST_DIR/normal-after.cer" >/dev/null 2>&1
cmp "$TEST_DIR/normal-before.cer" "$TEST_DIR/normal-after.cer" || fail 'Unrelated root changed'

cp "$STORE" "$TEST_DIR/once"
bash "$SCRIPT_DIR/distrust-emudhra.sh" "$STORE"
cmp "$STORE" "$TEST_DIR/once" || fail 'Mitigation is not idempotent'

# 已修复/重复执行不改写文件；缺失、损坏和错误密码必须报错。
expect_failure bash "$SCRIPT_DIR/distrust-emudhra.sh" "$TEST_DIR/missing"
printf 'invalid keystore' >"$TEST_DIR/corrupt"
expect_failure bash "$SCRIPT_DIR/distrust-emudhra.sh" "$TEST_DIR/corrupt"
cp "$STORE" "$TEST_DIR/wrong-password"
"$KEYTOOL" -storepasswd -keystore "$TEST_DIR/wrong-password" -storepass changeit \
  -new different-password >/dev/null 2>&1
expect_failure bash "$SCRIPT_DIR/distrust-emudhra.sh" "$TEST_DIR/wrong-password"

# 将问题证书换成未识别别名，官方检查器仍必须阻止构建成功。
add_root 'unexpected-root-alias' 'emSign ECC Root CA - G3'
expect_failure bash "$SCRIPT_DIR/distrust-emudhra.sh" "$STORE"
grep -F 'BAD ROOT:' "$TEST_DIR/expected-failure.log" >/dev/null || fail 'Unknown alias escaped detection'
printf 'Java trust-store contract: PASS\n'
