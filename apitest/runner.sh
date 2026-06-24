#!/usr/bin/env bash
# Entry point. Run one group or all groups.
#
#   ./apitest/runner.sh                              # run every group
#   ./apitest/runner.sh setup                        # run setup
#   ./apitest/runner.sh notes                        # run notes group
#   ./apitest/runner.sh setup device-read content-provider   # run several
#
# Group names map to files in apitest/groups/<name>.sh.
# Reads cid / token / url / adb_id from apitest/config.json.
# Writes raw API responses to apitest/results/.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APITEST_ROOT="$SCRIPT_DIR"
export APITEST_ROOT

# shellcheck source=lib/common.sh
source "$APITEST_ROOT/lib/common.sh"

load_config || exit 1

GROUPS_DIR="$APITEST_ROOT/groups"

if [[ $# -eq 0 ]]; then
  # Run every group, in dependency order.
  groups=(
    setup
    device-read
    content-provider
    notes
    media
    files
    app-state
    screen-mirror
    discovery
    chat-channels
    chat-messages
    schema
  )
else
  groups=("$@")
fi

OVERALL_PASS=0
OVERALL_FAIL=0
for g in "${groups[@]}"; do
  script="$GROUPS_DIR/$g.sh"
  if [[ ! -f "$script" ]]; then
    echo "  !! group $g not found at $script"
    continue
  fi
  PASS_COUNT=0
  FAIL_COUNT=0
  # shellcheck source=groups/<name>.sh
  source "$script"
  OVERALL_PASS=$((OVERALL_PASS + PASS_COUNT))
  OVERALL_FAIL=$((OVERALL_FAIL + FAIL_COUNT))
done

echo ""
echo "================================================================"
echo "  TOTAL: $OVERALL_PASS pass / $OVERALL_FAIL fail"
echo "================================================================"
exit $((OVERALL_FAIL > 0 ? 1 : 0))
