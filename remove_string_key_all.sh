#!/bin/bash

# Usage:
#   ./remove_string_key_all.sh key1 [key2 ...]
#   ./remove_string_key_all.sh            # interactive mode

set -euo pipefail

RES_DIR="./app/src/main/res"

collect_keys() {
  grep -RhoE 'name="[^"]+"' "$RES_DIR"/values*/strings*.xml \
    | sed -E 's/name="([^"]+)"/\1/' \
    | sort -u
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

FILES=$(find "$RES_DIR" -type f -path '*/values*/strings*.xml' | sort)
if [[ -z "$FILES" ]]; then
  echo "No strings*.xml files found under $RES_DIR"
  exit 1
fi

for KEY in "${KEYS[@]}"; do
  echo "Removing key: $KEY"
  for FILE in $FILES; do
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
  done
done

echo "Done. Removed ${#KEYS[@]} key(s)."
