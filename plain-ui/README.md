# plain-ui

Reusable Compose Multiplatform UI primitives shared by Plain applications.

The library provides shared Compose components in `com.ismartcoding.plain.ui.base`, including `PScaffold`, `PTopAppBar`, fast scrollbars, pull to refresh, and drag selection. It also includes the reusable large-file `CodeEditor` and its document, syntax highlighting, search, and edit-history engine, plus a multiplatform QR scanner in `com.ismartcoding.plain.ui.scanner`. Scanner host actions such as camera permission, image picking, scan history, and pairing are supplied through `ScanPageActions`.

`PTopAppBar` accepts `onNavigateBack` and `navigationIcon` callbacks. Pull refresh strings ship with the library resources. Drag selection uses the small `Identifiable` contract from `plain-common`.

Maven coordinate: `com.ismartcoding:plain-ui`. Releases are published to
`https://plainhub.github.io/plain-app/maven` from the dedicated `maven` branch (which also carries the policy and terms pages), with the `plain-ui-v<version>` tag
or the `Publish plain-ui` workflow.

```kotlin
dependencies {
    implementation("com.ismartcoding:plain-ui:0.4.0")
}
```

Use the shared components from `com.ismartcoding.plain.ui.base`, the `fastscroll`, `pullrefresh`, and `dragselect` packages, `ScanPage` from `com.ismartcoding.plain.ui.scanner`, and `CodeEditor` from `com.ismartcoding.plain.ui.components.codeeditor`. The PlainRouter Android client already uses
the same Maven repository URL as `plain-common`.
