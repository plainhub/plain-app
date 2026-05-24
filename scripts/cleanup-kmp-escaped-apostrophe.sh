#!/bin/bash
# Replace escaped apostrophes (\') with plain apostrophes (')
# in KMP composeResources strings*.xml files.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RES_DIR="$ROOT_DIR/shared/src/commonMain/composeResources"

if [[ ! -d "$RES_DIR" ]]; then
  echo "composeResources directory not found: $RES_DIR"
  exit 1
fi

FILES=()
while IFS= read -r file; do
  FILES+=("$file")
done < <(find "$RES_DIR" -type f -path '*/values*/strings*.xml' | sort)

if [[ ${#FILES[@]} -eq 0 ]]; then
  echo "No strings*.xml files found under $RES_DIR"
  exit 0
fi

changed_files=0
total_replacements=0

for file in "${FILES[@]}"; do
  count=$(grep -o "\\\\'" "$file" 2>/dev/null | wc -l | tr -d ' ')
  if [[ "$count" -gt 0 ]]; then
    perl -pi -e "s/\\\\'/'/g" "$file"
    changed_files=$((changed_files + 1))
    total_replacements=$((total_replacements + count))
  fi
done

echo "Done. Updated $changed_files file(s), replaced $total_replacements occurrence(s)."