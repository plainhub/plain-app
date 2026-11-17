#!/bin/bash
# Unit tests: Kotlin (JVM host) and Rust (pinned plain-server).
#
#   ./scripts/test-unit.sh                 # both groups
#   ./scripts/test-unit.sh kotlin          # one group
#
# `--output <dir>` writes report.json for the workbench /tests page.
set -uo pipefail

usage() {
  printf 'usage: %s [all|kotlin|rust] [--output <dir>]\n' "$0" >&2
  exit 2
}

select_group() {
  local group=""
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --output) [ "$#" -ge 2 ] && [ -n "$2" ] || usage; shift 2 ;;
      kotlin|rust|all) [ -z "$group" ] || usage; group="$1"; shift ;;
      *) usage ;;
    esac
  done
  GROUP="${group:-all}"
}

select_group "$@"
source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"
cd "$REPO_ROOT"

# --- Kotlin ------------------------------------------------------------------
run_kotlin() {
  head1 "Kotlin JVM host tests"
  # --rerun-tasks is required: without it Gradle reports UP-TO-DATE and the
  # stale XMLs from the previous run get counted as if they were fresh.
  if ! ./gradlew :shared:testAndroidHostTest :plain-common:testAndroidHostTest :ui-check:test --rerun-tasks -q > /tmp/plain-unit-kotlin.log 2>&1; then
    tail -40 /tmp/plain-unit-kotlin.log
    case_fail "kotlin_host_tests" "gradle failed, see /tmp/plain-unit-kotlin.log"
    return
  fi
  local total=0 failed=0 module
  for module in shared plain-common ui-check; do
    local task="testAndroidHostTest"
    if [ "$module" = "ui-check" ]; then task="test"; fi
    read -r t s f e <<< "$(count_junit_dir "$module/build/test-results/$task")"
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
  local wt rev
  if [ -d "$REPO_ROOT/.cargo/core-dependency-state" ]; then
    wt=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["core"])' \
         "$REPO_ROOT/.cargo/core-dependency-state/state.json") || return
  else
    rev=$(awk '/name = "plain-server"/{f=1} f&&/source = "git/{print; exit}' plain-rs/Cargo.lock 2>/dev/null \
          | sed -n 's/.*#\([0-9a-f]\{40\}\).*/\1/p')
    if [ -z "$rev" ]; then
      case_block "rust_plain_rs" "cannot resolve the pinned plain-server rev from Cargo.lock"
      return
    fi
    wt=$(mktemp -d)/plain-core
    say "  pinned rev $rev"
    if ! git -C "$CORE_ROOT" worktree add --detach "$wt" "$rev" >/dev/null 2>&1; then
      case_block "rust_plain_rs" "cannot create a core worktree at $rev"
      return
    fi
    trap 'git -C "$CORE_ROOT" worktree remove --force "$wt" 2>/dev/null' RETURN
  fi
  if ! (cd "$wt" && cargo test --locked --quiet -p plain-server --lib --features core,http_transport) \
        > /tmp/plain-unit-rust.log 2>&1; then
    tail -40 /tmp/plain-unit-rust.log
    case_fail "rust_plain_rs" "cargo test failed, see /tmp/plain-unit-rust.log"
    return
  fi
  local line passed
  line=$(grep -E '^test result:' /tmp/plain-unit-rust.log | tail -1)
  # cargo prints "test result: ok. 934 passed; 0 failed; ..." — the count
  # comes BEFORE the first semicolon, so match the count next to the word
  # rather than after a separator.
  passed=$(printf '%s' "$line" | grep -oE '[0-9]+ passed' | head -1 | cut -d' ' -f1)
  if [ -z "$passed" ]; then
    case_block "rust_plain_rs" "no 'test result:' line — did the suite run at all?"
    return
  fi
  case_pass "rust_plain_rs" "$passed tests at $rev"
}

case "$GROUP" in
  kotlin) run_kotlin ;;
  rust)   run_rust ;;
  all)    run_kotlin; run_rust ;;
esac

report_write
