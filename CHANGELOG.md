# Changelog

All notable changes to this project are documented in this file. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows
[SemVer](https://semver.org/).

## [0.1.0] - Unreleased

Initial MVP.

### Added

- `OfflineLab.initialize` / `OfflineLabConfig` with an allow/deny request matcher list.
- `NetworkProfile`: `Normal`, `Offline`, `Slow3G`, `HighLatency`, `RandomTimeout`, `Flaky`,
  `ServerErrors`, `RateLimited`, and `Custom(latencyMs, failureRate, timeoutRate, httpErrorRate)`.
- `OfflineLabInterceptor`, an OkHttp `Interceptor` applying latency, failures, simulated timeouts,
  and simulated HTTP error responses per the active profile.
- Deterministic testing support via the injectable `RandomSource` seam.
- Local, bounded event history (`OfflineLab.eventHistory()`) and an `OfflineLabEventListener`.
- Compose sample app with profile buttons, a custom-profile form, live event history, and a
  real-vs-simulated result indicator.

### Known limitations

- Simulated timeouts are `Thread.sleep` + a thrown `SocketTimeoutException`, not a true
  transport-level timeout — see README's "Timeout simulation limitations" section.
- No VPN/root-based interception (by design — see README Non-goals).
- `RateLimited` always returns 429 for matched requests (rate = 1.0); it does not model a rolling
  request-count window.
