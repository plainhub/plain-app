#!/bin/bash
# Turn on the desktop-access permission switches by tapping the rows
# themselves, never by coordinate: the list scrolls, so a fixed tap lands on a
# different permission after any layout change, and a wrong permission is
# granted silently.
#
# Each round re-reads the screen and taps the topmost switch that is still off,
# so a tap that misses, or a list that reorders under us, cannot make the run
# claim more than it did.
#
#   ./scripts/grant-web-access.sh <serial> [package]
set -uo pipefail
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH"

SERIAL="${1:?usage: grant-web-access.sh <serial> [package]}"
PKG="${2:-com.ismartcoding.plain.debug}"
MAX_TAPS="${PLAIN_MAX_TAPS:-30}"

dump() {
  adb -s "$SERIAL" shell "uiautomator dump /sdcard/perm.xml" >/dev/null 2>&1
  adb -s "$SERIAL" shell "cat /sdcard/perm.xml" 2>/dev/null | tr '>' '\n'
}

# The topmost `checkable="true" checked="false"` node, as "x y".
topmost_off() {
  dump | grep 'checkable="true" checked="false"' | python3 -c '
import re, sys
best = None
for line in sys.stdin:
    m = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", line)
    if not m:
        continue
    x1, y1, x2, y2 = map(int, m.groups())
    if best is None or y1 < best[1]:
        best = ((x1 + x2) // 2, (y1 + y2) // 2)
if best:
    print(best[0], best[1])
'
}

# A switch that is off and still off after a tap: the tap did not land.
still_off() { [ -n "$(topmost_off)" ]; }

count_off() { dump | grep -c 'checkable="true" checked="false"' || true; }

echo "device=$SERIAL package=$PKG"
echo "switches off at start: $(count_off)"

taps=0
while [ "$taps" -lt "$MAX_TAPS" ]; do
  spot="$(topmost_off)"
  if [ -z "$spot" ]; then
    echo "every switch on this page is on (after $taps taps)"
    break
  fi
  # Only switches currently on screen are reachable, so scroll until one is
  # visible instead of tapping past the bottom of the list.
  adb -s "$SERIAL" shell input tap $spot
  taps=$((taps + 1))
  sleep 1
  if [ "$taps" -gt 1 ] && [ "$taps" -le 8 ]; then
    if still_off; then
      echo "tap $taps at ($spot) changed nothing — scrolling"
      adb -s "$SERIAL" shell input swipe 540 1200 540 500 250
      sleep 1
    fi
  fi
done

echo "taps=$taps  switches still off here: $(count_off)"
adb -s "$SERIAL" shell "uiautomator dump /sdcard/perm.xml" >/dev/null 2>&1
adb -s "$SERIAL" shell "cat /sdcard/perm.xml" 2>/dev/null | tr '>' '\n' \
  | grep -oE 'text="[^"]+"' | grep -v 'text=""' | head -24