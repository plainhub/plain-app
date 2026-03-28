#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
UI_DIR="$ROOT_DIR/app/src/main/java/com/ismartcoding/plain/ui"
LIMIT="${1:-150}"

if [[ ! -d "$UI_DIR" ]]; then
  echo "UI directory not found: $UI_DIR" >&2
  exit 2
fi

violations=0
while IFS= read -r file; do
  lines=$(wc -l < "$file" | tr -d ' ')
  if (( lines > LIMIT )); then
    rel="${file#$ROOT_DIR/}"
    printf '%s %s\n' "$lines" "$rel"
    violations=$((violations + 1))
  fi
done < <(find "$UI_DIR" -name '*.kt' -type f | sort)

if (( violations > 0 )); then
  echo "Found $violations UI files over ${LIMIT} lines." >&2
  exit 1
fi

echo "OK: all UI files are within ${LIMIT} lines."
