# GE Calc — Project Brief

## What it is
A RuneLite plugin for Old School RuneScape that lets players type **maths expressions**
(not just plain numbers) into the Grand Exchange price and quantity fields.

## Purpose / problem solved
The GE input boxes only accept plain integers. A player who wants to buy 13.8k items at
11.3k each has no way to compute that total. GE Calc lets them type `13.8k * 11.3k` and
presses Enter to have the GE field populated with the result.

## Core features
1. **Expression evaluation** — `+ - * /`, parentheses, standard operator precedence.
2. **Unit suffixes** — `k`, `m`, `b`, `t` (case-insensitive), including decimals
   (`5.63k` → 5,630). Thousands separators like `1,000,000` are accepted.
3. **Percentages** — `10m - 1%` applies 1% to the left operand (GE tax). With `*` or
   `/`, `%` behaves as an ordinary fraction (`500k * 2%` → 10,000).
4. **Clamping** — result is clamped to the maximum of the active GE input mode and
   floored at 1.
5. **Full price overlay** — replaces the abbreviated GE slot price (`1.5m coins`)
   with the exact total (`1,500,000 gp`).

## Scope boundaries
- Single-purpose utility plugin. The README states it is a personal project and
  unlikely to receive major feature updates — respect this; don't propose large
  feature additions unprompted.
- Not affiliated with Jagex.

## Authors / provenance
Original author: **cman8396**, with **LargeChongus**. Fork maintained at
github.com/gary07161gh/GECalc. Licensed BSD 2-Clause.