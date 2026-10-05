# Progress

## Status: working, 15/15 tests green
`gradlew clean build` → **BUILD SUCCESSFUL**. Zero IDE lint warnings in all files
touched this session.

## What works
- **Expression evaluation** — `+ - * /`, parentheses, unary +/-, correct precedence,
  all `BigDecimal` arithmetic. 15 unit tests.
- **Units** — `k`/`m`/`b`/`t`, case-insensitive, decimals (`5.63k`), comma separators.
- **Percentages** — percent-of-left-operand after `+`/`-`; fraction elsewhere.
- **Clamping** — per input mode (`LEGACY_MAX` / `GE_MAX`), floor of 1, ceiling rounding.
- **Key interception** — calc chars appended; digits appended only past the client limit.
- **Full-price overlay** — shows `price × quantity` in gp on each GE slot.
- **Dev workflow** — `run.bat` / `gradlew run` launches RuneLite with the plugin.

## Test coverage (15 tests, `GECalcEvaluatorTest`)
`testPlainNumbers`, `testUnits`, `testBasicArithmetic`, `testOperatorPrecedence`,
`testChainedExpressions`, `testParentheses`, `testPercentages`,
`testDivisionRoundingCeiling`, `testCommasAndFormatting`, `testExpressionValidation`,
`testClamping`, `testErrorHandling`, `testValidateAgreesWithEvaluate`,
`testValidationEdgeCases`, `testDeeplyNestedParenthesesDoNotOverflow`.

**Not covered:** `GECalcKeyHandler`, `GECalcOfferPriceOverlay`, `GECalcPlugin` — all
require a mocked RuneLite `Client`. This is the main testing gap.

## What's left
1. **In-game verification of the digit-append fix** (the one unverified change).
2. **Commit the work** — the three new files are still untracked.
3. Optional: config toggle for the overlay; rename `GECalcPluginTest`; package
   `runelite-plugin.properties` into the jar; add mocked tests for handler/overlay.

## Version
`1.2.2` (`build.gradle` and `runelite-plugin.properties`). The `1.2.x` line covers the
`GECalcEvaluator` rewrite and the overlay.

## Decision log
| Decision | Rationale |
| --- | --- |
| Pin RuneLite to `1.13.1` | `latest.release` made builds non-reproducible. |
| Keep `options.release = 11` | RuneLite API 1.13.1 is bytecode major 55. Java 17 is only the *toolchain*. |
| Fix overlay with `BigInteger` | `long × int` can overflow on high-value offers. |
| Keep `MAX_NESTING_DEPTH` guard | `StackOverflowError` is an `Error`; `catch (Exception)` won't catch it. |
| Leave `.iml` out of git | Added to `.gitignore`. |
| Don't add features unprompted | README declares this a personal, low-maintenance project. |