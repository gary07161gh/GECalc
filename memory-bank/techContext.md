# Tech Context

## Stack
- **Language:** Java, `options.release = 11`
- **Build:** Gradle 8.5 (wrapper), single-module Groovy DSL `build.gradle`
- **Platform:** RuneLite external plugin (OSRS), `net.runelite:client`
- **DI:** Guice via `javax.inject.Inject` (`@Inject` fields, **not** constructors)
- **Logging:** Lombok `@Slf4j`
- **Tests:** JUnit 4.13.2

## Pinned dependency
```groovy
def runeLiteVersion = '1.13.1'
```
Originally `latest.release`. It was **pinned** because a moving version makes builds
non-reproducible. Bump it deliberately, not automatically.

## Local JDK
- Java 17 is installed and used to run Gradle.
- Output bytecode is **major version 55 (Java 11)** because `options.release.set(11)`.
- RuneLite `runelite-api` 1.13.1 classes are also major 55, so Java 11 target is correct.
- The earlier commit "upgrade Gradle wrapper to 8.5 for Java 17" refers to the
  *toolchain* JDK, not the bytecode target. Don't "fix" `release` to 17 without reason.

## Key tasks
| Task | Purpose |
| --- | --- |
| `gradlew test` | Run unit tests |
| `gradlew run` | Launch RuneLite with the plugin loaded in dev mode |
| `gradlew testPlugin` | Alias for `run` |
| `run.bat` | Windows wrapper → `gradlew.bat testPlugin` |
| `gradlew shadowJar` | Build the distributable `-all.jar` |

`run` uses `sourceSets.test.runtimeClasspath` because the launcher class
`com.gecalc.GECalcPluginTest` lives in the **test** source set. `enableAssertions = true`
is required — `ExternalPluginManager.loadBuiltin` throws at runtime without `-ea`.

## RuneLite API gotchas (verified against 1.13.1)
- `Widget.getFont()` returns **`net.runelite.api.FontTypeFace`**, NOT
  `net.runelite.api.Font` or `net.runelite.api.widgets.Font`. Neither of those classes
  exists in this API version. Verified by reading the class file constant pool.
- `Widget` lives in `net.runelite.api.widgets`, `Client` in `net.runelite.api`.
- `GrandExchangeOffer.getPrice()` = **price per item**; `getSpent()` = total spent;
  `getTotalQuantity()` = total quantity. See [[GECalcOfferPriceOverlay]] in
  activeContext.md for why this matters.

## Build warnings that are safe to ignore
- CVE warnings from transitive `testImplementation` RuneLite deps (logback, gson,
  commons-text). They come from RuneLite itself and aren't actionable here.
- `startUp`/`shutDown throws Exception` "never thrown" warnings are false positives —
  the signatures are fixed by the `Plugin` base class.