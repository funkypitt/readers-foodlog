# Reader's Food Log

A food journal made of photos only, for Android. Photographing what you are about to eat
should be as thoughtless as reaching into a bowl of peanuts: one touch on the widget, one
touch anywhere on the viewfinder, and the screen closes by itself.

- **Photos sorted by day and time**, labelled breakfast / lunch / dinner / snack from the
  time they were taken (4:00–10:30, 11:30–14:30, 18:00–22:00, snack otherwise). Touch a photo
  to change its label, share it or delete it.
- **Home = one folder per day.** Open a day to see its photos in the order of the clock.
- **Physical activity in one tap**: strength training, intense activity, endurance, or a free line.
- **Ways in**: a 1×1 camera widget, a 4×1 widget (today's count, pencil, camera), a
  quick-settings tile (works over the lock screen), launcher shortcuts, volume keys as shutter.
- **Purely local**: no network permission, no account, Android backup off. Export the journal
  as one zip and import it on another phone; importing merges and replaces nothing.
- Black and white, text only, English + fr/de/es/pt/ru, like the other Reader's apps.

## Storage

No database. `files/journal/2026-09-20/` holds the day's photos (`HHmmss_meal.jpg`, about
1600×1200) and `activities.txt` (one `HH:mm:ss<TAB>text` line per activity). The export zip is
exactly these folders.

## Build

    export JAVA_HOME=/path/to/jdk-21
    ./gradlew assembleDebug

Strings for the six languages are generated from the table in `tools/strings.py`.

MIT licence.
