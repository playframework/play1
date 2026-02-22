# Play Framework 1.x Modernization Plan

## Goals

**JDK 21+ Compatibility** — Make the framework run correctly on modern JVMs without
`--add-opens` hacks or deprecated flags.

**Performance** — Eliminate the runtime overhead introduced by bytecode enhancement and
the request handling architecture, particularly the reflection-heavy field access
interception.

Some changes serve both goals. The plan is structured in phases where each phase
produces a working, testable state.

---

## Dependency map

```
Phase 0 (prerequisite: establish baseline)
  ├── 0A  Get ant test passing on current JDK (11 and 17)
  ├── 0B  Add JDK 21 to CI matrix (expected failures)
  ├── 0C  Audit and expand test coverage for areas we're changing
  └── 0D  Establish TechEmpower benchmark baseline

Phase 1 (independent leaf changes, no ordering between them)
  ├── 1A  Remove -noverify flag
  ├── 1B  Remove SecurityManager usage
  ├── 1C  Extract constructor enhancer from PropertiesEnhancer
  ├── 1D  Add Method cache to PropertiesEnhancer.FieldAccessor
  └── 1E  Add opt-in JPA standard dirty checking (bypass saveAndCascade)

Phase 2 (can be done in parallel tracks after Phase 1)
  ├── Track A: JPA/Hibernate modernization
  │     2A  javax.persistence → jakarta.persistence  ──→  2B  Hibernate 5 → 6
  │
  └── Track B: Bytecode enhancer removal
        2C  Remove PropertiesEnhancer  (depends on 1C, 1D)
        2D  Remove LocalvariablesNamesEnhancer  (independent)

Phase 3 (depends on Phase 2 completion)
  ├── 3A  Netty 3 → Netty 4  (or alternative server)
  ├── 3B  Groovy 3 → Groovy 4
  └── 3C  Replace/remove JavaFlow continuations

Phase 4 (depends on Phase 3)
  └── 4A  Remove -javaagent requirement for production mode
```

---

## Phase 0 — Establish test baseline

Every subsequent phase relies on tests to verify we haven't broken anything. Before
making any changes, we need to know that the existing tests pass, that they cover the
areas we're about to modify, and that we can detect JDK 21 failures specifically.

### 0A. Verify existing tests pass on JDK 11 and 17
**Scope:** Investigation and bug fixes

Run `ant test` locally on both JDK 11 and JDK 17 (the two versions in the current CI
matrix). Fix any existing failures before proceeding. The goal is a clean green
baseline that we can diff against.

CI currently runs on: JDK 11 (ubuntu), JDK 17 (ubuntu + windows). Confirm all CI
builds are green on the master branch. If there are known-broken tests, either fix
them or document and exclude them so the remaining suite is a reliable signal.

Also run `ant unittest` separately to isolate framework unit test failures from
integration test failures.

### 0B. Add JDK 21 to CI matrix (allow failures)
**Scope:** 1 file

Add JDK 21 to `.github/workflows/build-test.yml` as an additional matrix entry with
`continue-on-error: true`. This gives us a continuously updated list of what fails on
JDK 21 without blocking PRs.

File:
- `.github/workflows/build-test.yml` — add `21` to the `jdk` matrix array, add a
  `continue-on-error` conditional for JDK 21

This creates the scoreboard for the entire modernization effort. As we complete phases,
JDK 21 failures should decrease until the `continue-on-error` can be removed.

### 0C. Audit test coverage and add missing tests
**Scope:** New test files in `framework/test-src/`
**Status:** Partially complete — see below.

**Added (commit 4c8d51a):**
- `framework/test-src/play/db/jpa/HibernateInterceptorTest.java` —
  Covers `findDirty()` explicit-save blocking, collection callback gating,
  `onSave()` ThreadLocal storage, and `afterTransactionCompletion()` cleanup.
  Guards Phase 1E and Phase 2B.
- `framework/test-src/play/classloading/enhancers/PropertiesEnhancerTest.java` —
  Covers getter/setter generation, `@PlayPropertyAccessor` annotation, boolean
  field handling, default constructor addition, and behavioral round-trip tests
  (getter returns field value, setter updates field). Uses Javassist bytecode
  loading with a child-first ClassLoader to test the actual enhanced bytes.
  Guards Phase 2C.
- `framework/test-src/play/classloading/enhancers/ContinuationEnhancerTest.java` —
  Covers `isEnhanced()` runtime detection for unknown classes, unenhanced classes,
  and classes with null `javaClass` (which exposed a latent NPE bug, now fixed).
  Full transformation test (await() detection + JavaFlow transform) requires a
  real Play classloader and is covered by the sample app integration tests.
  Guards Phase 3C.
- Also fixed a latent NPE in `ContinuationEnhancer.isEnhanced()`: it now
  null-checks `appClass.javaClass` before calling `isAssignableFrom()`.

**Still needed:**
- `JPABase` Hibernate internal API usage — `saveAndCascade()`, `cascadeOrphans()`,
  and the Hibernate SPI calls (`SessionImpl`, `CollectionEntry`, `PersistenceContext`,
  `HibernateProxy`) require a live Hibernate session. A proper unit test needs an
  in-memory H2 database with Hibernate bootstrapped. This is non-trivial and is
  better done as part of Phase 2B prep work, alongside the Hibernate 6 migration.
  Priority: write before starting Phase 2B. The sample app integration tests
  (yabe, forum) cover these paths indirectly in the meantime.
- `ControllersEnhancer` ThreadLocal rewriting and auto-redirect injection — testing
  requires a controller class processed through the full enhancer chain. Defer to
  Phase 2C prep.
- `ConstructorEnhancer` — will be new code (Phase 1C); write tests alongside it.
- Server boot/response smoke test — difficult to unit test due to Netty coupling.
  Defer to Phase 3A prep.

**Coverage confirmed adequate (no gaps blocking immediate phases):**
- `BeanWrapper` — `BeanWrapperTest.java` covers binding via fields and methods.
  The `@PlayPropertyAccessor` filter in `BeanWrapper.java:66,70` is exercised by
  existing tests; no additional coverage needed before Phase 2C.
- `ActionInvoker` — `ActionInvokerTest.java` is comprehensive (interceptors,
  exception unwrapping, static/non-static/Scala actions). Does not test
  continuation/await() paths, which are integration-tested via sample apps.
- `GroovyTemplate` — `GroovyTemplateTest.java` covers compilation and rendering.
  Confirmed passing on JDK 17; the JDK 23+ failure ("Unsupported class file
  major version") is expected and tracked via Phase 3B (Groovy 4 upgrade).
- `JPQLTest.java` — covers JPQL query generation from method names.

### 0D. Establish TechEmpower benchmark baseline
**Scope:** New directory `samples-and-tests/tfb/`

The [TechEmpower Framework Benchmarks](https://www.techempower.com/benchmarks/#section=data-r23)
(TFB) are the standard reference for comparing web framework throughput. A play1
implementation existed in the TFB repository from 2013 until it was removed in March
2025 (PR #9655) when the Typesafe download URL broke and the OpenJDK 10 base image
became unmaintainable.

Restoring a runnable TFB benchmark serves two purposes: it gives us a credible
before/after number to publish alongside the modernization work, and it catches
throughput regressions introduced by any phase.

**What the original play1 TFB implementation covered:**

| Test type | URL | Status |
|-----------|-----|--------|
| Plaintext | `/plaintext` | Implemented |
| JSON serialization | `/json` | Implemented |
| Single DB query | `/db` | Implemented |
| Multiple DB queries | `/query?queries=N` | Implemented |
| Fortunes | `/fortunes` | **Never implemented** |
| DB updates | `/update?queries=N` | **Never implemented** |
| Cached queries | `/cached-queries?count=N` | **Never implemented** |

**Implementation:**

Create `samples-and-tests/tfb/` as a self-contained Play 1.x app. Model it on the
original TFB play1 implementation:

- `app/controllers/Application.java` — action methods for each benchmark type
- `app/models/World.java` — `@Entity` extending `GenericModel` with `id` and
  `randomNumber` fields (Long); matches the TFB `hello_world` schema
- `app/models/Fortune.java` — `@Entity` for the fortunes test (new; wasn't in
  the original)
- `conf/routes` — routes for all benchmark URLs
- `conf/application.conf` — production-mode config pointing at `tfb-database`
  (MySQL) with an aggressively tuned JDBC URL and connection pool settings
- `Dockerfile` — builds from this repo's `play` CLI rather than downloading
  Play 1.5.2; uses a current JDK base image

**Complete the missing test types** that the original never implemented:

- **Fortunes** — query all `Fortune` rows, add a server-generated entry
  (`"Additional fortune added at request time."`), sort by message, render via a
  Groovy template. This exercises template rendering under load.
- **Updates** — fetch N `World` rows, update `randomNumber` on each, persist,
  return JSON. Uses `jpa.explicitSave=false` (Phase 1E) if available, otherwise
  calls `.save()` explicitly.
- **Cached queries** — same shape as the queries test but backed by Play's cache
  (`Cache.get`/`Cache.set`). Tests EhCache throughput.

**Database schema** (standard TFB `hello_world` schema):
```sql
CREATE TABLE World (id INT, randomNumber INT);
CREATE TABLE Fortune (id INT, message VARCHAR(2048));
```

**Baseline measurement procedure:**

Run against the framework as it stands at the end of Phase 0 (JDK 17, Hibernate 5,
Netty 3). Record requests/sec for each test type. Re-run after each phase that claims
a performance benefit (1D, 1E, 2B, 2C, 3A) and record the delta.

The TFB tooling (`tfb --test play1`) can run the full suite in a controlled
environment. For local spot checks, `wrk` is sufficient:
```
wrk -t4 -c256 -d30s http://localhost:9000/plaintext
wrk -t4 -c256 -d30s http://localhost:9000/json
wrk -t4 -c256 -d30s "http://localhost:9000/query?queries=20"
```

**Validation:** The app boots in PROD mode (`play run --%prod`) and all benchmark
URLs return correct responses per the TFB verification spec (correct JSON structure,
correct Content-Type headers, correct Fortune sort order).

**Baseline results** (recorded 2026-02-21):

Environment: macOS Darwin 24.6.0, OpenJDK 23.0.2, H2 in-memory DB (dev mode).
Run with: `./samples-and-tests/tfb/benchmark.sh 30 256` (30s, 4 threads, 256 connections).

| Endpoint | RPS | Avg lat | p50 | p99 |
|----------|-----|---------|-----|-----|
| plaintext | 7,463 | 4.23ms | 4.27ms | 5.14ms |
| json | 7,917 | 4.02ms | 4.04ms | 4.53ms |
| db (1 query) | 7,169 | 4.46ms | 4.46ms | 5.22ms |
| queries q=1 | 6,960 | 4.59ms | 4.61ms | 5.16ms |
| queries q=20 | 4,954 | 6.45ms | 6.44ms | 7.26ms |
| queries q=500 | 676 | 47.07ms | 47.30ms | 50.63ms |
| updates q=1 | 6,603 | 4.84ms | 4.85ms | 5.57ms |
| updates q=20 | 2,504 | 12.76ms | 12.74ms | 14.10ms |

Notes:
- H2 in-memory means DB overhead is near-zero; expect plaintext/json/db gap to widen with PostgreSQL.
- `updates q=20` is ~2× slower than `queries q=20` (12.76ms vs 6.45ms) — the ~6ms delta is JPA dirty-check + flush cost, the target of Phase 1E.
- Zero errors across all endpoints (wrk reports real socket/HTTP errors, not Content-Length variance like ab).
- Re-run `benchmark.sh` after each phase claiming a performance benefit (1D, 1E, 2B, 2C, 3A) and record delta here.

---

## Phase 1 — Low-risk preparatory changes

These are small, independent changes. Each can be a separate PR. All existing tests
should continue to pass after each one.

### 1A. Remove `-noverify` flag
**Goal:** JDK 21+
**Scope:** 2 files

`-noverify` is silently ignored on JDK 21+ and was deprecated since JDK 13. Removing
it now surfaces any bytecode that doesn't pass verification, which we need to know
about before making deeper changes.

Files:
- `framework/pym/play/application.py:302` — remove `java_args.append('-noverify')`
- `framework/pym/play/commands/eclipse.py:39` — remove `-noverify` from VM arguments

**Validation:** Run `ant test` with JDK 17+ (where `-noverify` is already partially
ignored). Any `VerifyError` failures identify enhancers that produce invalid stack maps
and must be fixed before proceeding.

**Risk:** Medium. If enhancers generate bytecode with invalid stack maps this will
surface as failures. Those failures are bugs we need to fix anyway — they're just
currently hidden.

### 1B. Remove SecurityManager usage
**Goal:** JDK 21+
**Scope:** 3 files

`SecurityManager` is deprecated-for-removal in JDK 17 and fully removed in JDK 24.

Files:
- `framework/src/play/utils/PThreadFactory.java:13-14` — remove
  `System.getSecurityManager()` call; use `Thread.currentThread().getThreadGroup()`
  unconditionally
- `framework/pym/play/application.py:304-310` — remove the `java.policy` /
  `java.security.manager` configuration block
- Remove/update any documentation referencing `java.policy` configuration

### 1C. Extract constructor enhancer from PropertiesEnhancer
**Goal:** Performance (preparation)
**Scope:** 2 files

PropertiesEnhancer does three things: generate constructors, generate accessors,
rewrite field access. The constructor generation is needed by JPA and data binding
regardless of whether we keep the other two. Extract it into a standalone
`ConstructorEnhancer` so it can survive the removal of PropertiesEnhancer.

Files:
- New: `framework/src/play/classloading/enhancers/ConstructorEnhancer.java` — just
  the `addDefaultConstructor` logic from PropertiesEnhancer
- `framework/src/play/plugins/EnhancerPlugin.java` — add `ConstructorEnhancer` to
  the enhancer array

**Validation:** Run `ant test`. Behavior should be identical since PropertiesEnhancer
still runs and still generates constructors — the new enhancer is additive.

### 1D. Add Method cache to PropertiesEnhancer.FieldAccessor
**Goal:** Performance
**Scope:** 1 file

The immediate performance win without any breaking changes. `FieldAccessor` currently
calls `o.getClass().getMethod(getter)` on every single field read/write. Add a
`ConcurrentHashMap<Class<?>, Map<String, Method>>` cache so reflective lookup happens
once per class per field, not once per access.

File:
- `framework/src/play/classloading/enhancers/PropertiesEnhancer.java` — add a static
  cache in `FieldAccessor`, look up from cache in `invokeReadProperty` and
  `invokeWriteProperty`, invalidate on classloader change

**Validation:** Run `ant test`. Behavior should be identical, just faster. Benchmark
before/after with a data-heavy sample app (yabe) to measure improvement.

### 1E. Add opt-in JPA standard dirty checking (bypass `saveAndCascade`)
**Goal:** Performance
**Scope:** 3 files
**Config:** `jpa.explicitSave=false` (new flag, default `true` to preserve existing behavior)

Play overrides Hibernate's standard dirty checking with an explicit-save model:
`HibernateInterceptor.findDirty()` returns an empty array for any entity where
`willBeSaved` is `false`, effectively blocking Hibernate from flushing changes unless
`.save()` was called. The `saveAndCascade` method in `JPABase._save()` walks the
entire object graph via reflection twice per save (once before flush, once after) to
propagate the `willBeSaved` flag to cascaded entities. This is O(graph-size)
reflection work with zero caching — field metadata, annotation checks, and
`setAccessible` calls are repeated on every save of every entity in the cascade chain.

For models with many relationships this becomes a serious bottleneck. The manual
cascade walk also duplicates what Hibernate does natively, and uses Hibernate internal
SPIs (`SessionImpl`, `CollectionEntry`, `CollectionPersister`, `PersistenceContext`)
that change in Hibernate 6 (Phase 2B).

When `jpa.explicitSave=false`, switch to standard JPA/Hibernate semantics:
- `_save()` just calls `persist()` (if new) + `flush()` — no `saveAndCascade` walk
- `HibernateInterceptor.findDirty()` returns `null` (defer to Hibernate's own dirty
  check) instead of blocking with an empty array
- Collection interceptor methods (`onCollectionUpdate`, etc.) return `true`
  unconditionally instead of gating on `willBeSaved`
- Cascading is handled entirely by Hibernate based on `CascadeType` annotations

This means any field mutation on a managed entity will be flushed at transaction
commit, which is standard JPA behavior but differs from Play's current explicit-save
contract. Hence the opt-in flag.

Files:
- `framework/src/play/db/jpa/JPABase.java` — in `_save()`, check config flag; when
  disabled, skip `saveAndCascade` and just persist + flush
- `framework/src/play/db/jpa/HibernateInterceptor.java` — check config flag in
  `findDirty()` and collection callbacks; when disabled, return `null`/`true` to let
  Hibernate manage dirty checking
- `framework/src/play/db/jpa/JPAPlugin.java` — read `jpa.explicitSave` from config
  and expose it (e.g., static boolean on `JPAPlugin` or `JPA`)

**Behavioral change when `jpa.explicitSave=false`:**
- Mutations on managed entities are flushed at commit even without `.save()`
- `.save()` still works (calls persist/flush) but is only needed for new entities
- This is standard JPA semantics and matches what most other JPA frameworks do

**Validation:** Run `ant test` with both `jpa.explicitSave=true` (default, existing
behavior) and `jpa.explicitSave=false`. The false case may surface tests that mutate
entities without intending to persist — these would be test bugs or cases where the
explicit-save guard was masking unintended writes.

**Relationship to Phase 2B:** When Hibernate 6 lands, the `saveAndCascade` code must
change anyway (the internal SPIs it uses moved or were removed). If
`jpa.explicitSave=false` proves reliable, the default can flip in Phase 2B, and the
legacy `saveAndCascade` code path can be removed entirely rather than ported to
Hibernate 6 APIs.

---

## Phase 2 — Parallel tracks

These two tracks can proceed independently. Track A is primarily for JDK 21+
compatibility; Track B is primarily for performance. They meet at the Hibernate 6
upgrade which serves both goals.

### Track A: JPA/Hibernate modernization

#### 2A. javax.persistence → jakarta.persistence
**Goal:** JDK 21+ (prerequisite for Hibernate 6)
**Scope:** ~15 files, ~70 import lines + string literals

Mechanical find-and-replace of the JPA namespace. This must be done before upgrading
Hibernate because Hibernate 6 only supports `jakarta.persistence`.

Files (all in `framework/src/play/db/jpa/` unless noted):
- `Model.java` — 3 imports
- `GenericModel.java` — 10 imports + 9 string literals in error messages
- `JPABase.java` — 7 imports
- `JPA.java` — 6 imports
- `JPAPlugin.java` — 9 imports + 2 property string literals (lines 215-216)
- `JPAEnhancer.java` — 2 hardcoded annotation name strings (lines 25, 32)
- `JPAModelLoader.java` — 13 imports
- `JPASupport.java` — 1 import
- `JPQL.java` — 2 imports
- `FileAttachment.java` — 2 imports
- `Blob.java` — (check for JPA annotations)
- `PersistenceUnitInfoImpl.java` — 5 imports
- `framework/src/play/test/Fixtures.java` — 1 import
- `framework/src/play/db/helper/JpaHelper.java` — 1 import

Also need to swap the JPA API JAR in `framework/lib/` from `javax.persistence` to
`jakarta.persistence`.

**Validation:** `ant test` — everything should work with Hibernate 5.6 + the Jakarta
JPA API artifact (Hibernate 5.6 has a `hibernate-core-jakarta` variant that supports
`jakarta.persistence` while keeping the same Hibernate API).

**Breaking change for users:** All user application `@Entity`, `@Id`, `@ManyToOne`
etc. imports must change. This is the single largest breaking change in the plan.
Document a migration script (sed/find-replace).

#### 2B. Hibernate 5.6 → Hibernate 6
**Goal:** JDK 21+ and Performance
**Scope:** ~10 files, significant API changes
**Depends on:** 2A

Hibernate 6 is the first version with official JDK 21 support. It also eliminates the
need for `--add-opens` flags that Hibernate 5 requires for runtime proxy generation.

Key changes:

**JPAPlugin.java** — The bootstrap API changed:
- `EntityManagerFactoryBuilderImpl` and `PersistenceUnitInfoDescriptor` moved/changed
  in Hibernate 6
- `org.hibernate.ejb.HibernatePersistence` → replaced by
  `org.hibernate.jpa.HibernatePersistenceProvider` (already partially using this in
  `PersistenceUnitInfoImpl`)
- Dialect class names changed: `MySQL8Dialect` → `MySQLDialect`,
  `Oracle10gDialect` → `OracleDialect`, many removed entirely (Hibernate 6 has
  automatic dialect detection)
- Property `org.hibernate.readOnly` may have changed

**JPABase.java** — Uses Hibernate internal SPIs directly:
- `org.hibernate.collection.internal.PersistentMap` — package moved in Hibernate 6
- `org.hibernate.engine.spi.CollectionEntry` — API changes
- `org.hibernate.engine.spi.EntityEntry` — API changes
- `org.hibernate.persister.collection.CollectionPersister` — API changes
- `org.hibernate.internal.SessionImpl` — internal API changes

**HibernateInterceptor.java** — `EmptyInterceptor` was removed in Hibernate 6;
replace with implementing `Interceptor` interface directly.

**MySQLDialect.java** — extends `MySQLInnoDBDialect` which was removed; replace with
extending `org.hibernate.dialect.MySQLDialect` directly.

**Blob.java** — `UserType` interface changed in Hibernate 6 (method signatures differ).

**EvolutionQuery.java** — References `Oracle8iDialect`, `MySQLDialect` for detection;
these class names changed.

**DB.java** — imports `SessionImpl`.

**JARs to update in `framework/lib/`:**
- `hibernate-core-5.6.5.Final.patched.jar` → `hibernate-core-6.x.jar`
- `hibernate-c3p0-5.6.15.Final.jar` → update or remove (HikariCP preferred)
- `hibernate-ehcache-5.6.15.Final.jar` → Hibernate 6 uses `hibernate-jcache` instead
  of direct EhCache integration
- `hibernate-commons-annotations-5.1.2.Final.jar` → merged into hibernate-core in 6
- `hibernate-validator-5.4.3.Final.jar` → update to 8.x (for jakarta.validation)

**Validation:** Full `ant test` suite. JPA behavior is heavily tested through the
sample apps (yabe, forum, etc.). Run each sample app's tests individually to isolate
failures.

### Track B: Bytecode enhancer removal

#### 2C. Remove PropertiesEnhancer
**Goal:** Performance
**Scope:** 3 framework files + breaking change for users
**Depends on:** 1C (constructor enhancer extraction)

Remove the accessor generation and field-access rewriting. Keep the standalone
`ConstructorEnhancer` from Phase 1C.

Framework changes:
- `framework/src/play/plugins/EnhancerPlugin.java` — remove `PropertiesEnhancer`
  from the array (keep `ConstructorEnhancer`)
- `framework/src/play/data/binding/BeanWrapper.java:66,70` — remove the
  `@PlayPropertyAccessor` annotation check (it becomes irrelevant since no methods
  carry the annotation anymore)
- `framework/src/play/classloading/enhancers/PropertiesEnhancer.java` — delete the
  file, or keep it as dead code behind `play.propertiesEnhancer.enabled=true` for a
  transition period

Use the existing config flag `play.propertiesEnhancer.enabled` (already defaults to
`true` in `PropertiesEnhancer.java:31`) to allow a transition period: change the
default to `false`, document the change, let users opt back in if they depend on
generated accessors.

**Breaking change for users:**
- `user.getName()` will fail if the user didn't write the getter — only relevant for
  code that explicitly calls generated accessors rather than using field syntax
- Custom getters with logic (e.g., `getName()` returns `name.toUpperCase()`) will no
  longer be called transparently from `user.name` field access in Java code — Groovy
  templates will still call getters via Groovy's MOP
- Annotations on generated accessors won't exist — unlikely to matter since the
  generated methods had no user-specified annotations

**Validation:** Run `ant test`. Focus on data binding tests (the `just-test-cases`
sample app) and template rendering tests.

#### 2D. Remove LocalvariablesNamesEnhancer
**Goal:** Performance
**Scope:** ~5 files
**Independent of other Phase 2 items**

This enhancer exists so `render(user, posts)` can look up the variable names "user"
and "posts" at runtime. The alternative is to require explicit naming:
`render(Map.of("user", user, "posts", posts))` or a new helper like
`renderArgs("user", user, "posts", posts)`.

This is a larger API break than PropertiesEnhancer removal. Options:

**Option A: Remove entirely.** Change `render()` to require named arguments.
Breaks all existing controller code that uses `render(var1, var2, ...)`.

**Option B: Replace with annotation processor.** A compile-time annotation processor
could capture local variable names from the source and generate the name mapping,
avoiding the runtime bytecode instrumentation. This preserves API compatibility but
adds a build step.

**Option C: Use Java 21+ `LocalVariable` API.** JDK 21+ has better reflection support
for local variables via `StackWalker`. This doesn't help on JDK 11-17 but could be a
forward-looking alternative.

**Recommendation:** Option A with a compatibility helper. Add a `renderArgs()` method
that takes name-value pairs, deprecate the magic `render()`, and provide a migration
tool that rewrites `render(user, posts)` → `render("user", user, "posts", posts)`.

Files:
- `framework/src/play/classloading/enhancers/LocalvariablesNamesEnhancer.java` — remove or disable
- `framework/src/play/plugins/EnhancerPlugin.java` — remove from array
- `framework/src/play/mvc/Controller.java` — add explicit-naming render variant
- `framework/src/play/classloading/enhancers/ControllersEnhancer.java` — remove
  `LocalVariablesSupport` interface check if no longer needed

**Validation:** This will break most sample app tests initially. Each sample app's
controllers must be migrated to explicit render arguments.

---

## Phase 3 — Infrastructure replacement

These are the large, high-effort changes that depend on Phase 2 being stable.

### 3A. Netty 3 → Netty 4 (or alternative)
**Goal:** JDK 21+
**Scope:** ~3200 lines across 11 files (full rewrite of server layer)

Netty 3.10.6 uses JDK internal APIs (`sun.misc.Unsafe`, `sun.nio.ch`) that are
inaccessible on JDK 21+ without `--add-opens`. The Netty 3 → 4 migration is a
complete API rewrite (not just package changes).

**Package change:** `org.jboss.netty` → `io.netty`

**API changes:**
- `ChannelPipelineFactory` → `ChannelInitializer<Channel>`
- `ServerBootstrap` constructor API completely different
- `ChannelBuffer` → `ByteBuf`
- `HttpRequest`/`HttpResponse` API restructured
- `ChannelHandler` lifecycle callbacks renamed
- Cookie API changed
- WebSocket API changed

Files to rewrite:
- `server/Server.java` (177 lines)
- `server/PlayHandler.java` (1233 lines) — the bulk of the work
- `server/HttpServerPipelineFactory.java` (83 lines)
- `server/FileService.java` (341 lines)
- `server/FileChannelBuffer.java` (343 lines)
- `server/StreamChunkAggregator.java` (84 lines)
- `server/FlashPolicyHandler.java` (54 lines)
- `server/ssl/SslHttpServerContextFactory.java` (151 lines)
- `server/ssl/SslHttpServerPipelineFactory.java` (89 lines)
- `server/ssl/SslPlayHandler.java` (65 lines)

**Alternative:** The framework already has a `modules/grizzly` module (~500 lines)
that provides an alternative HTTP server. Evaluate whether Grizzly (now Eclipse
Grizzly, JDK 21 compatible) could replace Netty as the default server, which would be
less work than porting the Netty layer to Netty 4.

**Validation:** Every sample app test exercises the HTTP server. The full `ant test`
suite is the validation. Also manually test: WebSocket connections, SSL/TLS, chunked
responses, file uploads, large file serving, keep-alive behavior.

### 3B. Groovy 3 → Groovy 4
**Goal:** JDK 21+
**Scope:** Low (mostly JAR swap + package rename)

Groovy 4.x has full JDK 21 support and moves from `org.codehaus.groovy` to
`org.apache.groovy` (though `org.codehaus.groovy` still works via compatibility
bridge in Groovy 4).

Files:
- `framework/src/play/templates/GroovyTemplate.java` — imports from
  `org.codehaus.groovy.control.*` and `org.codehaus.groovy.runtime.*`
- `framework/src/play/templates/GroovyTemplateCompiler.java` — Groovy imports
- `framework/src/play/templates/FastTags.java` — `groovy.lang.Closure`,
  `org.codehaus.groovy.runtime.NullObject`
- JAR swap in `framework/lib/`

**Validation:** Template rendering tests across all sample apps. The Groovy upgrade
is usually smooth for compiled Groovy code — the main risk is internal API changes in
the `CompilationUnit`/`CompilerConfiguration` classes.

### 3C. Replace or remove JavaFlow continuations
**Goal:** JDK 21+ and Performance
**Scope:** High
**Depends on:** 2C or can be done independently

Apache Commons JavaFlow is effectively abandoned. The library uses ASM to rewrite
bytecode for stack capture/restore, and it's the most fragile part of the enhancement
pipeline. It's also what most needs `-noverify` to work.

**The `await()` methods affected:**
- `Controller.await(String timeout)` — suspends with timeout
- `Controller.await(int millis)` — suspends with millis
- `Controller.await(Future)` — suspends until future completes

**Options:**

**Option A: Replace with `CompletableFuture` callbacks.** The callback variants of
`await()` already exist (`await(int, F.Action0)`, `await(Future, F.Action<T>)`) and
don't use continuations. Deprecate the continuation-based variants and guide users
toward callbacks.

**Option B: Replace with virtual threads (JDK 21+).** Virtual threads make the
continuation-style `await()` possible without bytecode transformation — just block
the virtual thread. This would be the most API-compatible replacement but limits the
minimum JDK version to 21.

**Option C: Remove entirely.** Drop `await()` support. This is the simplest but most
breaking option.

**Recommendation:** Option A for JDK 11-20 compatibility, with Option B as the
implementation for JDK 21+ (detect at runtime). This lets the same API work on both
JVM versions with different underlying mechanisms.

Files:
- `framework/src/play/classloading/enhancers/ContinuationEnhancer.java` — remove or
  make conditional
- `framework/src/play/mvc/Controller.java` — continuation-based `await()` methods
- `framework/src/play/mvc/ActionInvoker.java` — continuation resume logic
  (references `org.apache.commons.javaflow.Continuation`)
- `framework/src/play/plugins/EnhancerPlugin.java` — remove from array
- Remove `commons-javaflow-1590792.jar` from `framework/lib/`

---

## Phase 4 — Cleanup

### 4A. Remove -javaagent requirement for production mode
**Goal:** JDK 21+, simplification
**Depends on:** All of Phase 3

After removing PropertiesEnhancer, LocalvariablesNamesEnhancer, and
ContinuationEnhancer, the only enhancers left are ControllersEnhancer, SigEnhancer,
MailerEnhancer, and ConstructorEnhancer. If these can run at compile time (via an Ant
task or build plugin) rather than at class-load time, the `-javaagent` flag and
`HotswapAgent` would only be needed in DEV mode for hot reload.

This simplifies deployment and avoids the increasingly restricted
`Instrumentation.redefineClasses()` API on modern JVMs.

---

## Change matrix

| Change | JDK 21+ | Performance | Breaking |
|--------|---------|-------------|----------|
| 0A. Verify tests pass on JDK 11/17 | prereq | prereq | No |
| 0B. Add JDK 21 to CI | ✓ | | No |
| 0C. Audit and expand test coverage | prereq | prereq | No |
| 0D. Establish TechEmpower benchmark baseline | | prereq | No |
| 1A. Remove -noverify | ✓ | | No |
| 1B. Remove SecurityManager | ✓ | | No (unless using java.policy) |
| 1C. Extract constructor enhancer | | prep | No |
| 1D. FieldAccessor method cache | | ✓ | No |
| 1E. Opt-in JPA standard dirty checking | | ✓ | Opt-in — changes save semantics |
| 2A. javax → jakarta | ✓ (prereq) | | **Yes** — all user imports |
| 2B. Hibernate 5 → 6 | ✓ | ✓ | Yes — dialect config, internal API |
| 2C. Remove PropertiesEnhancer | | ✓ | **Yes** — generated accessors gone |
| 2D. Remove LocalvariablesNamesEnhancer | | ✓ | **Yes** — render() API change |
| 3A. Netty 3 → 4 | ✓ | ✓ | No (internal) |
| 3B. Groovy 3 → 4 | ✓ | | No (internal) |
| 3C. Replace JavaFlow continuations | ✓ | ✓ | Partial — continuation await() |
| 4A. Remove -javaagent for prod | ✓ | | No |

## Suggested issue labels

- `jdk21` — JDK 21+ compatibility
- `performance` — Runtime performance improvement
- `breaking` — Requires user code changes
- `phase-1` / `phase-2` / `phase-3` / `phase-4` — Sequencing

## Minimum viable JDK 21 support

If the goal is to get a working build on JDK 21 with minimum effort, the critical
path is: **0A → 0B → 0C → 1A → 1B → 2A → 2B → 3A → 3B → 3C**. Phase 0D (benchmark
baseline) is independent of the critical path but should be done before any phase
claiming a performance benefit (1D, 1E, 2B, 2C, 3A) so there is a number to diff against. The performance
track (1C, 1D, 2C, 2D) can be deferred or done in parallel without blocking JDK 21
compatibility. Phase 0 is non-negotiable — without a reliable test baseline, every
subsequent phase is flying blind.
