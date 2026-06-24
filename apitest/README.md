# apitest — GraphQL API test harness

Live integration tests for every `query` / `mutation` declared in
`app/src/main/java/com/ismartcoding/plain/web/schemas/`.

## Files

```
apitest/
├── config.json                # URL / cid / token / adb_id — fill before running
├── runner.sh                  # main entry: ./runner.sh [group-name...]
├── setup-session.sh           # one-time: mint a TYPE_CUSTOM token via SQL
├── lib/common.sh              # curl + adb + assertion helpers
├── groups/                    # one .sh per test group, named by what it covers
├── results/                   # raw JSON responses, one file per case
└── docs/                      # per-group run notes (when something breaks)
```

## Run

```bash
# 1. fill config.json
$EDITOR apitest/config.json

# 2. (optional) mint a session if you don't have an API token
./apitest/setup-session.sh

# 3. run a single group
./apitest/runner.sh setup

# 4. run several groups by name
./apitest/runner.sh setup device-read content-provider

# 5. run everything
./apitest/runner.sh
```

Each case does two things:
1. calls `POST /graphql` with `c-id` + `Authorization: Bearer <token>`
2. extracts the same value from the device via `adb -s $ADB_ID shell ...`

A case passes only if both halves agree.

## Test groups

See `docs/api-test-plan.md` for the full list. TL;DR:

| Group            | Schemas |
|------------------|---------|
| `setup`          | `AppGraphQL` + introspection |
| `device-read`    | `Package`, `DataStore`, `Db` |
| `content-provider` | `Contact`, `Sms`, `Call` |
| `notes`          | `Note`, `Tag`, `Feed` |
| `media`          | `Audio`, `Video`, `Image`, `Media`, `Doc` |
| `files`          | `FileQuery`, `FileMutation`, `FileUpload` |
| `app-state`      | `Bookmark`, `Pomodoro`, `Notification`, `AppLogs`, `AppFile` |
| `screen-mirror`  | `ScreenMirror` |
| `discovery`      | `Discover`, `Pairing`, `ChatPeer` |
| `chat-channels`  | `ChatChannel` |
| `chat-messages`  | `ChatMessage` |
| `schema`         | `SchemaTypes` |

## Auth

`POST /graphql` requires either:

- `Authorization: Bearer <token>` and a session row with `type='custom'` (the
  bearer path), **or**
- no `Authorization` header and a ChaCha20-encrypted body (the encrypted
  path used by the Web UI)

This harness uses the bearer path because it's the simplest to drive from a
shell script. The token is created by:

- Web UI: Settings → API Access → Create Token, or
- `./apitest/setup-session.sh` (inserts the row directly into `plain.db`).

## What the harness never does

- No `adb install` / `adb push` of APKs.
- No destructive mutations against contacts / SMS / call log on the device
  unless the test itself created the fixture first (or an explicit
  `content insert` step pre-populates it).
- No writes outside `apitest/` on the host. Device-side writes are
  limited to the temporary session row created by `setup-session.sh`.
