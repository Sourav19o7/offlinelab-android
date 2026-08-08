# How OfflineLab works

The learning-oriented companion to the README: why the code is shaped this way, not just what to
call.

## The central design tension: realistic randomness vs. testable randomness

A network-condition simulator is fundamentally about randomness — "30% of requests time out." But
code that calls `kotlin.random.Random.nextDouble()` directly is nearly impossible to unit test
precisely: you either run it thousands of times and assert on statistical distribution (slow,
flaky, imprecise), or you don't really test the failure branches at all.

The fix used throughout `OfflineLabInterceptor.kt` is **dependency-injected randomness** via a
tiny seam:

```kotlin
fun interface RandomSource { fun nextDouble(): Double }
```

Production code uses `RandomSource.system` (wraps `kotlin.random.Random.Default`). Tests use a
`ScriptedRandomSource` that returns an exact, pre-written sequence of values — see
`OfflineLabInterceptorTest.kt`. This turns "does a 0.5 timeout rate roughly time out half the
time" (statistical, slow to verify) into "does a roll of exactly `0.1` against a `0.5` threshold
timeout, every single time, deterministically" (a one-line assertion, instant).

**Why not just inject `kotlin.random.Random` itself?** `kotlin.random.Random` is an abstract class
whose only abstract member is `nextBits(bitCount: Int)`; every other method (`nextDouble()`,
`nextInt()`, ...) is implemented in terms of that using a specific bit-manipulation algorithm.
To make a test double return an exact `nextDouble()` value, you'd have to reverse-engineer that
algorithm and pick `nextBits()` inputs that produce it — fragile and unreadable. A one-method
`fun interface` sidesteps that entirely: the test double just returns whatever the test wants,
directly.

## Why the interceptor rolls three separate times, in a fixed order

Given a resolved profile with a `timeoutRate`, `failureRate`, and `httpErrorRate` all non-zero
(like `Flaky`), the interceptor does:

```kotlin
if (timeoutRate > 0 && randomSource.nextDouble() < timeoutRate) { /* timeout */ }
if (failureRate > 0 && randomSource.nextDouble() < failureRate) { /* failure */ }
if (httpErrorRate > 0 && randomSource.nextDouble() < httpErrorRate) { /* http error */ }
/* else: real request */
```

Each condition is an **independent** roll (not, e.g., one roll split into three ranges), and the
order is fixed and documented. This matters for two reasons: first, it means the three rates are
composable and easy to reason about independently — a `0.1` failure rate means "10% of requests
that get this far fail," full stop, not "10% of some combined budget." Second, fixing the order
means a `ScriptedRandomSource` with N values maps predictably to which check consumed which roll —
`OfflineLabInterceptorTest`'s "falls through to a real request" test relies on exactly this: three
scripted values, one per check, all "missing" their threshold.

## Why `Offline` is a distinct code path, not `failureRate = 1.0`

You could model `Offline` as `Custom(failureRate = 1.0)`. Instead it's a separate `offline: Boolean`
flag on `ResolvedProfile`, checked and short-circuited *before* the latency/random-roll logic runs
at all:

```kotlin
if (resolved.offline) {
    recordEvent(...)
    throw IOException("OfflineLab: simulated offline (profile=${resolved.name})")
}
```

The reasoning: "offline" is a qualitatively different claim than "10% chance of failure" — it
should be unconditional and instant (no latency added, no random draw needed, no chance it
"succeeds anyway"), matching what a real user flipping on airplane mode actually experiences.
Modeling it as `failureRate = 1.0` would technically work today, but it would also *consume a
random roll* for a case that should never need one — a subtle behavioral difference that a
dedicated boolean avoids entirely, and `ProfileResolverTest`'s `Offline is flagged offline`
test pins this down explicitly.

## Why `ProfileResolver` is a single pure function instead of methods on each profile

`NetworkProfile` is a `sealed class` of mostly empty `data object`s (`Normal`, `Offline`,
`Slow3G`, ...) plus one parameterized `Custom`. The temptation is to give each profile object its
own `resolve()` method. Instead, `ProfileResolver.resolve(profile): ResolvedProfile` is one
function with a `when` over every case, living in `internal/`.

This is a deliberate trade-off: it means `NetworkProfile` itself stays a pure data description
("this is what `Slow3G` *is*") with zero behavior, and the *mapping* from that description to
concrete numbers lives in exactly one place that's trivial to read top-to-bottom and exhaustively
unit test (`ProfileResolverTest` asserts the resolved numbers for every built-in). If every
profile's resolution logic were spread across per-object methods, understanding "what does Flaky
actually do" would mean reading multiple files instead of one function.

## Why `ProfileStore` uses `AtomicReference`, not a lock

`ProfileStore.get()`/`set()` back onto `java.util.concurrent.atomic.AtomicReference`, not a
`synchronized` block like `CircularBuffer` uses. The difference: `CircularBuffer.add()` is a
*compound* operation (check size, maybe remove, then add) that needs a lock to stay atomic as a
whole. `ProfileStore`'s operations are each already a single atomic action — `AtomicReference.get()`
and `.set()` are both lock-free CPU-level atomic operations on modern JVMs — so there's nothing a
lock would add except contention. This is chosen specifically because `get()` runs on **every**
intercepted request (a hot path relative to `CircularBuffer.add()`, which only runs when an event
is actually recorded), so avoiding lock overhead there matters more.

## Why the fake `Response` is built with `Response.Builder()` instead of throwing

For failure profiles (`Offline`, timeout, connection-failure), the interceptor **throws** —
matching what a real OkHttp call site expects on network failure (`try { call.execute() } catch
(e: IOException) { ... }`). But for `ServerErrors`/`RateLimited`/`httpErrorRate`, it **returns** a
real `okhttp3.Response` object built via `Response.Builder()`, with a real status code and a
`X-OfflineLab-Simulated: true` header, rather than throwing an `HttpException` or similar.

This matches how a real server error actually arrives at the call site: OkHttp treats a `500`
response as a *successful* HTTP exchange (the server did respond, just with an error status) —
`Response.isSuccessful` is `false`, but no exception is thrown. Simulating it any other way
(throwing on 500) would exercise a code path in the host app that a real 500 would never trigger,
defeating the point of the simulation. The `X-OfflineLab-Simulated` header exists purely so the
event history / debug UI can tell simulated responses apart from real ones without guessing —
never inspected by application response-parsing code, and never present on a real server response
(marking it in the header, not in the response body, avoids corrupting whatever body-parsing/
deserialization the host app's real success/error path expects).

## Testing without a real OkHttp call stack

`OfflineLabInterceptorTest` never opens a socket, starts `MockWebServer`, or builds a real
`OkHttpClient`. Instead, `FakeChain` implements `okhttp3.Interceptor.Chain` directly — a plain
Kotlin `class` implementing an interface OkHttp itself defines. Reading `OfflineLabInterceptor.kt`
shows it only ever calls two `Chain` methods: `request()` and `proceed(request)`. So `FakeChain`
implements those two meaningfully and throws `UnsupportedOperationException` on the rest
(`call()`, `connection()`, the timeout accessors) — if a future change to the interceptor started
calling one of those, the test would fail loudly and immediately rather than silently returning a
wrong default. This is the same "push framework dependencies to the edges" idea as ReproKit's
`DeviceInfoCollector` split, applied to OkHttp instead of the Android framework: the interceptor's
actual decision logic (`ProfileResolver`, `MatchEvaluator`, the random rolls) has zero dependency
on a real network stack, so none of the tests need one either.
