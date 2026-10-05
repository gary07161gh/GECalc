# GE Calc

------
| This is a personal project, and probably won't receive any major feature updates.
| --- |

A [RuneLite](https://github.com/runelite/runelite) plugin that adds the ability to use maths to set price and quantity in the Grand Exchange window, and enables the entry of decimal values when using the k, m, b, and t unit identifiers. See usage below.

As of version 1.2 the plugin supports complex expressions, such as '13.8k * 11.3k' for a result of 155,940,000.

Usage
------
The plugin enables the use of expressions to set the price and/or quantity using the Grand Exchange.

| Input  | Result |
| ------------- | ------------- |
| 45 * 4  | 180  |
| 180 / 4  | 45  |
| 100 + 100  | 200  |
| 100 - 50  | 50  |

Expressions follow standard operator precedence, so `2 + 3 * 4` is 14, not 20. Parentheses can be used to group terms:

| Input  | Result |
| ------------- | ------------- |
| (2 + 3) * 4  | 20  |
| 13.8k * 11.3k  | 155,940,000  |

This plugin also allows entering of decimal values followed by a unit:

| Input  | Result |
| ------------- | ------------- |
| 5.63k  | 5,630  |
| 8.5m  | 8,500,000  |
| 1.024b  | 1,024,000,000  |
| 2.5t  | 2,500,000,000,000  |

Units are case-insensitive, and thousands separators are accepted, so `1,024b` also works.

Percentages
------
A `%` suffix applies to the value on its left when it follows `+` or `-`, which is handy for tax and fees:

| Input  | Result |
| ------------- | ------------- |
| 10m - 1%  | 9,900,000  |
| 100k + 10%  | 110,000  |

When used with `*` or `/`, a `%` is treated as an ordinary fraction instead: `500k * 2%` is 10,000.

Results are rounded up to the nearest whole number, since the GE input does not accept decimals.

Limits
------
Values are clamped to the maximum the current Grand Exchange input mode allows, and anything below 1 is raised to 1. If an expression cannot be parsed it is left in the input field untouched rather than being replaced with a wrong number.

The Grand Exchange quantity and price input windows contain an asterisk '*' at the end of the text field. This is only visual and does not impact the result.

Full price overlay
------
When the Grand Exchange window is open, the plugin replaces the abbreviated price on each offer slot (for example `1.5m coins`) with the full amount (`1,500,000 gp`), so the exact total is visible without opening each offer.

![GE Dialog](assets/panel.png "GE Dialog")

![Value Entry](assets/entry.png "Value Entry")

Bugs & Requests
-------
If you do run into any bugs please [create an issue](https://github.com/cman8396/GECalc/issues/new). I am an "anything but Java" developer so be patient.

License
-------
GE Calc is licensed under the BSD 2-Clause License. See LICENSE for info.

Author
------
cman8396, LargeChongus
