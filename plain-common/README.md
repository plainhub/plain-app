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

Add the Maven repository that hosts the artifact to the consuming project's
repositories. The default version can be changed with `-PplainCommonVersion=…`.

## Publishing

`publishToMavenLocal` publishes the KMP metadata and platform publications to
the local Maven cache. To publish to a Maven repository, set
`plainCommonMavenUrl`; credentials can be supplied through
`plainCommonMavenUsername` and `plainCommonMavenPassword` Gradle properties.

```shell
./gradlew :plain-common:publishAllPublicationsToPlainCommonRepository \
  -PplainCommonVersion=1.0.0 \
  -PplainCommonMavenUrl=https://your-maven-repository
```
