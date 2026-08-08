# Publishing

## Current status

OfflineLab `0.1.0` is **not published** to any Maven repository. Use it today via a Gradle
included build or by copying the `:offlinelab` module directly.

## Generate a local AAR

```bash
./gradlew :offlinelab:assembleRelease
# output: offlinelab/build/outputs/aar/offlinelab-release.aar
```

## Publish to Maven Local

Add to `offlinelab/build.gradle.kts`:

```kotlin
plugins {
    // ...existing plugins
    id("maven-publish")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "dev.local.androidtools"
            artifactId = "offlinelab"
            version = "0.1.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
```

then:

```bash
./gradlew :offlinelab:publishToMavenLocal
```

## Future: GitHub Packages / Maven Central

Same story as every other module in this project family: needs a signing key and/or a
`write:packages` token, both supplied only via GitHub Actions repository secrets — **never**
committed to this repo. `.gitignore` already excludes `local.properties`, `*.jks`, `*.keystore`,
and `keystore.properties`.
