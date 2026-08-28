# Android 9 crashes before the first PlainApp frame

## Tracking

- Issue: https://github.com/plainhub/plain-app/issues/346
- Repository: https://github.com/plainhub/plain-app
- Branch/commit tested: `upstream/main` at `43eebc1c3dc88328c27363c8dd01c12b6fbfd898`
- Pull request: https://github.com/plainhub/plain-app/pull/349

## Priority and scope

This issue was selected because it makes the application completely unusable on a supported platform: the reporter's Android 9/32-bit device accepts PlainApp 3.3.10, but every launch immediately returns to the home screen. It is a current-release regression with a deterministic Android 9 reproduction.

The older device-freeze report (#309) has higher theoretical severity but requires a specific OnePlus/ColorOS/third-party-tool combination that is not safely available. The upload-folder report (#286) already has explicit client and server filename/path sanitization on current code. Screen-mirroring, USB-tethering discovery, and media-playback candidates are narrower and lack equally deterministic inputs.

This investigation is limited to the Android 9 launch failure. The note/Markdown editor, unrelated emulator hardware behavior, and general Android UI refactoring are excluded.

## Reported behavior

On a Huawei Y6s JAT-L29 running Android 9, the newly released PlainApp 3.3.10 old/32-bit APK installs successfully but exits immediately when opened. The attached recording shows the launcher attempting to open PlainApp and returning to the home screen before any PlainApp UI is drawn.

## Reproduction

### Environment

- Published `PlainApp-3.3.10-Old-32bit.apk`, version code `659`
- Published APK minimum SDK 28, target SDK 36, native ABI `armeabi-v7a`
- Clean `upstream/main` F-Droid debug build, version code `661`, native ABI `x86`
- Android 9/API 28 Google APIs x86 emulator with `armeabi-v7a` native translation
- Dedicated `PlainApp_Android_9_API_28_x86` AVD; no user data or physical phone involved

### Steps

1. Boot a clean Android 9/API 28 emulator.
2. Install the published 3.3.10 old/32-bit APK and launch its main activity.
3. Observe the process and capture the fatal exception.
4. Build clean current upstream for the emulator's 32-bit x86 ABI.
5. Install the side-by-side debug APK and directly launch `MainActivity`.
6. Observe the current-upstream fatal exception.

### Actual result

Both the published 32-bit release and clean current-upstream build fail before the first application frame. Android throws:

```text
java.lang.RuntimeException: Unable to start activity ... MainActivity
Caused by: java.lang.UnsupportedOperationException:
Unknown windowLayoutInDisplayCutoutMode: 3
```

The published process is no longer alive after the launch attempt. The current debug stack resolves the failure to `WindowCompat.setDecorFitsSystemWindows()` in `MainActivity.onCreate()` at line 147, where installing the window decor causes Android 9 to validate the activity theme.

### Expected result

PlainApp supports Android 9 (`minSdk = 28`), so its main activity must use window attributes recognized by API 28 and reach the first application frame.

## Investigation findings

`Theme.PlainActivity` is the post-splash theme used by `MainActivity`. Its unqualified `values/themes.xml` definition sets:

```xml
<item name="android:windowLayoutInDisplayCutoutMode">always</item>
```

That resource resolves to numeric cutout mode `3` on every API level, including Android 9. Android 9's `PhoneWindow.generateLayout()` does not recognize mode `3` and throws while the decor view is installed.

The same unqualified theme item exists in both the app resource layer and the shared Android resource layer. Commit `50b33aa9` changed both copies to `always` on August 20, shortly before the affected 3.3.10 release.

The following alternatives were ruled out:

- APK/device ABI mismatch: the official ARMv7 APK installed successfully through Android's advertised ARM native translation and reached `MainActivity`.
- Release shrinking or obfuscation: a non-minified current-upstream x86 debug build throws the same framework exception with source line information.
- Stale app version: the failure occurs in the published 3.3.10 APK and current `upstream/main`.
- Slow ARM emulation: the supported KVM-backed x86 AVD boots normally, and the failure is a synchronous framework exception.

## Root cause

The base, unqualified Android theme requests display-cutout mode `always` (numeric value `3`) even on API 28. Because Android 9 does not recognize that newer mode, window decor creation throws synchronously and prevents every `MainActivity` launch.

## Implemented correction

The base `Theme.PlainActivity` now uses Android 9's supported `shortEdges` mode. A matching `values-v30` override preserves `always` on Android 11 and newer, where that mode is recognized. Both existing copies of the Android theme (the app resource layer and shared Android resource layer) use the same qualifier split so their behavior cannot diverge depending on the consuming build target.

No activity lifecycle code or SDK support range changed.

## Regression coverage

`AndroidThemeCompatibilityTest.plainActivityThemeUsesSupportedCutoutMode` resolves the real packaged `Theme.PlainActivity` and asserts the platform-compatible value:

- API 28 through 29: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`
- API 30 and newer: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`

On the unchanged baseline, the API 28 test failed with `expected:<1> but was:<3>`. It passes after the correction on both the Android 9/API 28 and Android 12/API 31 AVDs.

## Validation

Baseline current-upstream build:

```text
PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH \
  ./gradlew :app:assembleFdroidDebug -PabiFilters=x86
BUILD SUCCESSFUL
```

Focused API 28 regression test:

```text
ANDROID_SERIAL=emulator-5560 PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH \
  ./gradlew :app:connectedFdroidDebugAndroidTest -PabiFilters=x86 \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ismartcoding.plain.tests.AndroidThemeCompatibilityTest
Starting 1 tests on PlainApp_Android_9_API_28_x86(AVD) - 9
Finished 1 tests on PlainApp_Android_9_API_28_x86(AVD) - 9
BUILD SUCCESSFUL
```

Modern-resource regression test:

```text
ANDROID_SERIAL=emulator-5558 PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH \
  ./gradlew :app:connectedFdroidDebugAndroidTest -PabiFilters=x86_64 \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ismartcoding.plain.tests.AndroidThemeCompatibilityTest
Starting 1 tests on PlainApp_Android_12_API_31(AVD) - 12
Finished 1 tests on PlainApp_Android_12_API_31(AVD) - 12
BUILD SUCCESSFUL
```

After-fix Android 9 launch proof:

```text
Starting: Intent { cmp=com.ismartcoding.plain.debug/com.ismartcoding.plain.MainActivity }
Status: ok
Activity: com.ismartcoding.plain.debug/com.ismartcoding.plain.MainActivity
Displayed com.ismartcoding.plain.debug/com.ismartcoding.plain.MainActivity: +4s81ms
```

The fixed build reaches and displays `MainActivity`; the log contains no `Unknown windowLayoutInDisplayCutoutMode` failure. The headless AVD subsequently encounters the separately documented no-Bluetooth-hardware debug failure after the activity has displayed.

Required final all-flavor debug build:

```text
PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 28s
```

## Remaining limitations

- The reporter's Huawei hardware is not available; reproduction uses Android 9's official emulator image and its supported ARM native translation.
- A secondary no-Bluetooth-hardware debug-emulator failure occurs only after `MainActivity` is displayed. The reporter's Huawei has Bluetooth hardware; that separate failure is excluded from this correction.
- The upstream assignment request is pending because `@th317erd` does not currently have permission to self-assign.
- GitHub reports no automated checks for the pull-request branch; the local validation above is the available proof at submission time.
