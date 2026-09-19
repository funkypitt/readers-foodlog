package com.freedomfighter.readersfoodlog.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.freedomfighter.readersfoodlog.CaptureActivity
import com.freedomfighter.readersfoodlog.MainActivity
import com.freedomfighter.readersfoodlog.R
import com.freedomfighter.readersfoodlog.data.Journal
import com.freedomfighter.readersfoodlog.ui.Labels
import java.time.LocalDate

object FoodWidgets {
    fun refresh(context: Context) = WidgetUi.refresh(context, ShotWidget::class.java, LineWidget::class.java)
    fun capture(context: Context) = WidgetUi.activity(context, Intent(context, CaptureActivity::class.java), 1)
    fun activity(context: Context) = WidgetUi.activity(context, Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_ADD_ACTIVITY), 2)
    fun open(context: Context) = WidgetUi.activity(context, Intent(context, MainActivity::class.java), 4)
}

/** One cell, one gesture: touch it and the camera is open. */
class ShotWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val v = RemoteViews(context.packageName, R.layout.widget_shot)
            WidgetUi.paint(v, context, intArrayOf(R.id.widget_plus))
            v.setOnClickPendingIntent(R.id.widget_root, FoodWidgets.capture(context))
            mgr.updateAppWidget(id, v)
        }
    }
}

/** Today's count, with the activity pencil and the camera. */
class LineWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val today = LocalDate.now()
        val entries = Journal.entries(context, today)
        val photos = entries.count { it is com.freedomfighter.readersfoodlog.data.PhotoEntry }
        val summary = Labels.summary(context, photos, entries.size - photos).ifEmpty { context.getString(R.string.nothing_yet) }
        for (id in ids) {
            val v = RemoteViews(context.packageName, R.layout.widget_line)
            WidgetUi.paint(v, context, intArrayOf(R.id.widget_title, R.id.widget_activity, R.id.widget_plus), intArrayOf(R.id.widget_sub))
            v.setTextViewText(R.id.widget_title, context.getString(R.string.app_title))
            v.setTextViewText(R.id.widget_sub, context.getString(R.string.today) + " · " + summary)
            v.setOnClickPendingIntent(R.id.widget_body, FoodWidgets.open(context))
            v.setOnClickPendingIntent(R.id.widget_activity, FoodWidgets.activity(context))
            v.setOnClickPendingIntent(R.id.widget_plus, FoodWidgets.capture(context))
            mgr.updateAppWidget(id, v)
        }
    }
}
