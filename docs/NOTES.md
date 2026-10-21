# Notes

Reference material moved out of the README.

## Storage

No database. `files/journal/2026-09-20/` holds the day's photos (`HHmmss_meal.jpg`, about
1600×1200) `activities.txt` (one `HH:mm:ss<TAB>text` line per activity) and `weight.txt` (kilograms). The export zip is
exactly these folders.

## Meal labels

Breakfast / lunch / dinner / snack from the time the photo was taken: 4:00–10:30,
11:30–14:30, 18:00–22:00, snack otherwise. A picture chosen from the gallery (the text
row at the bottom of the viewfinder, system photo picker, no storage permission) is filed
under the time it is added, not the time it was taken, and re-encoded like a shot: upright,
at most 1600 px, no EXIF.

## Provider

A signature-protected provider (`content://com.freedomfighter.readersfoodlog/today`) gives
today's count to apps signed with the same key.
