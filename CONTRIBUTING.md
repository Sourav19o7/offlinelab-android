# Contributing to OfflineLab

## Setup

```bash
git clone https://github.com/Sourav19o7/offlinelab-android.git
cd offlinelab-android
./gradlew :offlinelab:testDebugUnitTest
```

Open the folder in Android Studio (Hedgehog+); `:offlinelab` (library) and `:sample` (Compose demo)
both show up.

## Before opening a PR

```bash
./gradlew :offlinelab:testDebugUnitTest
./gradlew :offlinelab:lintDebug
./gradlew :offlinelab:assembleDebug :sample:assembleDebug
```

## Code style

- `.editorconfig` in the repo root defines formatting.
- Public API lives in `dev.local.androidtools.offlinelab`; everything else belongs under
  `internal/`.
- `OfflineLabInterceptor` is the one exception — it's public (not internal) specifically so it can
  be constructed and tested directly, independent of the `OfflineLab` singleton.

## Adding a new built-in profile

1. Add the `data object`/`data class` case to `NetworkProfile`.
2. Map it to concrete parameters in `internal/ProfileResolver.kt`.
3. Add a `ProfileResolverTest` case asserting the resolved parameters.
4. Add it to the sample app's `builtInProfiles` list and to the README's profile table.

## Adding randomness-dependent behavior

Never call `kotlin.random.Random` directly inside `OfflineLabInterceptor`. Always go through the
injected `RandomSource` so the behavior stays deterministically testable with a scripted sequence
(see `OfflineLabInterceptorTest`'s `ScriptedRandomSource`).

## Reporting bugs / requesting features

Use the issue templates. Anything touching whether simulation could leak into a release build or
affect traffic outside the host app gets reviewed with extra scrutiny — see `SECURITY.md`.
