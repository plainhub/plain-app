# plain-ui

Reusable Compose Multiplatform UI primitives shared by Plain applications.

The library provides shared Compose components in `com.ismartcoding.plain.ui.base`, including `PScaffold`, `PTopAppBar`, fast scrollbars, pull to refresh, and drag selection. It also includes the reusable large-file `CodeEditor` and its document, syntax highlighting, search, and edit-history engine, plus the `QrCodeScanner` control in `com.ismartcoding.plain.ui.scanner`. The app keeps the scan page TopBar and result sheet; the scanner receives host callbacks for image picking, scan results, and app-specific handling.

`PSegmentedButtons` provides single selection for generic options, with sizes from `ButtonSize`, per-option disabling, theme colors through `PSegmentedButtonsDefaults`, and an optional content slot. See [compiled examples](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ShowcaseSegmentedButtons.kt).

`PTopAppBar` accepts `onNavigateBack` and `navigationIcon` callbacks. Pull refresh strings ship with the library resources. Drag selection uses the small `Identifiable` contract from `plain-common`.

Maven coordinate: `com.ismartcoding:plain-ui`. Releases are published to
`https://plainhub.github.io/plain-app/maven` from the dedicated `maven` branch (which also carries the policy and terms pages), with the `plain-ui-v<version>` tag
or the `Publish plain-ui` workflow.

```kotlin
dependencies {
    implementation("com.ismartcoding:plain-ui:0.4.0")
}
```

Use the shared components from `com.ismartcoding.plain.ui.base`, the `fastscroll`, `pullrefresh`, and `dragselect` packages, `QrCodeScanner` from `com.ismartcoding.plain.ui.scanner`, and `CodeEditor` from `com.ismartcoding.plain.ui.components.codeeditor`. Downstream Android clients already use
the same Maven repository URL as `plain-common`.
