# Tests

```text
tests/
├── app/
│   └── androidTest/          # Android device tests
├── shared/
│   ├── commonTest/           # Unit tests + iOS simulator tests
│   ├── androidHostTest/      # Unit tests
│   ├── iosTest/              # iOS simulator tests
│   ├── testFixtures/
│   ├── androidTestFixtures/
│   └── manual/
├── plain-common/
│   ├── commonTest/           # Unit tests + iOS simulator tests
│   ├── androidHostTest/      # Unit tests
│   └── testFixtures/
└── ui-check/
    └── test/                 # Unit tests
```

Run from the repository root.

## Unit tests

Run on your computer without a device. The default runs Kotlin JVM host tests and the pinned Core Rust tests. Desktop Web tests are owned by and run from `plain-desktop`.

```bash
bash scripts/test-unit.sh
bash scripts/test-unit.sh kotlin
bash scripts/test-unit.sh rust
```

## Android device tests

Run on a connected Android test device or emulator.

```bash
./gradlew :app:connectedGithubDebugAndroidTest
```

Generate synthetic 3.3.25 data with the published Room schema for upgrade tests.

```bash
python3 tests/app/legacy_upgrade_fixture.py "$(mktemp -d)/legacy-upgrade"
```

The output contains two paired devices with separate app sandboxes. For emulator tests, use
`--stored-files-root` to set the device path stored in share and favorite-folder records.
Use `--room-version 8` for a 3.0.0 fixture or `--room-version 16` for a 3.2.3 fixture.

## iOS simulator tests

Run on an iOS simulator.

```bash
./gradlew :shared:iosSimulatorArm64Test :plain-common:iosSimulatorArm64Test
```
