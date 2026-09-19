![Reader's Food Log](docs/banner.png)

# Reader's Food Log

A food journal made of photos only, for Android. Photographing what you are about to eat
should be as thoughtless as reaching into a bowl of peanuts: one touch on the widget, one
touch anywhere on the viewfinder, and the screen closes by itself.

- **Photos sorted by day and time**, labelled breakfast / lunch / dinner / snack from the
  time they were taken (4:00–10:30, 11:30–14:30, 18:00–22:00, snack otherwise). Touch a photo
  to change its label, share it or delete it.
- **Home = one folder per day.** Open a day to see its photos in the order of the clock.
- **Physical activity in one tap**: strength training, intense activity, endurance, or a free line.
- **Weight, once a day**: one tap opens the number pad; a curve shows the last 30 days, 90 days,
  year or everything. Stored in kilograms, shown in kg or lb.
- **Ways in**: a 1×1 camera widget, a 4×1 widget (today's count, pencil, camera), a
  quick-settings tile (works over the lock screen), launcher shortcuts, volume keys as shutter.
- **Reader's Launcher tile** (launcher 1.17.0+): today's count with ✎ activity, ⚖ weight and ◉ camera,
  read through a signature-protected provider (`content://com.freedomfighter.readersfoodlog/today`).
- **Purely local**: no network permission, no account, Android backup off. Export the journal
  as one zip and import it on another phone; importing merges and replaces nothing.
- Black and white, text only, English + fr/de/es/pt/ru, like the other Reader's apps.

## Storage

No database. `files/journal/2026-09-20/` holds the day's photos (`HHmmss_meal.jpg`, about
1600×1200) `activities.txt` (one `HH:mm:ss<TAB>text` line per activity) and `weight.txt` (kilograms). The export zip is
exactly these folders.

## Build

    export JAVA_HOME=/path/to/jdk-21
    ./gradlew assembleDebug

Strings for the six languages are generated from the table in `tools/strings.py`.

MIT licence.

## Install

Signed APK in the [releases](https://github.com/funkypitt/readers-foodlog/releases/latest), or add the
F-Droid repository `https://funkypitt.github.io/fdroid-repo/repo`.
