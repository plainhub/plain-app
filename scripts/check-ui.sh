#!/usr/bin/env bash
set -euo pipefail
export PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH
plain_ui_repo="$(cd "$(dirname "$0")/.." && pwd)"
ui_target_root="${1:-$plain_ui_repo}"
ui_baseline="${2:-$ui_target_root/scripts/ui-component-baseline.tsv}"
"$plain_ui_repo/gradlew" -p "$plain_ui_repo" :ui-check:test :ui-check:installDist
"$plain_ui_repo/ui-check/build/install/ui-check/bin/ui-check" "$ui_target_root" "$ui_baseline"
