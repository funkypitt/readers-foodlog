package com.freedomfighter.readersfoodlog.ui

import android.content.Context
import com.freedomfighter.readersfoodlog.R
import com.freedomfighter.readersfoodlog.data.Meal
import com.freedomfighter.readersfoodlog.data.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Words shared by the screens, the camera and the widgets. */
object Labels {
    fun meal(context: Context, m: Meal): String = context.getString(when (m) {
        Meal.BREAKFAST -> R.string.meal_breakfast
        Meal.LUNCH -> R.string.meal_lunch
        Meal.DINNER -> R.string.meal_dinner
        Meal.SNACK -> R.string.meal_snack
    })

    fun day(context: Context, date: LocalDate, today: LocalDate): String = when (date) {
        today -> context.getString(R.string.today)
        today.minusDays(1) -> context.getString(R.string.yesterday)
        else -> {
            val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
            DateTimeFormatter.ofPattern(if (date.year == today.year) "EEEE d MMMM" else "EEE d MMM yyyy", locale).format(date).lowercase(locale)
        }
    }

    /** The name of a theme, as the settings row shows it. */
    fun themeName(c: Context, m: ThemeMode): String = c.getString(when (m) {
        ThemeMode.DARK -> R.string.theme_dark; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LAVANDE -> R.string.theme_lavande; ThemeMode.SAUGE -> R.string.theme_sauge; ThemeMode.NORDIQUE -> R.string.theme_nordique
        ThemeMode.TERRACOTTA -> R.string.theme_terracotta; ThemeMode.COQUETTE -> R.string.theme_coquette; ThemeMode.MILITANTE -> R.string.theme_militante
        ThemeMode.TORRIDE -> R.string.theme_torride; ThemeMode.NATURE -> R.string.theme_nature
    })

    fun summary(context: Context, photos: Int, activities: Int, weight: String? = null): String = listOfNotNull(
        if (photos > 0) context.resources.getQuantityString(R.plurals.n_photos, photos, photos) else null,
        if (activities > 0) context.resources.getQuantityString(R.plurals.n_activities, activities, activities) else null,
        weight
    ).joinToString(" · ")
}
