package com.freedomfighter.readersfoodlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freedomfighter.readersfoodlog.ui.DayScreen
import com.freedomfighter.readersfoodlog.ui.HomeScreen
import com.freedomfighter.readersfoodlog.ui.LocalColors
import com.freedomfighter.readersfoodlog.ui.Nav
import com.freedomfighter.readersfoodlog.ui.ReaderTheme
import com.freedomfighter.readersfoodlog.ui.Screen
import com.freedomfighter.readersfoodlog.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val nav = Nav()
    private val app get() = application as App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handle(intent)
        setContent {
            val settings by app.prefs.settings.collectAsState()
            ReaderTheme(settings) {
                Bars()
                when (val s = nav.current) {
                    Screen.Home -> HomeScreen(nav, app)
                    is Screen.DayView -> DayScreen(nav, app, s.date)
                    Screen.Settings -> SettingsScreen(nav, app)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    override fun onResume() { super.onResume(); app.resumes.intValue++ }

    private fun handle(intent: Intent?) {
        if (intent?.action != ACTION_ADD_ACTIVITY) return
        while (nav.stack.size > 1) nav.pop()
        app.activityPrompt.value = true
        intent.action = null
    }

    @Composable
    private fun Bars() {
        val colors = LocalColors.current
        val view = LocalView.current
        LaunchedEffect(colors.isDark) {
            val c = WindowInsetsControllerCompat(window, view)
            c.isAppearanceLightStatusBars = !colors.isDark
            c.isAppearanceLightNavigationBars = !colors.isDark
        }
    }

    companion object { const val ACTION_ADD_ACTIVITY = "com.freedomfighter.readersfoodlog.ADD_ACTIVITY" }
}
