# plain-ui

Reusable Compose Multiplatform UI primitives shared by Plain applications.

The library currently provides `PScaffold`, `PIcon`, `HorizontalSpace`, and
`VerticalSpace` in `com.ismartcoding.plain.ui.base`. App-specific resources,
platform behavior, and ViewModel-backed components stay in their owning app.

Maven coordinate: `com.ismartcoding:plain-ui`. Releases are published to
`https://plainhub.github.io/plain-app/maven` from the dedicated `maven` branch (which also carries the policy and terms pages), with the `plain-ui-v<version>` tag
or the `Publish plain-ui` workflow.

```kotlin
dependencies {
    implementation("com.ismartcoding:plain-ui:0.1.0")
}
```

Use `PScaffold`, `PIcon`, `HorizontalSpace`, and `VerticalSpace` from
`com.ismartcoding.plain.ui.base`. Downstream Android clients already use
the same Maven repository URL as `plain-common`.
