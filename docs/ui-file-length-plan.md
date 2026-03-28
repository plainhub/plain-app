# UI File Length Plan (<= 150 lines)

This project enforces `<= 150` lines per single UI Kotlin file.

## Quick Check

```bash
scripts/check-ui-file-length.sh 150
```

- Exit `0`: all UI files comply.
- Exit `1`: one or more files exceed limit.

Current full violation snapshot is tracked in:
- `docs/ui-file-length-violations.txt`

## Recommended Refactor Order

1. `ui/page/*` pages: split screen state/events/actions into dedicated composables.
2. `ui/page/*/components/*`: split item/card vs action/handler logic.
3. `ui/components/mediaviewer/*`: split decoder, gesture, action overlays.
4. `ui/models/*`: split by concern into `*State`, `*Actions`, `*Loader`.
5. `ui/base/*`: split reusable primitives into separate files.

## Split Rules

- No whitespace/blank-line trimming tricks to reduce line count.
- Each split must be logical and reusable.
- Keep public behavior unchanged.
- Keep each resulting file `<= 150` lines.
