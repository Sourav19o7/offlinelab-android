# Security Policy

## Supported versions

OfflineLab is pre-1.0 (`0.1.0`). Only the latest published version is supported.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security-sensitive findings (e.g. a way for
simulation to run in a release build, or a way for it to affect traffic outside the host app).
Open a [private security advisory](../../security/advisories/new) instead, or contact the
maintainer directly.

## Threat model

OfflineLab is a **local-only, debug-time** tool. Its threat model assumes:

- The host app explicitly wires `OfflineLabConfig.enabled` to its own debug flag; OfflineLab has
  no way to verify this from outside the app.
- OfflineLab only ever affects `OkHttpClient` instances the host app explicitly builds with
  `OfflineLab.interceptor()` added — it has no mechanism to see or alter any other client, any
  other app's traffic, or system-wide connectivity.
- Simulated responses are visually distinguishable (via the `X-OfflineLab-Simulated` header and
  the event history) so a developer can never mistake a simulated result for a real server
  response during debugging.

## What would count as a security bug here

- The interceptor doing anything when `enabledProvider()` returns `false` — even sleeping,
  drawing a random number, or emitting an event.
- Any code path that reads or logs a request/response **body**, or an `Authorization` header.
- Any code path that could affect traffic on an `OkHttpClient` the host app did **not** explicitly
  attach `OfflineLab.interceptor()` to.
- Any attempt at VPN-based, root-based, or system-wide network interception. This project
  deliberately never does this — finding a place where it does would be a critical bug.
