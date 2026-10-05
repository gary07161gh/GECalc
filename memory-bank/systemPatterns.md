# System Patterns & Architecture

## Class map
| Class | Source set | Role |
| --- | --- | --- |
| `GECalcPlugin` | main | RuneLite entry point. Wires up the key handler + overlay. |
| `GECalcKeyHandler` | main | `KeyListener`. Intercepts typing in GE quantity/price fields. |
| `GECalcEvaluator` | main | **Pure static** expression parser. No RuneLite imports. |
| `GECalcOfferPriceOverlay` | main | Draws full gp totals over the GE overview. |
| `GECalcPluginTest` | **test** | Not a test — a `main()` launcher for dev mode. |

## The central design rule
**`GECalcEvaluator` must never import anything from `net.runelite`.**

It is a pure function over strings with no game state, which is what makes it
trivially unit-testable (15 tests, no RuneLite mocks). Keep it that way. Any
game-state dependency belongs in the key handler or the overlay.

## Parser architecture (`GECalcEvaluator`)
Recursive-descent parser, textbook structure:
```
parse()          → guards against trailing tokens
  parseExpression()   handles +/- AND percent-of-left-operand
    parseTerm()       handles * /
      parseFactor()   handles unary +/-
        parsePrimary() handles numbers, %, and ( )
```
- `Token` / `TokenType` are private nested types.
- **All arithmetic uses `BigDecimal`**, never `double`. The original pre-refactor
  parser used `double` and lost precision on large GE values.
- Division always specifies a scale and `RoundingMode.HALF_UP` (20 decimal places)
  to avoid `ArithmeticException` on non-terminating decimals.
- Final result is `setScale(0, RoundingMode.CEILING)` — GE rejects decimals, so
  always round **up** (never lose value to truncation).
- `evaluate()` returns `BigDecimal.ONE` on any error rather than throwing.

## Percent semantics (deliberately asymmetric)
- After `+`/`-`: `N%` means *N percent of the left operand* → `10m - 1%` = 9,900,000.
- Elsewhere (via `parsePrimary`): `N%` is an ordinary fraction → `500k * 2%` = 10,000.
This is intentional and matches how players think about GE tax. It is covered by
`testPercentages` — don't "simplify" it into a single rule without changing tests.

## Input pipeline
```
keystroke → GECalcKeyHandler.keyPressed
             ├─ calc char (+ - * / ( ) k m b ...) → append (client rejects these itself)
             ├─ digit → append ONLY past CLIENT_INPUT_LIMIT (see below)
             └─ Enter  → parseQuantity()
                            ├─ GECalcEvaluator.isValid()  → false = leave field untouched
                            └─ GECalcEvaluator.evaluate() → write via clientThread.invoke()
```
`clientThread.invoke(...)` is mandatory — varc writes must happen on the client thread.

### The digit-append rule (easy to regress)
Digits and non-standard characters are handled **asymmetrically on purpose**:
- Non-standard chars: the GE box rejects them natively, so the plugin is the only
  thing that can insert them → always append.
- Digits: below `CLIENT_INPUT_LIMIT` (10) the **client inserts the digit itself**.
  Appending too would duplicate it. Only override past the limit.

The original author documented this explicitly:
> "if we don't [check length] and the length is less than 10 it adds the value twice"

A refactor briefly regressed this into "append every digit". `keyPressed` must keep
the `hasReachedClientInputLimit()` guard.

## `sanitize()` must stay shared
`evaluate()` and `isValid()` both call the private `sanitize()`. This is a
**correctness invariant**, not just DRY: if they diverged, an expression could pass
`isValid` (so the plugin would overwrite the field) and then fail in `evaluate`
(silently writing `1`). `testValidateAgreesWithEvaluate` guards this.

## Recursion depth guard
`Parser.MAX_NESTING_DEPTH = 64`. `parsePrimary` recurses per `(`. Deeply nested input
(`"(".repeat(5000)`) would throw `StackOverflowError`, which is an `Error` and escapes
the `catch (Exception)` handlers. The depth counter + try/finally prevents this.
Covered by `testDeeplyNestedParenthesesDoNotOverflow`.

## GE input modes
`VarClientInt.INPUT_TYPE`: `7` = legacy, `30` = 64-bit GE.
`getMaximumValue()` picks the clamp ceiling accordingly —
`LEGACY_MAX` = `Integer.MAX_VALUE` (2,147,483,647),
`GE_MAX` = 2,149,631,130,647.

## `GECalcOfferPriceOverlay`
- Locates the GE via `InterfaceID.GeOffers.UNIVERSE` / `.CONTENTS`, iterates
  `SLOT_COUNT = 8` slots, recurses with `findCoinsWidget()` to find the child widget
  whose text ends in `" coins"`.
- **`totalPrice()` = `getPrice() * getTotalQuantity()`, computed via `BigInteger`.**
  `getPrice()` is the *per-item* price (RuneLite labels it "Price each"), so using it
  alone showed the unit price instead of the total. Verified against
  `GrandExchangeOfferSlot`, which uses the same product as the offer total.
  `BigInteger` guards against `long * int` overflow on high-value offers.
- `DYNAMIC` position + `ABOVE_WIDGETS` layer; returns `null` (draw-only, no bounds).

## DI note
`GECalcOfferPriceOverlay` uses **constructor injection**; `GECalcPlugin` and
`GECalcKeyHandler` use **field injection**. Both work under Guice; this is just
inconsistent, not broken.