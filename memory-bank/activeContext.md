# Active Context

## Current status
Working tree was mid-refactor at the start of this session: `GECalcEvaluator`,
`GECalcOfferPriceOverlay`, and `GECalcEvaluatorTest` were **untracked** (new, never
committed), while `GECalcPlugin`, `GECalcKeyHandler`, and `build.gradle` had
uncommitted modifications. Nothing has been committed yet.

`gradlew clean build` passes. **15/15 tests green** in `GECalcEvaluatorTest`.

## Recent work — codebase review (2026-10-05)
Reviewed all 4 main classes, the build, and the README, then implemented fixes.

### High severity — FIXED
**1. Overlay showed the wrong number.** `GECalcOfferPriceOverlay` used
`offer.getPrice()`, which is the price **per item**, not the offer total. A 5000×200gp
offer rendered `200 gp` instead of `1,000,000 gp`. Now uses
`getPrice() * getTotalQuantity()` via `BigInteger` (see `totalPrice()`).
Verified against upstream `GrandExchangeOfferSlot.java`, which computes the total
the same way.

**2. Digit input double-append regression.** `keyPressed` appended *every* digit
unconditionally. The previous implementation deliberately guarded digits behind a
length check, because below 10 chars the client inserts the digit itself and a second
append duplicates it. Restored via `CLIENT_INPUT_LIMIT` + `hasReachedClientInputLimit()`.
Non-standard chars still append unconditionally, which is correct — the client rejects
those natively.
**Still needs in-game confirmation** (can't be verified without launching RuneLite).

### Medium — FIXED
- **`sanitize()` extracted** in `GECalcEvaluator`; `evaluate()` and `isValid()` now
  share one sanitisation path. Previously duplicated verbatim, which risked them
  drifting apart (see the invariant note in systemPatterns.md).
- **Removed dead statement** — `result.setScale(0, RoundingMode.CEILING);` in
  `isValid()` discarded its return value and the result was never used.
- **Recursion depth guard** added (`MAX_NESTING_DEPTH = 64`) — deeply nested parens
  previously threw `StackOverflowError`, an `Error` that escaped both `Exception` handlers.
- **README rewritten** to document `t`, `%`, parentheses, comma separators, clamping,
  the 64-bit mode, and the full-price overlay.
- **Pinned `runeLiteVersion`** from `'latest.release'` to `'1.13.1'` for reproducible builds.
- Added `*.iml` to `.gitignore`.

### Low — FIXED
- Removed unused `Client client` field and `import net.runelite.api.*` wildcard from `GECalcPlugin`.
- Replaced inline `java.awt.Dimension` / `java.awt.Rectangle` with proper imports.
- Added a null-guard on `priceWidget.getFont()`.

### Known inaccuracies found in the old README (FIXED)
- Line 9 claimed `13.8k * 11.3k` = `25100`. Actual result is **155,940,000**.
  Verified by test, not mental math. Now pinned in `testValidateAgreesWithEvaluate`.
- Typos: "Runelite" → "RuneLite", "Grans Exchange" → "Grand Exchange", "add" → "adds".

## A mistake worth remembering
I initially wrote a test asserting `isValid(x) == true` implies `evaluate(x) != ONE`.
It failed on `((((1))))` — which *legitimately* equals 1. The test was wrong, not the
code. Replaced with a table of explicit `(input, expected, isValid)` triples, which is
strictly stronger. **Lesson: don't infer "success" from a sentinel value that is also a
legitimate result.**

## Open items / not done
- **Verify the digit-append fix in-game** (run `gradlew run`, type a multi-digit price
  in the GE field, confirm no duplicated digits).
- **Overlay has no config toggle** — it can't be disabled independently of the plugin.
  Would need a `GECalcConfig implements Config` class. Not done; out of scope so far.
- **`runelite-plugin.properties` is not packaged into the jar.** Verified: both
  `gecalc-1.2.2.jar` and `gecalc-1.2.2-all.jar` contain zero `.properties` entries.
  Impact is limited (the Plugin Hub reads this from the repo, not the jar), but a
  manually-installed external jar won't carry its metadata. Pre-existing, not a regression.
- **No tests for `GECalcKeyHandler` or `GECalcOfferPriceOverlay`** — both need a mocked
  `Client`. The evaluator is the only unit-tested class.
- **Consider renaming `GECalcPluginTest`** — it's a `main()` launcher, not a test.
  Renaming would require updating `pluginMainClass` in `build.gradle`.

## Notes on the overlay text detection
The overlay keys off the literal suffix `" coins"`. If Jagex changes the GE widget text
(e.g. wording or a different abbreviation), the overlay silently stops drawing rather
than crashing — it checks `text.endsWith(COINS_SUFFIX)` before touching anything.
Worth re-checking after any OSRS UI update.