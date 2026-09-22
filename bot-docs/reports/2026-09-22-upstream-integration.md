# Upstream integration and debug APK refresh

Integrated Android upstream `1b770ca2a` and frontend upstream `a519e9c3` into
the existing isolated working branches. Upstream has merged desktop PR #27;
Android PR #373 remains open.

## Integration decisions

- Follow the new `Capability` / `App.capabilities` contract in both projects.
  Resolve helper/mirror conflicts and migrate affected frontend test fixtures.
- Preserve our service's `stopWithTask=false` while accepting upstream's
  `specialUse`-only foreground-service declaration.
- Regenerate the SDL snapshot from the merged runtime, retaining deterministic
  root operation order rather than manually merging generated schema text.
- Rebuild and replace generated web resources from the matched frontend in web
  mode. Retain Android-safe chunk naming, conversation fragment corrections,
  bundle/schema regression tests, and finished-APK resource verification.

## Verification

Frontend typecheck, 1,175 tests (51 skipped), web build and Tauri frontend build
passed. Android: 1,157 tests passed, one skipped, no failures; Github debug APK
assembled. All 908 web resources match the finished APK byte-for-byte. The APK
verifier's matching/missing/changed/missing-index tests also passed.

Installed with `adb install -r` over the existing physical-phone debug package,
without uninstalling or clearing app data. Launched successfully; the HTTP service
is foreground with specialUse. USB HTTP and the normal public HTTPS endpoint serve
`index-DaiIxPET.js`. Both endpoints return the exact packaged query/helper bytes
with JavaScript MIME types; the App fragment requests `capabilities`, not the
removed `features` field.

The user's browser debugging port 9222 remained unavailable after installation.
Do not equate static asset checks with authenticated browser verification.
No real SMS was sent and no background/Doze soak test was performed this run.
