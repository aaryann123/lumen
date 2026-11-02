# Building

| | |
|---|---|
| JDK | 21 (`brew install openjdk@21`, or Android Studio's bundled JDK) |
| Android SDK | platform 37, build-tools, platform-tools |
| Gradle | wrapper included (`./gradlew`) |
| Node | optional, only for the `npm run` shortcuts |

## First run

```bash
echo "sdk.dir=/path/to/android/sdk" > local.properties   # gitignored
export JAVA_HOME=/path/to/jdk-21                            # the npm scripts default to Homebrew's openjdk@21
npm run dev        # ./gradlew :app:installRelease (every connected device)
npm run apk        # writes Lumen.apk (gitignored)
npm test           # ./gradlew :app:testDebugUnitTest
```

The release build is minified, shrinks resources, and is signed with your debug key.

## Layout

```
app/build.gradle.kts             SDK levels, Compose, release signing
app/src/main/AndroidManifest.xml  reader activity, two widget receivers, exact-alarm permission
app/src/main/java/app/lumen/     all Kotlin (see ARCHITECTURE.md)
app/src/main/res/xml/            widget provider info (sizes, 30-minute update, keyguard category for the clock)
app/src/main/res/layout/         one ImageView per widget, plus the clock's picker preview
app/src/main/res/font/           Montserrat, Great Vibes (OFL)
app/src/test/                    QuotesTest, ClockTest
docs/top-200-candidates.md       lines not yet in the list
Tools/screenshots.sh             regenerate docs/screenshots on an emulator
```

## Adding a line

Add a row to `QUOTES` in `Quotes.kt`:

```kotlin
h("All we have to decide is what to do with the time that is given to us.", "Gandalf", "The Fellowship of the Ring"),
v("You come at the king, you best not miss.", "Omar Little", "The Wire"),
```

`h` is a hero, `v` a villain. Run `npm test`; `shortSlotsGetShortLines` checks the short-slot fallback still works.

## Changing the look

Type sizes live in each widget's `draw()`; the shared margin is `PAD` in `Slot.kt`; the cloud's thickness, blur and
opacity are in `shadeOf`. Text is always white.

## Regenerating screenshots

`sh Tools/screenshots.sh` boots a headless, read-only Pixel 9 Pro Fold AVD named `fold`, installs the release APK,
and captures the reader into `docs/screenshots/`. `widgets.jpg` came from the Fluid launcher's widget page on the
same emulator. Never use a personal phone for these.

## Not supported yet

- Stock Android's lock-screen widget hub is untested; on most phones the widgets are home-screen only.
- No user-added quotes; the list is compiled in.
