#!/bin/bash
# Delete one or more translation keys from all locale strings*.xml files
# in both the Android resources and the KMP composeResources folders.
#
# Usage:
#   ./scripts/delete-translation-key.sh key1 [key2 ...]
#   ./scripts/delete-translation-key.sh            # interactive mode

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

ANDROID_RES_DIR="$PROJECT_ROOT/app/src/main/res"
KMP_RES_DIR="$PROJECT_ROOT/shared/src/commonMain/composeResources"

# Collect every values*/strings*.xml under both locations (Android `strings.xml`
# and KMP `strings_*.xml`).
collect_files() {
  {
    find "$ANDROID_RES_DIR" -type f -path '*/values*/strings*.xml' 2>/dev/null
    find "$KMP_RES_DIR"     -type f -path '*/values*/strings*.xml' 2>/dev/null
  } | sort -u
}

collect_keys() {
  collect_files | xargs grep -hoE 'name="[^"]+"' 2>/dev/null \
    | sed -E 's/name="([^"]+)"/\1/' \
    | sort -u
}

key_exists() {
  local key="$1"
  collect_files | xargs grep -lE "<string[[:space:]]+name=\"${key}\"" >/dev/null 2>&1
}

read -r -a KEYS <<< "${*:-}"

if [[ ${#KEYS[@]} -eq 0 ]]; then
  echo "No key passed. Enter keys manually (space-separated)."
  echo
  echo "Available keys (first 120):"
  collect_keys | head -120
  echo
  read -r -p "Keys to remove: " MANUAL_KEYS
  if [[ -z "$MANUAL_KEYS" ]]; then
    echo "No keys entered. Exit."
    exit 0
  fi
  read -r -a KEYS <<< "$MANUAL_KEYS"
fi

FILES=$(collect_files)
if [[ -z "$FILES" ]]; then
  echo "No strings*.xml files found under:"
  echo "  $ANDROID_RES_DIR"
  echo "  $KMP_RES_DIR"
  exit 1
fi

NOT_FOUND=()
for KEY in "${KEYS[@]}"; do
  if ! key_exists "$KEY"; then
    NOT_FOUND+=("$KEY")
    continue
  fi

  echo "Removing key: $KEY"
  while IFS= read -r FILE; do
    if ! grep -qE "<string[[:space:]]+name=\"${KEY}\"" "$FILE"; then
      continue
    fi
    awk -v key="$KEY" '
    BEGIN { skip = 0 }
    {
      if ($0 ~ "<string[[:space:]]+name=\"" key "\"[^>]*>") {
        skip = 1
      }
      if (skip) {
        if ($0 ~ /<\/string>/) {
          skip = 0
        }
        next
      }
      print
    }
    ' "$FILE" > "${FILE}.tmp" && mv "${FILE}.tmp" "$FILE"
    echo "  -> $FILE"
  done <<< "$FILES"
done

if [[ ${#NOT_FOUND[@]} -gt 0 ]]; then
  echo
  echo "Error: the following key(s) were not found in any strings*.xml file:"
  for KEY in "${NOT_FOUND[@]}"; do
    echo "  - $KEY"
  done
  exit 1
fi

echo "Done. Removed ${#KEYS[@]} key(s)."
