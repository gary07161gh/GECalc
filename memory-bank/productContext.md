# Product Context

## User problem
A player is on the Grand Exchange and needs to set an exact price or quantity.
The GE input accepts plain integers only. Any arithmetic — "what is 13,800 × 11,300?"
or "10m minus 1% tax?" — has to be done in a calculator app first, then typed back
in. GE Calc collapses that into one step.

## Primary user journey
1. Player opens the Grand Exchange and clicks **Set quantity** or **Set price**.
2. Types an expression, e.g. `13.8k * 11.3k`.
3. Presses Enter.
4. The field now contains `155940000`.

Supporting journey: open the GE overview and read the **exact** gp value of each offer
instead of a rounded abbreviation.

## UX goals
- **Zero learning curve.** The syntax is arithmetic people already know. No mode switch,
  no prefix, no special entry box.
- **Never silently corrupt input.** If an expression can't be parsed, the field is left
  untouched (this is why `isValid()` gates `evaluate()` in the key handler). Losing
  what you typed is the worst possible failure — worse than doing nothing.
- **Never round in the wrong direction.** Results use `RoundingMode.CEILING` so a
  computed price is never a hair under what the player intended.
- **Exactness where it matters.** The overlay exists because an abbreviated `1.5m`
  hides whether you're off by a rounding error.

## Personality
Small, focused, no-nonsense utility. The README explicitly says it's a personal project
unlikely to get major feature updates — favour correctness fixes and small polish over
speculative features.

## What users would consider a bug
- A duplicated digit while typing (see the CLIENT_INPUT_LIMIT invariant).
- The field being cleared or overwritten with `1` after a typo.
- An offer showing the unit price where the total should be.
- Prices rounding down.