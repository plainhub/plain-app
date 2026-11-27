#!/bin/bash
set -euo pipefail

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 APK [APK ...]" >&2
  exit 2
fi

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
aapt2=""
for candidate in "$sdk"/build-tools/*/aapt2; do
  if [ -x "$candidate" ]; then
    aapt2="$candidate"
  fi
done
if [ -z "$aapt2" ]; then
  echo "Android SDK aapt2 is required to check shortcut resources" >&2
  exit 2
fi

for apk in "$@"; do
  "$aapt2" dump resources "$apk" | awk -v apk="$apk" '
    BEGIN {
      count = split("notes docs feeds pomodoro_timer images videos audio files", shortcuts, " ")
    }
    $1 == "resource" { entry = $3 }
    $1 == "()" { packaged[entry] = 1 }
    END {
      for (i = 1; i <= count; i++) {
        icon = "drawable/shortcut_" shortcuts[i]
        label = "string/shortcut_" shortcuts[i]
        if (!packaged[icon]) {
          print apk ": missing default resource " icon > "/dev/stderr"
          failed = 1
        }
        if (!packaged[label]) {
          print apk ": missing default resource " label > "/dev/stderr"
          failed = 1
        }
      }
      exit failed
    }
  '
done
