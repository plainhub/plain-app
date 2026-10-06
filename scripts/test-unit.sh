#!/bin/bash
# Unit tests — Kotlin (JVM host), Rust (plain-rs), web (vitest).
#
#   ./scripts/test-unit.sh                 # all three groups
#   ./scripts/test-unit.sh kotlin          # one group
#
# `--output <dir>` writes report.json for the workbench /tests page.
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"

GROUP="${1:-all}"
cd "$REPO_ROOT"

# --- Kotlin ------------------------------------------------------------------
run_kotlin() {
  head1 "Kotlin JVM host tests"
  # --rerun-tasks is required: without it Gradle reports UP-TO-DATE and the
  # stale XMLs from the previous run get counted as if they were fresh.
  if ! ./gradlew :shared:testAndroidHostTest :plain-common:testAndroidHostTest \
        :plain-ui:testAndroidHostTest --rerun-tasks -q > /tmp/plain-unit-kotlin.log 2>&1; then
    tail -40 /tmp/plain-unit-kotlin.log
    case_fail "kotlin_host_tests" "gradle failed, see /tmp/plain-unit-kotlin.log"
    return
  fi
  local total=0 failed=0 module
  for module in shared plain-common plain-ui; do
    read -r t s f e <<< "$(count_junit_dir "$module/build/test-results/testAndroidHostTest")"
    if [ "$t" -eq 0 ]; then
      case_fail "kotlin_$module" "no JUnit XML produced"
      continue
    fi
    total=$((total + t))
    failed=$((failed + f + e))
    if [ "$((f + e))" -eq 0 ]; then
      case_pass "kotlin_$module" "$t tests, $s skipped"
    else
      case_fail "kotlin_$module" "$((f + e)) of $t failed"
    fi
  done
  say "  total: $total kotlin tests"
}

# --- Rust --------------------------------------------------------------------
run_rust() {
  head1 "Rust (plain-rs)"
  if ! command -v cargo >/dev/null 2>&1; then
    case_block "rust_plain_rs" "cargo not on PATH"
    return
  fi
  # plain-rs is a git dependency, so its tests have to run inside a worktree
  # checked out at the rev Cargo.lock pinned — the local plain-desktop tree may
  # be on any commit, and testing that would not test what ships.
  local rev
  rev=$(awk '/name = "plain-rs"/{f=1} f&&/source = "git/{print; exit}' rust/plain-rust/Cargo.lock 2>/dev/null \
        | sed -n 's/.*#\([0-9a-f]\{40\}\).*/\1/p')
  if [ -z "$rev" ]; then
    case_block "rust_plain_rs" "cannot resolve the pinned plain-rs rev from Cargo.lock"
    return
  fi
  local wt
  wt=$(mktemp -d)/plain-rs
  say "  pinned rev $rev"
  if ! git -C "$DESKTOP_ROOT" worktree add --detach "$wt" "$rev" >/dev/null 2>&1; then
    case_block "rust_plain_rs" "cannot create a worktree at $rev"
    return
  fi
  # Always drop the worktree, even when the tests fail — a stale checkout makes
  # the next run test the wrong code.
  trap 'git -C "$DESKTOP_ROOT" worktree remove --force "$wt" 2>/dev/null' RETURN

  if ! (cd "$wt" && cargo test --quiet --features content_api,http_transport) \
        > /tmp/plain-unit-rust.log 2>&1; then
    tail -40 /tmp/plain-unit-rust.log
    case_fail "rust_plain_rs" "cargo test failed, see /tmp/plain-unit-rust.log"
    return
  fi
  local line passed
  line=$(grep -E '^test result:' /tmp/plain-unit-rust.log | tail -1)
  passed=$(printf '%s' "$line" | sed -n 's/.*; \([0-9]*\) passed.*/\1/p')
  if [ -z "$passed" ]; then
    case_block "rust_plain_rs" "no 'test result:' line — did the suite run at all?"
    return
  fi
  case_pass "rust_plain_rs" "$passed tests at $rev"
}

# --- web ---------------------------------------------------------------------
run_web() {
  head1 "web (vitest)"
  if [ ! -d "$DESKTOP_ROOT" ]; then
    case_block "web_vitest" "plain-desktop not found at $DESKTOP_ROOT"
    return
  fi
  # `integration` needs a phone serving on 127.0.0.1:8080, so it is not part of
  # the unit gate — running it here would report 51 ECONNREFUSED as regressions.
  if ! (cd "$DESKTOP_ROOT" && yarn vitest run --project=unit --project=cws \
          --project=graphql --project=docs) > /tmp/plain-unit-web.log 2>&1; then
    tail -40 /tmp/plain-unit-web.log
    case_fail "web_vitest" "vitest failed, see /tmp/plain-unit-web.log"
    return
  fi
  local passed
  passed=$(grep -E '^\s*Tests\s+[0-9]+ passed' /tmp/plain-unit-web.log | tail -1 \
          | sed -n 's/.*Tests\ *\([0-9]*\) passed.*/\1/p')
  [ -z "$passed" ] && passed=$(grep -oE '[0-9]+ passed' /tmp/plain-unit-web.log | tail -1 | cut -d' ' -f1)
  case_pass "web_vitest" "${passed:-?} tests (unit + cws + graphql + docs)"

  local dev
  dev=$(serial_for_model Pixel_7 || serial_for_model Pixel_9 || true)
  if [ -n "$dev" ] && wait_for_health "$(device_ip "$dev")" 8080 3; then
    case_block "web_integration" "needs the device reachable on 127.0.0.1:8080 — run scripts/api.sh"
  else
    case_block "web_integration" "no reachable device; 51 integration specs not executed"
  fi
}

case "$GROUP" in
  kotlin) run_kotlin ;;
  rust)   run_rust ;;
  web)    run_web ;;
  all)    run_kotlin; run_rust; run_web ;;
  *) say "usage: $0 [all|kotlin|rust|web]"; exit 2 ;;
esac

report_write