![Reader's Food Log](docs/banner.png)

# Reader's Food Log

One touch photographs what you are about to eat; the app labels it breakfast, lunch, dinner
or snack and files it by day. No calories, no database, no typing. Activity in one tap,
weight once a day with its curve. No network permission; the journal exports as a zip.

## Key points

- Taking a photo: one touch on the widget, one touch anywhere on the viewfinder (or a volume
  key), and the screen closes by itself.
- Ways in: a 1×1 camera widget, a 4×1 widget (today's count, pencil, camera), a quick-settings
  tile that works over the lock screen, launcher shortcuts, and the food log tile of
  [Reader's Launcher](https://github.com/funkypitt/readers-launcher), a shortcut to the viewfinder.
- Home is one folder per day; open a day for its photos in the order of the clock. Touch a
  photo to change its label, share it or delete it.
- Physical activity in one tap: strength training, intense activity, endurance, or a free line.
- Weight, once a day: one tap opens the number pad; a curve shows 30 days, 90 days, a year or
  everything. Stored in kilograms, shown in kg or lb.
- Local only: camera permission, no network permission, no account, Android backup off.
- Export the journal as one zip and import it on another phone; importing merges and replaces nothing.
- Black and white, text only, six languages (en, fr, de, es, pt, ru).

More detail: [docs/NOTES.md](docs/NOTES.md).

## Install


[<img src="docs/badge_obtainium.png" alt="Get it on Obtainium" height="48">](https://gallaz.ch/eink/#readers-foodlog)

- **F-Droid** (recommended, updates arrive by themselves): add the repository from [gallaz.ch/eink](https://gallaz.ch/eink/#fdroid), or the address `https://funkypitt.github.io/fdroid-repo/repo` in F-Droid.
- **Obtainium**: tap the badge on the phone, or add `https://github.com/funkypitt/readers-foodlog` in Obtainium.
- **APK**: attached to the [latest release](../../releases/latest). No automatic updates.

All three deliver the same file, with the same signature.

## Build

    export JAVA_HOME=/path/to/jdk-21
    ./gradlew assembleDebug

Strings for the six languages are generated from the table in `tools/strings.py`.

MIT licence.
