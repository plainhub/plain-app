# plain-common

Reusable Kotlin Multiplatform code extracted from PlainApp. Source packages are
kept unchanged under `com.ismartcoding.plain`.

The module includes common helpers, crypto, extensions, logcat, KDataLoader,
KGraphQL, mDNS, XML/RSS support, and the vendored Ktor server required by the
KGraphQL Ktor plugin. Supporting sources are included here so this module has no
dependency on `shared-lib`.

## Maven coordinates

```kotlin
implementation("com.ismartcoding:plain-common:0.1.0-SNAPSHOT")
```

Add the public GitHub Pages Maven repository to the consuming project's
repositories:

```kotlin
maven {
    url = uri("https://plainhub.github.io/plain-app/maven")
}
```

Then add the dependency shown above. Releases use tags named
`plain-common-v<version>`; the default local version can be changed with
`-PplainCommonVersion=…`.

## Publishing

The `Publish plain-common` GitHub Actions workflow builds the KMP publications
and deploys them to the `gh-pages` branch when a `plain-common-v<version>` tag
is pushed. Configure the repository's Pages source to `gh-pages` / root once.
It can also be run manually with a version number. The workflow uses its
temporary `GITHUB_TOKEN`; no publishing credentials are stored in the repository.

For a local Maven cache, run `./gradlew :plain-common:publishToMavenLocal`.
