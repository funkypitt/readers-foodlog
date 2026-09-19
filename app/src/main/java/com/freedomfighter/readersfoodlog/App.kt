package com.freedomfighter.readersfoodlog

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.freedomfighter.readersfoodlog.data.Prefs

class App : Application() {
    val prefs: Prefs by lazy { Prefs(this) }
    /** True while the "which activity?" sheet is open; the widget and the shortcut set it too. */
    val activityPrompt = mutableStateOf(false)
    /** Counts the returns to the app, so "today" is read again after midnight. */
    val resumes = mutableIntStateOf(0)
    override fun onCreate() { super.onCreate(); prefs }
}
