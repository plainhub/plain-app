# Screen-mirror orientation races leave landscape control coordinates stale

## Tracking

- Issue: https://github.com/plainhub/plain-app/issues/313
- Repository: https://github.com/plainhub/plain-app
- Branch/commit tested: `upstream/main` at `716f5e9ae6a5f0ec05c0ecb207dd9c4c31213fea`
- Pull request: https://github.com/plainhub/plain-app/pull/350

## Priority and scope

This issue was selected because it breaks precise remote control whenever a mirrored phone rotates to landscape, has no reliable workaround within the active session, and is reproducible on current upstream code with an Android emulator. Higher-severity candidates require unavailable vendor-specific hardware, while other leading candidates lack the media samples or physical tethering topology needed for evidence-based work.

The investigation covers Android screen-mirror orientation tracking, encoder resizing, the bundled browser client's rendered coordinate space, and remote tap delivery. Note/Markdown-editor work, unrelated media playback, and general screen-mirror performance are explicitly excluded.

## Reported behavior

The reporter observes accurate remote-control clicks while the phone is in portrait, but clicks become substantially offset after opening a landscape application or game. The report used PlainApp 3.1.9 on a realme GT Neo5 SE running Android 16. The maintainer could not reproduce it and attempted a correction for 3.1.12.

## Reproduction

### Environment

- PlainApp 3.3.10 (`661`), F-Droid release-equivalent x86_64 APK built from clean current upstream source
- `upstream/main` at `716f5e9ae6a5f0ec05c0ecb207dd9c4c31213fea`
- Android 12/API 31 Google APIs x86_64 emulator, 1080 x 2340 physical display
- Current bundled PlainApp web client served by the Android application
- Chrome 151 on Linux/X11
- PlainApp Accessibility Service enabled; screen-capture permission granted
- Browser-only test-harness override from `prefer-hardware` to `prefer-software` for WebCodecs because the headless emulator does not expose a usable host H.264 hardware decoder; coordinate and orientation code is unchanged

No physical phone or private device data was accessed.

### Steps

1. Build and install the clean current-upstream APK, start PlainApp's local server, and authenticate the bundled browser client.
2. Start screen mirroring, grant Android's screen-capture prompt, and enable PlainApp's Accessibility Service and browser remote-control overlay.
3. Open Android Settings in portrait and click `Network & internet` through the browser mirror.
4. Confirm that Android opens the `Network & internet` screen.
5. Rotate the emulator through its hardware acceleration sensor, allowing Android to transition to landscape while the mirror service remains active.
6. Compare the physical display rotation and size with the mirror service's last orientation decision, encoder size, virtual-display size, and browser canvas dimensions.

### Actual result

The portrait control path succeeds: the browser sends a normalized tap and Android opens the expected `Network & internet` Settings screen.

During hardware rotation, `OrientationEventListener` runs before Android commits the corresponding `Display.rotation`. The callback therefore observes the previous display orientation. In the reproduced final state:

```text
physical display: rotation=3, real=2340x1080 (landscape)
service decision: newIsPortrait=true
encoder/virtual display: 832x1802 (portrait)
browser canvas: 832x1802 (portrait)
```

The logs show the stale read directly:

```text
screen mirror: sensor=90 newIsPortrait=true (was false)
MirrorPipeline: captureSize phys=1080x2340 ... -> 832x1802
MirrorPipeline: orientation changed (portrait=true), encoder at 832x1802
```

The physical Android display commits landscape immediately afterward, but no later orientation correction reaches the pipeline. The mirror and control coordinate space consequently remain portrait while the controlled display is landscape.

### Expected result

PlainApp must react to the committed Android configuration. After rotation, a 2340 x 1080 physical display must produce a landscape encoder/canvas, invalidate the accessibility screen-size cache, and keep normalized browser clicks aligned with their Android targets.

## Investigation findings

`ScreenMirrorService` initializes its orientation correctly from the default display. While mirroring, however, it relies on `OrientationEventListener.onOrientationChanged()`. The callback ignores the supplied sensor angle and immediately calls `currentDisplayIsPortrait()`, which reads `Display.rotation`.

Android's sensor event and display-configuration commit are separate events. On the reproduced API 31 system, the sensor callback arrives first. The service then updates its `isPortrait` state and asks `ScreenMirrorPipeline` to rebuild from the old real display dimensions. Once Android commits the new rotation, the service receives no second corrective callback.

The current browser coordinate normalization already accounts for contain-fit scaling and letterboxing, and portrait taps reach the expected target. `PlainAccessibilityService` also maps normalized coordinates against current real display metrics and exposes cache invalidation. These results rule out the browser's letterbox calculation and the basic accessibility gesture path as the primary failure.

The following alternatives were also ruled out:

- Stale released code: the failure is present at current `upstream/main`.
- Missing Accessibility permission: the service is bound and portrait remote taps succeed.
- Browser-only scaling error: the encoder and browser canvas themselves remain in the wrong orientation.
- A settings-only rotation artifact: the decisive reproduction uses emulator acceleration-sensor changes and records `OrientationEventListener` callbacks.
- Unsupported browser H.264 hardware decoding: software decoding renders the same current-upstream stream; the decoder preference does not participate in orientation or control mapping.

## Root cause

`ScreenMirrorService` treats a sensor-orientation callback as proof that `Display.rotation` has already changed. That ordering is not guaranteed. Reading the display synchronously from the early sensor callback can capture the previous orientation, rebuild the encoder with stale dimensions, and leave both the mirrored frame and remote-control coordinate space one rotation behind the physical display.

## Implemented correction

`ScreenMirrorService` now initializes its state from the current committed `Configuration.orientation` and handles later transitions through `Service.onConfigurationChanged()`. Android delivers this callback after updating the component's resources, so the service no longer guesses committed display state from an early physical-sensor notification.

A small `ScreenMirrorOrientationState` coordinator accepts only committed portrait or landscape values, ignores `ORIENTATION_UNDEFINED`, and deduplicates repeated configurations. Each real transition updates the state before invalidating `PlainAccessibilityService`'s screen-size cache and rebuilding the mirror pipeline. The previous `OrientationEventListener`, synchronous `Display.rotation` read, and associated enable/disable lifecycle code have been removed.

This also avoids assuming that rotation 0/180 means portrait, an assumption that is not valid for devices whose natural orientation is landscape.

## Regression coverage

`ScreenMirrorOrientationStateTest` proves that:

- a committed portrait-to-landscape transition changes the state and requests exactly one rebuild;
- a duplicate landscape configuration does not request another rebuild;
- a committed landscape-to-portrait transition restores portrait and requests one rebuild; and
- an undefined configuration changes neither state nor rebuild count.

`ScreenMirrorCaptureSizeTest` now includes the exact reproduced landscape dimensions and low-end pixel cap, asserting that a 2340 x 1080 display produces a landscape 1802 x 832 capture rather than preserving stale portrait geometry.

## Validation

Baseline current-upstream APK build and installation succeeded. The portrait remote-control assertion passed, and the landscape failure above was reproduced against the actual Android backend/browser pairing.

The regression test was first run before the coordinator existed and failed compilation on the missing `ScreenMirrorOrientationState`, recording the red gate. After the correction, the following checks passed:

```text
./gradlew :shared:testAndroidHostTest \
  --tests com.ismartcoding.plain.tests.ScreenMirrorOrientationStateTest \
  --tests com.ismartcoding.plain.tests.ScreenMirrorCaptureSizeTest
BUILD SUCCESSFUL

./gradlew :shared:testAndroidHostTest
BUILD SUCCESSFUL

./gradlew :app:assembleDebug
BUILD SUCCESSFUL (fdroidDebug, githubDebug, and googleDebug)

./gradlew :app:assembleFdroidRelease -PabiFilters=x86_64
BUILD SUCCESSFUL
```

The corrected release-equivalent APK was installed over the reproduced baseline on the same Android 12 emulator. The original browser/backend scenario then passed in both orientations:

```text
portrait browser tap: Network & internet -> expected Android screen opened
screen mirror: configuration changed (portrait=false)
MirrorPipeline: captureSize phys=2340x1080 ... -> 1802x832
MirrorPipeline: orientation changed (portrait=false), encoder at 1802x832
browser canvas: 1802x832
landscape browser tap: Connected devices -> expected Android screen opened
screen mirror: configuration changed (portrait=true)
MirrorPipeline: captureSize phys=1080x2340 ... -> 832x1802
MirrorPipeline: orientation changed (portrait=true), encoder at 832x1802
```

The portrait and landscape clicks were sent through the actual browser remote-control overlay as normalized coordinates, not through direct ADB input. This proves the corrected mirror geometry and accessibility coordinate path together.

No physical phone or private device data was used during validation.

## Remaining limitations

- The reporter's realme/Android 16 hardware is unavailable; reproduction uses Android 12's official emulator image.
- The emulator's browser path requires a software-decoder preference override because its virtual graphics stack does not expose a usable H.264 hardware decoder. No orientation or coordinate code is modified by that harness override.
- GitHub denied self-assignment to `@th317erd`; assignment has been requested from `@ismartcoding` in the issue.
- Pull-request CI and maintainer review are pending.
