# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Play Framework 1.x — a Java web framework emphasizing developer productivity and RESTful architectures. Version 1.8.0, targeting Java 11+. The framework provides hot code reloading, bytecode enhancement, and convention-over-configuration patterns.

## Build Commands

All build commands use Ant and should be run from `framework/`:

```bash
# Build the framework JAR (includes clean, compile, modules)
ant jar

# Compile only (no clean)
ant compile

# Run all tests (unit tests + sample app integration tests)
ant test

# Run only unit tests (framework/test-src/)
ant unittest

# Run a single test class
ant test-single -Dtestclass=play.utils.SomeTest

# Clean build artifacts
ant clean

# Generate Javadoc
ant javadoc
```

## Play CLI

The `play` script at the repository root is a Python 3 CLI. Common commands:

```bash
play new <app-path>       # Create new application
play run <app-path>       # Run app (dev mode, port 9000)
play test <app-path>      # Run app in test mode
play auto-test <app-path> # Run app tests headless
play deps <app-path>      # Resolve dependencies
play precompile <app-path>
```

## Repository Structure

- `framework/` — Core framework
  - `src/play/` — Main Java source (~313 files)
  - `test-src/` — Unit tests (run via `ant unittest`)
  - `tests/src/` — Integration test helpers (run via `ant compile-tests`)
  - `lib/` — Runtime dependencies (JARs)
  - `lib-test/` — Test-only dependencies
  - `build.xml` — Ant build file
  - `dependencies.yml` — Ivy dependency declarations
  - `pym/play/` — Python CLI modules and commands
- `modules/` — Built-in modules (testrunner, grizzly, docviewer, crud, secure)
- `samples-and-tests/` — Sample apps used for integration testing (yabe, forum, zencontact, etc.)

## Architecture

### MVC Pattern

- **Controllers**: Extend `play.mvc.Controller`. Actions are public static methods. HTTP context (request, response, session, flash) accessed via ThreadLocal statics. Render methods (`render()`, `renderJSON()`, `renderXML()`, etc.) throw result exceptions to short-circuit execution.
- **Models**: Extend `play.db.jpa.Model` for JPA entities with auto-generated `Long id`. Bytecode-enhanced at load time to add finder methods. `GenericModel` provides the query API.
- **Views**: Groovy templates in `app/views/`. `FastTags` for custom template tags, `JavaExtensions` for template helpers.
- **Router**: `play.mvc.Router` parses `conf/routes` files, maps HTTP methods + URL patterns to controller actions.

### Plugin System

Plugins extend `play.PlayPlugin` and are registered in `src/play.plugins` with numeric priority (lower = higher priority). Key plugins in load order:

- **0 EnhancerPlugin** — Bytecode enhancement for controllers, models
- **300 DBPlugin** — Database connection management
- **400 JPAPlugin** — JPA/Hibernate lifecycle
- **450 Evolutions** — Database migration tracking
- **500 MessagesPlugin** — i18n message loading
- **700 JobsPlugin** — Background job scheduling (@Every, @On, @OnApplicationStart)

`PluginCollection` manages lifecycle. Plugins hook into compilation, request handling, binding, routing, and template events.

### Class Loading & Hot Reload

`play.classloading.ApplicationClassloader` provides custom class loading. `HotswapAgent` is a Java agent (specified as Premain-Class in the JAR manifest) that enables bytecode hotswapping in dev mode. Enhancers in `play.classloading.enhancers/` transform classes at load time:

- `ControllersEnhancer` — Injects HTTP context thread-locals
- `JPAEnhancer` — Adds JPA model helper methods
- `ContinuationEnhancer` — Async continuation support
- `SigEnhancer` — Change detection signatures

### Invocation Model

`play.Invoker` manages a thread pool for request handling and background jobs. `Play.Mode` (DEV/PROD) controls behavior — DEV enables hot reload, error pages with source context, and the documentation module.

### Key Subsystems

- **Data binding**: `play.data.binding` — HTTP params to Java types, with custom `TypeBinder` implementations
- **Validation**: `play.data.validation` — Annotation-based (`@Required`, `@Email`, `@Min`, etc.) backed by OVal
- **Caching**: `play.cache.Cache` — Pluggable (EhCache, Memcached) with TTL strings ("10s", "3mn", "8h")
- **Jobs**: `play.jobs.Job<V>` — Annotated with `@Every("1h")`, `@On("0 0 * * *")` (cron)
- **WS**: `play.libs.WS` — HTTP client with OAuth/OAuth2 support
- **DB**: HikariCP or C3P0 connection pooling, H2/MySQL/PostgreSQL drivers included

## Testing Structure

- **Unit tests** (`framework/test-src/`): JUnit tests for framework internals. Run with `ant unittest`. Test classes must end in `*Test.java`.
- **Integration tests** (`samples-and-tests/`): Full Play apps tested via `play auto-test`. Python scripts (`tests.py`) orchestrate lifecycle testing.
- **App tests** use `play.test.FunctionalTest` for HTTP-level testing and `play.test.Fixtures` for YAML test data loading.
