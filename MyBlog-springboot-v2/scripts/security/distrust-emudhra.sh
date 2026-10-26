#!/usr/bin/env bash
set -euo pipefail

# 官方临时处置：https://ops.java/emudhra（2026-10-24）。仅用于构建镜像或 CI JDK。
# shortcut: 官方镜像尚未更新；确认构建、运行和 CI 的官方 JDK 均通过 CheckCerts 后移除此脚本。
readonly JAVA_HOME="${JAVA_HOME:?JAVA_HOME is required}"
readonly STORE="${1:-$JAVA_HOME/lib/security/cacerts}"
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
readonly KEYTOOL="$JAVA_HOME/bin/keytool"

[[ -f "$STORE" ]] || { echo "Missing trust store: $STORE" >&2; exit 1; }
[[ -f "$SCRIPT_DIR/CheckCerts.class" ]] || { echo 'Compile CheckCerts.java before applying mitigation' >&2; exit 1; }
# 先验证可读性和密码，不能把损坏/无权限误判成“证书不存在”。
"$KEYTOOL" -list -keystore "$STORE" -storepass changeit >/dev/null

# Oracle 与 Temurin 的别名不同；删除后用官方检查器按证书主体检查，未知别名不得静默放行。
for alias in \
  'emsignrootcag1 [jdk]' \
  'emsignrootcag2 [jdk]' \
  'emsigneccrootcag3 [jdk]' \
  'comodoaaaca [jdk]' \
  'cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' \
  'cn_emsign_root_ca__g2,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' \
  'cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' \
  'cn_aaa_certificate_services,o_comodo_ca_limited,l_salford,st_greater_manchester,c_gb [jdk]'; do
  if "$KEYTOOL" -list -keystore "$STORE" -storepass changeit -alias "$alias" >/dev/null 2>&1; then
    "$KEYTOOL" -delete -keystore "$STORE" -storepass changeit -alias "$alias"
    printf 'Removed root: %s\n' "$alias"
  fi
done

"$JAVA_HOME/bin/java" "-Djavax.net.ssl.trustStore=$STORE" -Djavax.net.ssl.trustStorePassword=changeit \
  -cp "$SCRIPT_DIR" CheckCerts
