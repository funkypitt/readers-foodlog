package com.freedomfighter.readersfoodlog.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.freedomfighter.readersfoodlog.data.Journal
import com.freedomfighter.readersfoodlog.data.PhotoEntry
import com.freedomfighter.readersfoodlog.data.Prefs
import com.freedomfighter.readersfoodlog.ui.Labels
import com.freedomfighter.readersfoodlog.ui.Units
import java.time.LocalDate

/**
 * Today at a glance for Reader's Launcher's tile (signature-protected): the counts, and the
 * same words the app shows. It can be queried before Application.onCreate, so it reads the
 * folders and the raw preferences only.
 */
class TodayProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val context = context ?: return null
        if (uri.path != "/today") return null
        val today = LocalDate.now()
        val entries = Journal.entries(context, today)
        val photos = entries.count { it is PhotoEntry }
        val activities = entries.size - photos
        val weight = Journal.weight(context, today)
        val pounds = Prefs.pounds(Prefs.raw(context))
        val res = context.resources
        val c = MatrixCursor(arrayOf("photos", "activities", "weight_kg", "photos_label", "activities_label", "weight_label"))
        c.addRow(arrayOf<Any?>(
            photos, activities, weight,
            if (photos > 0) Labels.summary(context, photos, 0) else "",
            if (activities > 0) Labels.summary(context, 0, activities) else "",
            weight?.let { Units.show(context, it, pounds) } ?: ""
        ))
        c.setNotificationUri(context.contentResolver, URI)
        return c
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0

    companion object { val URI: Uri = Uri.parse("content://com.freedomfighter.readersfoodlog/today") }
}
