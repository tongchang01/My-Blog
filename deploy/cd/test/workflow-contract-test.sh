#!/usr/bin/env bash
set -euo pipefail

readonly WORKFLOW="${1:?usage: workflow-contract-test.sh <images.yml>}"
readonly REPO_ROOT="$(cd -- "$(dirname -- "$WORKFLOW")/../.." && pwd)"
readonly CD_DOCUMENT="$REPO_ROOT/docs/handbook/ops/github-ssh-cd.md"

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

require_line() {
  local needle="$1"
  grep -F -- "$needle" "$WORKFLOW" >/dev/null || fail "missing workflow contract: $needle"
}

require_line 'deploy:'
require_line 'needs: publish'
require_line "if: github.ref == 'refs/heads/main'"
require_line 'environment: production'
require_line 'group: myblog-production'
require_line 'cancel-in-progress: false'
require_line 'id-token: write'
require_line 'aws-actions/configure-aws-credentials@v5'
require_line 'authorize-security-group-ingress'
require_line 'revoke-security-group-ingress'
require_line 'if: always()'
require_line 'StrictHostKeyChecking=yes'
require_line 'deploy $RELEASE_SHA'
if grep -F 'deploy \"$RELEASE_SHA\"' "$WORKFLOW" >/dev/null; then
  fail 'SSH command must not send literal quotes to the forced command'
fi

[[ -f "$CD_DOCUMENT" ]] || fail "missing CD runbook: $CD_DOCUMENT"
for required in \
  'GitHub OIDC provider' \
  'MyBlogGitHubCdRole' \
  'myblog-github-cd-ssh' \
  'production Environment' \
  '临时 /32' \
  '撤销' \
  'workflow_dispatch' \
  '不自动回滚数据库'; do
  grep -F -- "$required" "$CD_DOCUMENT" >/dev/null || fail "missing CD documentation contract: $required"
done

printf 'workflow contract: PASS\n'
