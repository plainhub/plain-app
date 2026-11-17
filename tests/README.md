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

## iOS simulator tests

Run on an iOS simulator.

```bash
./gradlew :shared:iosSimulatorArm64Test :plain-common:iosSimulatorArm64Test
```
