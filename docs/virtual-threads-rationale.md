# Virtual Threads — Why They Matter for Play 1.x

This document explains what virtual threads are, why they're a good fit for Play 1.x's
blocking programming model, why three specific framework changes are all required to
realise the benefit, and what the actual performance improvement looks like.

Related plan entries: Phase 2B, Phase 3A, Phase 3D in `MODERNIZATION_PLAN.md`.

---

## The fundamental problem: blocking is only expensive if threads are expensive

When a Play controller action runs a database query, the thread executing it stops and
waits. It holds its position in the thread pool, occupying a slot, doing nothing, until
the database responds. For a PostgreSQL query taking 1ms, a thread is idle 99.9% of the
time it is "handling" that request.

This is why thread pool sizing matters so much. With `play.pool=11` (the default on a
10-core machine) and 256 concurrent connections all waiting on database queries, 245
connections are always queued. Throughput is capped at roughly:

```
max_throughput ≈ pool_size / query_latency
```

11 threads / 1ms = ~11,000 requests/second maximum, regardless of how fast the hardware
or database is.

The conventional fix is a larger thread pool. But platform threads are expensive — each
costs ~1MB of stack memory, and the OS scheduler must context-switch between them. The
pool sweep run during Phase 0D showed this directly: at `play.pool=256`, throughput
*dropped* compared to `play.pool=64` because 256 threads competing for 10 CPU cores
costs more in context switching than it gains from reduced queuing.

The result is a permanent tuning compromise: too small a pool and requests queue; too
large a pool and the OS thrashes. The right value changes with hardware and workload.

---

## What virtual threads do

A virtual thread is a thread managed by the JVM rather than the OS. When a virtual
thread blocks — on a database query, a network call, a `Thread.sleep()` — the JVM
**unmounts** it from the OS thread (called the *carrier*) that was running it. The
carrier is immediately free to pick up another virtual thread. When the blocking
operation completes, the JVM remounts the virtual thread on any available carrier and
it resumes from where it left off.

The practical consequence: you can have tens of thousands of virtual threads in flight
on a handful of carrier threads. Each carrier is always doing real work. There is no
queuing, no thread pool sizing problem, no context switching overhead between blocked
requests.

For Play 1.x specifically, this is the ideal model. The simple synchronous programming
style is preserved:

```java
// This code looks blocking — and it is — but with virtual threads the blocking
// is free: the carrier unmounts and handles another request while this one waits.
World world = World.findById(id);
world.randomNumber = ThreadLocalRandom.current().nextInt(1, 10001);
world.save();
```

No callbacks. No `CompletableFuture` chains. No reactive operators. The application
code stays unchanged and gains the concurrency characteristics of a reactive framework.

---

## Why three framework changes are all required

Virtual threads only unmount when they are *allowed* to unmount. A virtual thread gets
**pinned** — forced to stay on its carrier — whenever it enters a `synchronized` block
or `synchronized` method. While pinned, it behaves exactly like a platform thread: the
carrier is blocked, no other virtual thread can run on it, and the benefit disappears.

Play 1.x has two significant sources of `synchronized` pinning that must be removed
before virtual threads are effective:

### Phase 2B — Hibernate 6 (removes DB-layer pinning)

Every database operation in Hibernate 5 passes through `synchronized` session
management code. An EntityManager open, a query, a flush, a transaction commit — all of
these acquire `synchronized` locks internally. The virtual thread is pinned for the
entire DB operation, which is exactly when you most need it to unmount.

Hibernate 6 replaced those `synchronized` blocks with `ReentrantLock`. A virtual thread
can release a `ReentrantLock` before parking and re-acquire it on resumption, so DB
operations no longer pin. This is the highest-impact change: without it, virtual threads
are pinned during the majority of each request's execution time for any DB endpoint.

H2's JDBC driver also uses `synchronized` internally. Switching to PostgreSQL (which
uses a JDBC driver written without pervasive synchronization on the hot path) removes
this source of pinning as well.

### Phase 3A — Netty 4 (removes server-layer pinning)

When the Invoker thread finishes processing a request and calls `channel.write()` to
send the HTTP response, Netty 3's write path acquires a `synchronized` lock on the
channel object (`AbstractNioWorker.writeNow()`). This was confirmed by decompiling the
bytecode — `monitorenter`/`monitorexit` wraps the `currentWriteEvent` and
`inWriteNowLoop` field accesses. Every response write pins the virtual thread for the
duration.

Netty 4's strict EventLoop model eliminates this. Any write from outside the EventLoop
thread is submitted as a task to a lock-free `MpscQueue`:

```java
// Netty 4: write from non-EventLoop thread
eventLoop.execute(() -> channel.writeAndFlush(response));
// calling (virtual) thread holds no lock — it can unmount freely
```

Netty 4 is also required for JDK 21 compatibility independent of virtual threads, since
Netty 3 uses `sun.misc.Unsafe` and `sun.nio.ch` internals that are inaccessible on
JDK 21+ without `--add-opens`.

### Phase 3D — Virtual thread executor (the actual switch)

With the pinning sources removed, the switch itself is straightforward: replace the
fixed-size `ScheduledThreadPoolExecutor` in `play.Invoker` with a
`newVirtualThreadPerTaskExecutor()`. Every incoming request gets its own virtual thread.
The `play.pool` configuration value becomes irrelevant for request handling.

Background jobs (`@Every`, `@On` cron annotations) should remain on a small
platform-thread `ScheduledExecutorService` — long-lived scheduled tasks are not a good
fit for virtual threads.

As a bonus, once the Invoker runs on virtual threads, `Controller.await(int millis)` can
be implemented as a plain `Thread.sleep()` — the virtual thread parks for the specified
duration and the carrier handles other requests in the meantime. This replaces the
JavaFlow continuation bytecode transformation (Phase 3C) entirely on JDK 21+.

---

## The pinning picture across all three changes

| Pinning source | Netty 3 + Hibernate 5 | After 2B only | After 3A only | After 2B + 3A |
|---|---|---|---|---|
| Hibernate session management | Every DB op | **Gone** | Every DB op | **Gone** |
| H2/PostgreSQL JDBC driver | Every DB op | Reduced¹ | Every DB op | Reduced¹ |
| Netty write path | Every response | Every response | **Gone** | **Gone** |

¹ PostgreSQL's JDBC driver has far less `synchronized` usage than H2; switching to
PostgreSQL for production benchmarking removes most of this.

Attempting Phase 3D without 2B and 3A means virtual threads are pinned during DB
operations (the majority of each request's time for DB endpoints) and during every
response write. The result is a different implementation with effectively the same
throughput characteristics as platform threads.

---

## What the performance improvement looks like

This is not about making a single request faster. A request that queries PostgreSQL
still takes ~1ms. Virtual threads do not change per-request latency.

What changes is what happens with many concurrent requests all waiting on I/O
simultaneously:

| | Platform threads (current) | Virtual threads (after 2B + 3A + 3D) |
|---|---|---|
| Threads while waiting on DB | Holds OS thread slot, does nothing | Unmounted — carrier handles another request |
| Pool tuning required | Yes — sweet spot varies by hardware + DB latency | No |
| Throughput ceiling | `pool_size / query_latency` | `db_connection_pool / query_latency` |
| Context switching overhead | Grows with pool size | Fixed at carrier count (~20 IO threads) |
| Memory per concurrent request | ~1MB stack per platform thread | ~few KB per virtual thread |

The ceiling shifts from the thread pool to the database connection pool and the database
itself — which is the right bottleneck to have. Thread pool capacity is no longer a
constraint on how many concurrent requests the framework can serve.

### Why the Phase 0D pool sweep understates the benefit

The pool sweep (H2 in-memory, all endpoints) showed only modest gains from increasing
`play.pool` because H2 is an in-memory database with sub-millisecond query times. With
fast queries, even a small thread pool rarely blocks long enough for queuing to hurt.

The payoff is proportional to query latency. With PostgreSQL on a real network:

- Each query ≈ 1–5ms round trip
- 256 concurrent connections × 2ms average = 512ms of blocking per "cycle"
- Platform threads need 256+ pool slots to avoid queuing during that 512ms
- Virtual threads need 0 pool slots — they unmount and remount transparently

At high concurrency against a real database, virtual threads allow Play 1.x to serve
throughput proportional to database capacity rather than thread count — competitive with
reactive frameworks like Vert.x or Spring WebFlux, without requiring any change to the
synchronous, blocking programming model that existing Play 1.x applications are written
against.

---

## Summary

Virtual threads are a good fit for Play 1.x because Play is fundamentally a blocking,
thread-per-request framework. The programming model does not need to change. What needs
to change is what blocking *costs*: currently, it costs an OS thread slot; after 2B +
3A + 3D, it costs nothing.

The three changes form a single unit:

```
2B  Hibernate 6  ──►  removes synchronized from DB operations
3A  Netty 4      ──►  removes synchronized from response writes
3D  VT executor  ──►  replaces platform thread pool with virtual threads
                       (only effective once 2B and 3A have removed the pinning)
```

Each change is independently valuable (2B and 3A are also required for JDK 21+
compatibility regardless of virtual threads), but their performance benefit compounds:
together they eliminate all significant pinning sources and allow the virtual thread
executor to function as intended.
