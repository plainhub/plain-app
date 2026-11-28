set -eu

uid=$(id -u)
case "$uid" in
    2000) target=com.android.shell ;;
    0) target=0 ;;
    *) exit 1 ;;
esac

state=$(cmd appops get --user 0 "$target" WRITE_SMS)
uid_mode=$(printf '%s\n' "$state" | sed -n 's/^Uid mode: WRITE_SMS: \([a-z]*\).*/\1/p')
if [ -n "$uid_mode" ]; then
    target=$uid
    mode=$uid_mode
else
    mode=$(printf '%s\n' "$state" | sed -n 's/^WRITE_SMS: \([a-z]*\).*/\1/p; s/^Default mode: \([a-z]*\)$/\1/p')
fi

case "$mode" in
    allow) ;;
    default|ignore|deny|foreground)
        trap 'cmd appops set --user 0 "$target" WRITE_SMS "$mode" || exit 1' EXIT
        trap 'exit 1' HUP INT TERM
        cmd appops set --user 0 "$target" WRITE_SMS allow
        ;;
    *) echo "Unknown WRITE_SMS mode" >&2; exit 1 ;;
esac

content delete --uri "$1" --user "$2"
