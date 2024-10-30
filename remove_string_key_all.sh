#!/bin/bash

# Usage: ./remove_string_key_all.sh your_key

KEY="$1"

if [[ -z "$KEY" ]]; then
  echo "Usage: $0 <string key to remove>"
  exit 1
fi

# Find all strings.xml files under the given res directory
find ./app/src/main/res -type f -name "strings.xml" | while read -r FILE; do
  echo "Processing file: $FILE"

  # Use awk to delete the <string name="KEY">...</string> block safely
  awk -v key="$KEY" '
  BEGIN { skip = 0 }
  {
    if ($0 ~ "<string[[:space:]]+name=\"" key "\"[[:space:]]*>") {
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

echo "Done."  
