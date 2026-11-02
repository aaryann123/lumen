# Architecture

Lumen is two `AppWidgetProvider`s, a Compose reader, and a list. Every widget is a bitmap Lumen draws itself,
placed in an `ImageView` through `RemoteViews`.

```
          Quotes.kt  QUOTES, pool(), deal(), fitting()
              │
   Lumen (object in QuoteWidget.kt)   prefs "lumen": current, bag, show
     │                 │
MainActivity      QuoteWidget ── draw() ─┐
 (Compose)        ClockWidget ── draw() ─┴─ Slot.kt: ink(), shadeOf(), inSlot()  ->  Bitmap  ->  RemoteViews
```

## Files

| File | Responsibility |
|---|---|
| `Quotes.kt` | `Quote(line, who, film, villain)`, the list, filter pool, shuffle bag, short-slot fallback |
| `QuoteWidget.kt` | `Lumen` state object, the quote widget provider, the script + caps typesetting |
| `ClockWidget.kt` | Word clock lines, the provider, the minute alarm |
| `Slot.kt` | `inSlot` sizes art to the slot, `ink` is the white text paint, `shadeOf` the dark cloud under it |
| `MainActivity.kt` | Full-screen reader with All / Heroes / Villains |

## Why bitmaps

`RemoteViews` can't load custom fonts into a `TextView`, can't blur a text shadow wide enough, and Samsung's lock
screen restyles text widgets with its own font and size. Drawing to a bitmap gives the exact type, the cloud
behind it, and the same result on every host.

## One size, any slot

`inSlot` reads the slot's size from `getAppWidgetOptions` and draws the art into a bitmap exactly that size, so the
host shows it 1:1. The art is never scaled up; it shrinks only when the slot is smaller, and the text still starts
`PAD` (20 dp) from the slot's left edge, so the clock and quote line up when stacked. The clock sits at the
bottom of its slot so it lands right above the quote.

## The dark cloud

`shadeOf` copies the text paint, strokes it 6 dp thick and blurs it 12 dp, at 55% black. Drawn first, it hugs the
letterforms and spreads about 15 dp, inside `PAD`, so the bitmap edge never cuts it. On dark wallpapers it
disappears; on light ones it is what makes the white readable.

## Rotation

`Lumen.advance` deals the next index from a shuffle bag stored as a comma-separated string. When the bag empties,
the pool is shuffled again and, if the first pick equals the last shown, rotated so the same line never repeats
across rounds. `pool` falls back to everything when a filter would be empty.

## The clock tick

An exact `RTC` alarm (not `RTC_WAKEUP`) fires at the next minute. While the screen is off it waits, then fires as
soon as the phone wakes, so the lock screen is right when you look and the phone is never woken for it.
`MY_PACKAGE_REPLACED` restarts the chain because an app update cancels alarms.

## Things that bit us

- **Light lock wallpapers ate the text.** Thin white strokes vanished; the clock moved from Light to Regular
  weight and every widget got the dark cloud.
- **A rectangle scrim read as a grey box.** Thickened, blurred text as the shadow fixed it.
- **The lock screen cut long lines mid-sentence.** Samsung forces about two lines there, so one-row slots use
  `fitting` to swap in a line of 90 characters or fewer.
- **One- and two-letter openers looked lost in script.** "I" and "A" take the next word along.
- **The clock went minutes stale while the app was frozen.** Samsung holds non-waking alarms for frozen apps;
  the companion launcher [Fluid](https://github.com/aaryann123/fluid-launcher) broadcasts `app.lumen.TICK` on
  screen-on and `app.lumen.NEXT` on screen-off, so the quote also turns over like a rotating lock wallpaper.
- **Script and caps needed to tuck.** The script's tail dips 9 dp into the caps line, like the reference layout.
