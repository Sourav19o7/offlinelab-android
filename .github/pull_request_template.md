## What changed and why

## Checklist

- [ ] `./gradlew :offlinelab:testDebugUnitTest` passes
- [ ] `./gradlew :offlinelab:lintDebug` passes
- [ ] `./gradlew :offlinelab:assembleDebug :sample:assembleDebug` passes
- [ ] README updated if public API or behavior changed
- [ ] New/changed profiles have a deterministic unit test (seeded `RandomSource`)
- [ ] No secrets, tokens, or keystores are included in this diff
- [ ] Verified the change cannot affect traffic outside the host app's own OkHttpClient
