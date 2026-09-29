# plain-ui

Reusable Compose Multiplatform UI primitives shared by Plain applications.

The library provides shared Compose components in `com.ismartcoding.plain.ui.base`, including `PScaffold`, `PTopAppBar`, fast scrollbars, pull to refresh, and drag selection. App-specific resources, platform behavior, and ViewModel-backed components stay in their owning app.

`PTopAppBar` accepts `onNavigateBack` and `navigationIcon` callbacks. Pull refresh strings ship with the library resources. Drag selection uses the small `Identifiable` contract from `plain-common`.

Maven coordinate: `com.ismartcoding:plain-ui`. Releases are published to
`https://plainhub.github.io/plain-app/maven` from the dedicated `maven` branch (which also carries the policy and terms pages), with the `plain-ui-v<version>` tag
or the `Publish plain-ui` workflow.

```kotlin
dependencies {
    implementation("com.ismartcoding:plain-ui:0.1.0")
}
```

Use the shared components from `com.ismartcoding.plain.ui.base` and its `fastscroll`, `pullrefresh`, and `dragselect` packages. Downstream Android clients already use
the same Maven repository URL as `plain-common`.
