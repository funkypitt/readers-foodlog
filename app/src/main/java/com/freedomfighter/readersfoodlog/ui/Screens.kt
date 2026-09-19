package com.freedomfighter.readersfoodlog.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.freedomfighter.readersfoodlog.App
import com.freedomfighter.readersfoodlog.BuildConfig
import com.freedomfighter.readersfoodlog.CaptureActivity
import com.freedomfighter.readersfoodlog.R
import com.freedomfighter.readersfoodlog.data.ActivityEntry
import com.freedomfighter.readersfoodlog.data.Backup
import com.freedomfighter.readersfoodlog.data.FontChoice
import com.freedomfighter.readersfoodlog.data.Journal
import com.freedomfighter.readersfoodlog.data.Meal
import com.freedomfighter.readersfoodlog.data.PhotoEntry
import com.freedomfighter.readersfoodlog.data.Pictures
import com.freedomfighter.readersfoodlog.data.TextSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class Screen {
    data object Home : Screen()
    data class DayView(val date: LocalDate) : Screen()
    data object Settings : Screen()
    data object Weight : Screen()
}

class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

fun photograph(context: Context) = context.startActivity(Intent(context, CaptureActivity::class.java))

// ---------------------------------------------------------------------------------------------
// Home: one folder per day
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeScreen(nav: Nav, app: App) {
    val context = LocalContext.current
    val version by Journal.version.collectAsState()
    val today = remember(version, app.resumes.intValue) { LocalDate.now() }
    val days = remember(version, today) { Journal.days(context) }
    val todays = days.firstOrNull { it.date == today }
    val s by app.prefs.settings.collectAsState()
    fun summary(d: com.freedomfighter.readersfoodlog.data.Day) = Labels.summary(context, d.photos, d.activities, d.weight?.let { Units.show(context, it, s.pounds) })
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.app_title), onBack = null, trailing = stringResource(R.string.settings), onTrailing = { nav.push(Screen.Settings) })
            LazyColumn(Modifier.weight(1f)) {
                item {
                    TextRow(
                        stringResource(R.string.today),
                        secondary = todays?.let { summary(it) } ?: stringResource(R.string.nothing_yet)
                    ) { nav.push(Screen.DayView(today)) }
                }
                items(days.filter { it.date != today }, key = { it.date.toEpochDay() }) { d ->
                    TextRow(Labels.day(context, d.date, today), secondary = summary(d)) { nav.push(Screen.DayView(d.date)) }
                }
            }
            ActionBar(nav, app, todays?.weight)
        }
    }
    ActivityLogger(app)
}

/** The two gestures of the app, under the thumb: photograph, note an activity. */
@Composable
fun ActionBar(nav: Nav, app: App, weightToday: Double?) {
    val context = LocalContext.current
    val colors = LocalColors.current
    val tick = rememberTick()
    Rule()
    val s by app.prefs.settings.collectAsState()
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f)) { TextRow("✎  " + stringResource(R.string.activity), size = LocalTypo.current.title) { tick(); app.activityPrompt.value = true } }
        Box(Modifier.weight(1f)) {
            TextRow(weightToday?.let { Units.show(context, it, s.pounds) } ?: ("⚖  " + stringResource(R.string.weight)), size = LocalTypo.current.title) { tick(); nav.push(Screen.Weight) }
        }
    }
    Box(
        Modifier.fillMaxWidth().background(colors.fg).noRippleClickable { tick(); photograph(context) }.padding(horizontal = rowPadH, vertical = 30.dp)
    ) { T("◉  " + stringResource(R.string.photograph), color = colors.bg, maxLines = 1) }
    Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
}

/** Three one-tap activities, or a free line; saved at the current time. */
@Composable
fun ActivityLogger(app: App) {
    val context = LocalContext.current
    var free by remember { mutableStateOf(false) }
    if (app.activityPrompt.value) {
        val quick = listOf(R.string.act_strength, R.string.act_intense, R.string.act_endurance).map { stringResource(it) }
        TextMenu(
            stringResource(R.string.activity_title),
            quick.map { label -> MenuItem(label) { Journal.addActivity(context, label) } },
            onDismiss = { app.activityPrompt.value = false },
            footer = listOf(MenuItem(stringResource(R.string.act_other)) { free = true })
        )
    }
    if (free) TextPrompt(stringResource(R.string.activity_free), onDone = { Journal.addActivity(context, it); free = false }, onCancel = { free = false })
}

// ---------------------------------------------------------------------------------------------
// A day: its photos and activities, in the order of the clock
// ---------------------------------------------------------------------------------------------

@Composable
fun DayScreen(nav: Nav, app: App, date: LocalDate) {
    val context = LocalContext.current
    val version by Journal.version.collectAsState()
    val today = remember(app.resumes.intValue) { LocalDate.now() }
    val entries = remember(version, date) { Journal.entries(context, date) }
    var photoMenu by remember { mutableStateOf<PhotoEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<PhotoEntry?>(null) }
    var activityMenu by remember { mutableStateOf<ActivityEntry?>(null) }
    var editing by remember { mutableStateOf<ActivityEntry?>(null) }
    var weighing by remember { mutableStateOf(false) }
    val s by app.prefs.settings.collectAsState()
    val weight = remember(version, date) { Journal.weight(context, date) }
    BackHandler { nav.pop() }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(Labels.day(context, date, today), onBack = { nav.pop() })
            if (entries.isEmpty() && weight == null) {
                Small(stringResource(R.string.nothing_yet), Modifier.weight(1f).padding(horizontal = rowPadH, vertical = 24.dp))
            } else LazyColumn(Modifier.weight(1f)) {
                if (weight != null) item { TextRow(Units.show(context, weight, s.pounds), secondary = stringResource(R.string.weight), size = LocalTypo.current.title) { weighing = true } }
                items(entries, key = { e -> if (e is PhotoEntry) e.file.name else "a" + (e as ActivityEntry).line + e.text }) { e ->
                    when (e) {
                        is PhotoEntry -> PhotoBlock(e) { photoMenu = e }
                        is ActivityEntry -> TextRow(e.text, secondary = HM.format(e.time) + " · " + stringResource(R.string.activity), size = LocalTypo.current.title) { activityMenu = e }
                    }
                }
                item { VSpace(24.dp) }
            }
            if (date == today) ActionBar(nav, app, weight) else Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
    if (date == today) ActivityLogger(app)

    if (weighing) WeightPrompt(app, date) { weighing = false }
    photoMenu?.let { p ->
        TextMenu(
            HM.format(p.time),
            Meal.entries.map { m -> MenuItem((if (m == p.meal) "● " else "○ ") + Labels.meal(context, m)) { Journal.relabel(context, p, m) } },
            onDismiss = { photoMenu = null },
            footer = listOf(
                MenuItem(stringResource(R.string.share)) { sharePhoto(context, p.file) },
                MenuItem(stringResource(R.string.delete)) { confirmDelete = p }
            )
        )
    }
    confirmDelete?.let { p ->
        TextMenu(stringResource(R.string.delete_photo_q), listOf(
            MenuItem(stringResource(R.string.delete_for_good)) { Journal.delete(context, p) },
            MenuItem(stringResource(R.string.action_cancel)) { }
        ), onDismiss = { confirmDelete = null })
    }
    activityMenu?.let { a ->
        TextMenu(HM.format(a.time) + " · " + a.text, listOf(
            MenuItem(stringResource(R.string.change_text)) { editing = a },
            MenuItem(stringResource(R.string.delete)) { Journal.editActivity(context, date, a, null) }
        ), onDismiss = { activityMenu = null })
    }
    editing?.let { a ->
        TextPrompt(stringResource(R.string.activity_free), initial = a.text, onDone = { Journal.editActivity(context, date, a, it); editing = null }, onCancel = { editing = null })
    }
}

@Composable
private fun PhotoBlock(p: PhotoEntry, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalColors.current
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val bitmap by produceState<Bitmap?>(Pictures.cached(p.file), p.file) {
        if (value == null) value = withContext(Dispatchers.IO) { Pictures.load(p.file, widthPx) }
    }
    Column(Modifier.fillMaxWidth().noRippleClickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = rowPadH).padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            T(Labels.meal(context, p.meal), size = LocalTypo.current.title, maxLines = 1)
            Small("   " + HM.format(p.time), maxLines = 1)
        }
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height), contentScale = ContentScale.FillWidth)
        else Box(Modifier.fillMaxWidth().aspectRatio(0.75f).border(1.dp, colors.rule))
    }
}

fun sharePhoto(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(send, null)) }
}

// ---------------------------------------------------------------------------------------------
// Settings: carry the journal to another phone, the look
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(nav: Nav, app: App) {
    val context = LocalContext.current
    val s by app.prefs.settings.collectAsState()
    val colors = LocalColors.current
    val typo = LocalTypo.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    fun say(text: String) { notice = text }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            working = true
            val n = withContext(Dispatchers.IO) { Backup.export(context, uri) }
            working = false
            say(if (n >= 0) context.getString(R.string.exported) else context.getString(R.string.failed))
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            working = true
            val n = withContext(Dispatchers.IO) { Backup.import(context, uri) }
            working = false
            say(when { n > 0 -> context.resources.getQuantityString(R.plurals.imported, n, n); n == 0 -> context.getString(R.string.nothing_new); else -> context.getString(R.string.not_a_journal) })
        }
    }
    BackHandler { nav.pop() }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.settings), onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Small(stringResource(R.string.local_hint), Modifier.padding(horizontal = rowPadH).padding(top = 16.dp, bottom = 4.dp), maxLines = 4)
                TextRow(stringResource(if (working) R.string.working else R.string.export_journal), secondary = stringResource(R.string.export_hint), size = typo.title) {
                    if (!working) exporter.launch("food-log-${LocalDate.now()}.zip")
                }
                TextRow(stringResource(R.string.import_journal), secondary = stringResource(R.string.import_hint), size = typo.title) {
                    if (!working) importer.launch(arrayOf("application/zip", "application/octet-stream"))
                }
                notice?.let { T(it, Modifier.padding(horizontal = rowPadH, vertical = 12.dp), size = typo.title) }
                Rule(Modifier.padding(vertical = 8.dp))
                TextRow(if (colors.isDark) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light), secondary = stringResource(R.string.colours)) { app.prefs.toggleTheme(colors.isDark) }
                TextRow(when (s.textSize) { TextSize.SMALL -> "S"; TextSize.MEDIUM -> "M"; TextSize.LARGE -> "L" }, secondary = stringResource(R.string.text_size)) {
                    app.prefs.setTextSize(when (s.textSize) { TextSize.SMALL -> TextSize.MEDIUM; TextSize.MEDIUM -> TextSize.LARGE; TextSize.LARGE -> TextSize.SMALL })
                }
                TextRow(when (s.font) { FontChoice.SANS -> "sans-serif"; FontChoice.SERIF -> "serif"; FontChoice.MONO -> "mono" }, secondary = stringResource(R.string.font)) {
                    app.prefs.setFont(when (s.font) { FontChoice.SANS -> FontChoice.SERIF; FontChoice.SERIF -> FontChoice.MONO; FontChoice.MONO -> FontChoice.SANS })
                }
                TextRow(if (s.pounds) "lb" else "kg", secondary = stringResource(R.string.weight_unit)) { app.prefs.setPounds(!s.pounds) }
                TextRow(if (s.haptics) stringResource(R.string.on) else stringResource(R.string.off), secondary = stringResource(R.string.haptics)) { app.prefs.setHaptics(!s.haptics) }
                Rule(Modifier.padding(vertical = 8.dp))
                Small(stringResource(R.string.meal_hours), Modifier.padding(horizontal = rowPadH, vertical = 12.dp), maxLines = 6)
                Rule(Modifier.padding(vertical = 8.dp))
                TextRow(stringResource(R.string.app_name), secondary = BuildConfig.VERSION_NAME, size = typo.title) { }
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}
