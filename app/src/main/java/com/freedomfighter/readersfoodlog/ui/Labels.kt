package com.freedomfighter.readersfoodlog.ui

import android.content.Context
import com.freedomfighter.readersfoodlog.R
import com.freedomfighter.readersfoodlog.data.Meal
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

    fun summary(context: Context, photos: Int, activities: Int, weight: String? = null): String = listOfNotNull(
        if (photos > 0) context.resources.getQuantityString(R.plurals.n_photos, photos, photos) else null,
        if (activities > 0) context.resources.getQuantityString(R.plurals.n_activities, activities, activities) else null,
        weight
    ).joinToString(" · ")
}
