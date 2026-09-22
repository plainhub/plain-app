# Bundled web/backend contract mismatch

## Report and reproduction

The browser displayed `Property battery on App does not exist` during startup.
Read-only inspection of the live site's HTML and JavaScript found entry
`index-nHuZnhYY.js` and query chunk `query-Detar1oC.js`. Its AppFragment requested
eight fields removed from the current backend: battery, appVersion, osVersion,
audioCurrent, audioMode, sdcardPath, usbDiskPaths, and internalStoragePath.

The same obsolete bundle was still committed in plain-app upstream `e639bcea5`.
The latest plain-desktop source (`d2d06688`) already uses the current App fields
and the separate device status API. Thus merely fetching backend source or
reloading the browser could not repair the mismatched packaged resources.

Added `WebAppFragmentContractTest`, which reads the actual JavaScript under
`app/src/main/resources/web/assets` and compares its AppFragment field selections
against the generated runtime GraphQL App type. Before the fix, it failed with
the exact query-chunk filename and all eight unsupported field names.

## Fix and integration

- Integrated latest upstream in both active PR branches, preserving the prior
  sync/background fixes and reconciling overlapping download/feature changes.
- Updated stale frontend test fixtures to the new sentAt/lastMessageAt timestamps
  and chatItems target argument; retained the ordering/reconciliation assertions.
- Built the matched frontend in web mode and replaced the APK's generated web
  resources with that dist output. Old hashed assets were removed by a scoped
  rsync into the Git-tracked web-resource directory, recoverable from Git history.
- Kept the bundle contract regression test to detect future App field drift.

The check covers the flat App and conversation fragments, not every frontend operation.
No compatibility shim was added to reintroduce removed fields into the backend.

## Build recipe

From the matched plain-desktop checkout, run `corepack yarn typecheck`,
`corepack yarn test`, and `corepack yarn build` with no Tauri-mode environment.
From plain-app, sync that sibling checkout's `dist/` into
`app/src/main/resources/web/`, then run `:shared:testAndroidHostTest` and
`:app:assembleGithubDebug`. Always sync the web build, not the Tauri build.

## Verification

Frontend: typecheck passed; 1,151 tests passed and 51 skipped; web build passed.
The bundled-App contract test failed before asset replacement, as described above.
Android: 1,149 tests passed, one skipped, no failures; Github debug APK assembled.
The new upstream SDL snapshot gate initially failed because root operation output
order depended on resolver registration order. SchemaPrinter now sorts root fields
for printing only, with a regression assertion and regenerated snapshot. Verified
the snapshot's line multiset is unchanged (ordering only). Schema regeneration and
the subsequent full suite passed in separate Gradle invocations.

Tauri frontend build and all six capture tests also passed.

Installed the verified APK over the existing debug application with `adb install -r`
without clearing settings or uninstalling. The foreground server restarted. Both
USB-forwarded HTTP and the user's normal HTTPS endpoint now serve
`index-DsYEhPxo.js`; their query asset matches the rebuilt resource byte-for-byte
and the AppFragment no longer requests the eight removed fields.

The user's browser debugging endpoint was unavailable, so authenticated browser
rendering was not re-tested in this session. An already-open tab must reload to
execute the new bundle. This verification does not claim a new SMS/Doze soak test.

## Follow-up: packaged module missing and enhanced conversation query drift

The initial installation was not a successful browser verification. The user
reported a blank page; CDP reproduction confirmed HTTP 404 for Vue's
`_plugin-vue_export-helper-BDNMzG2s.js`, leaving the app root empty. The file was
present in web resources but absent from the APK. Android's Java-resource
packaging filters underscore-prefixed basenames. The previous build recipe had
bypassed the frontend's separate post-build renaming workaround.

The frontend now generates `chunk-`-prefixed chunk names in Vite 8's active
`rolldownOptions.output` configuration. Imports and hashes are generated together,
without post-build rewriting. Removed the macOS-specific sed workaround and made
the existing build script stop on failures. The legacy `rollupOptions` block is
not used by Vite when `rolldownOptions` is present; no unrelated settings were
migrated as part of this repair.

Added a failing-first Android asset-name regression and
`scripts/verify_web_apk.py`, which compares every web resource's path and bytes
against the finished APK. The latter failed on exactly the omitted helper. Its
unit test covers matching, missing, changed, and missing-index inputs.

After the packaging repair, the live authenticated UI rendered but had an empty
conversation sidebar. A metadata-only authenticated API check returned 1,050
conversations, while the enhanced frontend fragment still requested obsolete
`date`. Updated it to `lastMessageAt`; a failing-first frontend regression checks
parity with the base fragment, and the Android bundle contract now checks both
conversation fragments against the actual runtime schema.

Final follow-up verification:

- Android: 1,151 passed, one skipped, no failures; Github debug build passed.
- Frontend: 1,152 passed, 51 skipped; typecheck, web and Tauri frontend builds passed.
- Finished APK: all 896 web resources present with exact bytes, including the helper.
- Installed over the existing debug package without data clearing. New entry:
  `index-Cdtl4lnZ.js`; helper serves HTTP 200 with `text/javascript`.
- Authorized live Brave CDP session: 50 conversations rendered; opening a listed
  conversation displayed 100 messages and the composer. Observed 17 GraphQL and
  80 JavaScript responses, with no HTTP, console, or page errors.
- Repeated with browser network cache disabled: 50 conversations, 100 messages,
  visible composer, 80 JavaScript responses, no errors. Restored normal caching.
- No carrier messages sent; no claim of a new background/Doze soak test. Browser
  verification collected counts/errors, not message bodies or credentials.

Before installing future builds, run:
`python3 scripts/verify_web_apk.py app/build/outputs/apk/github/debug/app-github-debug.apk`.
