package com.freedomfighter.readersfoodlog.data

import android.content.Context
import com.freedomfighter.readersfoodlog.widget.FoodWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Meal(val key: String) {
    BREAKFAST("breakfast"), LUNCH("lunch"), DINNER("dinner"), SNACK("snack");

    companion object {
        /** The label a photo gets from the time it was taken. */
        fun at(t: LocalTime): Meal {
            val m = t.hour * 60 + t.minute
            return when (m) {
                in 4 * 60 until 10 * 60 + 30 -> BREAKFAST
                in 11 * 60 + 30 until 14 * 60 + 30 -> LUNCH
                in 18 * 60 until 22 * 60 -> DINNER
                else -> SNACK
            }
        }
        fun ofKey(k: String): Meal = entries.firstOrNull { it.key == k } ?: SNACK
    }
}

sealed class Entry { abstract val time: LocalTime }
data class PhotoEntry(override val time: LocalTime, val meal: Meal, val file: File) : Entry()
/** [line] is the entry's position in the day's activities file. */
data class ActivityEntry(override val time: LocalTime, val text: String, val line: Int) : Entry()

/** [weight] is in kilograms, whatever unit the screens show. */
data class Day(val date: LocalDate, val photos: Int, val activities: Int, val weight: Double? = null)

/**
 * The whole journal is plain folders: journal/2026-09-20/ holds the day's photos
 * (HHmmss_meal.jpg), activities.txt (one "HH:mm:ss<TAB>text" line per activity) and
 * weight.txt (the day's weight in kilograms).
 */
object Journal {
    private val DAY = DateTimeFormatter.ISO_LOCAL_DATE
    private val STAMP = DateTimeFormatter.ofPattern("HHmmss")
    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val ACTIVITIES = "activities.txt"
    private const val WEIGHT = "weight.txt"

    private val _version = MutableStateFlow(0)
    /** Bumped after every change, so the screens list the folders again. */
    val version: StateFlow<Int> = _version

    fun changed(context: Context) {
        _version.value++
        FoodWidgets.refresh(context)
        context.contentResolver.notifyChange(com.freedomfighter.readersfoodlog.provider.TodayProvider.URI, null)
    }

    private fun root(context: Context) = File(context.filesDir, "journal")
    private fun dir(context: Context, date: LocalDate) = File(root(context), DAY.format(date))

    fun days(context: Context): List<Day> =
        (root(context).listFiles() ?: emptyArray())
            .mapNotNull { d -> runCatching { LocalDate.parse(d.name, DAY) }.getOrNull() }
            .map { date -> entries(context, date).let { e -> Day(date, e.count { it is PhotoEntry }, e.count { it is ActivityEntry }, weight(context, date)) } }
            .filter { it.photos + it.activities > 0 || it.weight != null }
            .sortedByDescending { it.date }

    fun entries(context: Context, date: LocalDate): List<Entry> {
        val d = dir(context, date)
        val photos = (d.listFiles() ?: emptyArray()).mapNotNull { f ->
            if (f.extension != "jpg" || f.length() == 0L) return@mapNotNull null
            val parts = f.nameWithoutExtension.split('_')
            val t = runCatching { LocalTime.parse(parts[0], STAMP) }.getOrNull() ?: return@mapNotNull null
            PhotoEntry(t, Meal.ofKey(parts.getOrElse(1) { "" }), f)
        }
        return (photos + activities(d)).sortedBy { it.time }
    }

    private fun activities(d: File): List<ActivityEntry> {
        val f = File(d, ACTIVITIES)
        if (!f.exists()) return emptyList()
        return f.readLines().mapIndexedNotNull { i, l ->
            val tab = l.indexOf('\t')
            if (tab < 0) return@mapIndexedNotNull null
            val t = runCatching { LocalTime.parse(l.substring(0, tab), CLOCK) }.getOrNull() ?: return@mapIndexedNotNull null
            ActivityEntry(t, l.substring(tab + 1), i)
        }
    }

    fun weight(context: Context, date: LocalDate): Double? =
        File(dir(context, date), WEIGHT).takeIf { it.exists() }?.readText()?.trim()?.toDoubleOrNull()

    /** One weight per day, in kilograms; null removes it. */
    fun setWeight(context: Context, date: LocalDate, kg: Double?) {
        val d = dir(context, date)
        if (kg == null) { File(d, WEIGHT).delete(); tidy(d) }
        else { d.mkdirs(); File(d, WEIGHT).writeText(String.format(java.util.Locale.ROOT, "%.2f\n", kg)) }
        changed(context)
    }

    /** Every weight entered, oldest first. */
    fun weights(context: Context): List<Pair<LocalDate, Double>> =
        (root(context).listFiles() ?: emptyArray())
            .mapNotNull { d -> runCatching { LocalDate.parse(d.name, DAY) }.getOrNull() }
            .mapNotNull { date -> weight(context, date)?.let { date to it } }
            .sortedBy { it.first }

    /** Where the camera writes the shot taken at [now]; call [changed] once it is saved. */
    fun newPhotoFile(context: Context, now: LocalDateTime): File {
        val d = dir(context, now.toLocalDate()).apply { mkdirs() }
        val meal = Meal.at(now.toLocalTime())
        var t = now.toLocalTime().withNano(0)
        var f: File
        do { f = File(d, "${STAMP.format(t)}_${meal.key}.jpg"); t = t.plusSeconds(1) } while (f.exists())
        return f
    }

    fun relabel(context: Context, photo: PhotoEntry, meal: Meal) {
        if (meal == photo.meal) return
        val target = File(photo.file.parentFile, "${STAMP.format(photo.time)}_${meal.key}.jpg")
        if (!target.exists() && photo.file.renameTo(target)) changed(context)
    }

    fun delete(context: Context, photo: PhotoEntry) {
        photo.file.delete()
        tidy(photo.file.parentFile)
        changed(context)
    }

    fun addActivity(context: Context, text: String, now: LocalDateTime = LocalDateTime.now()) {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return
        val d = dir(context, now.toLocalDate()).apply { mkdirs() }
        File(d, ACTIVITIES).appendText("${CLOCK.format(now.toLocalTime().withNano(0))}\t$clean\n")
        changed(context)
    }

    /** Rewrites one line of the day's activities; a blank [text] removes it. */
    fun editActivity(context: Context, date: LocalDate, entry: ActivityEntry, text: String?) {
        val d = dir(context, date)
        val f = File(d, ACTIVITIES)
        if (!f.exists()) return
        val lines = f.readLines().toMutableList()
        if (entry.line !in lines.indices) return
        val clean = text?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (clean.isEmpty()) lines.removeAt(entry.line) else lines[entry.line] = "${CLOCK.format(entry.time)}\t$clean"
        if (lines.isEmpty()) f.delete() else f.writeText(lines.joinToString("\n", postfix = "\n"))
        tidy(d)
        changed(context)
    }

    private fun tidy(d: File?) { if (d != null && d.list()?.isEmpty() == true) d.delete() }
}
