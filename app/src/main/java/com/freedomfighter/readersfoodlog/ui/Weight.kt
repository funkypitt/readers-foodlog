package com.freedomfighter.readersfoodlog.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedomfighter.readersfoodlog.App
import com.freedomfighter.readersfoodlog.R
import com.freedomfighter.readersfoodlog.data.Journal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Weights live in kilograms on disk; pounds are only a way of typing and showing them. */
object Units {
    private const val LB = 0.45359237

    fun show(context: Context, kg: Double, pounds: Boolean, sign: Boolean = false): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val v = if (pounds) kg / LB else kg
        return String.format(locale, if (sign) "%+.1f" else "%.1f", v) + " " + if (pounds) "lb" else "kg"
    }

    /** "72,4" or "72.4" in the current unit, to kilograms; null when it is not a plausible weight. */
    fun parse(text: String, pounds: Boolean): Double? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val kg = if (pounds) v * LB else v
        return kg.takeIf { it in 20.0..400.0 }
    }
}

/** Asks for the weight of [date]; an implausible entry is simply asked again. */
@Composable
fun WeightPrompt(app: App, date: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val s by app.prefs.settings.collectAsState()
    val last = remember { Journal.weights(context).lastOrNull { it.first <= date } }
    val title = stringResource(if (s.pounds) R.string.weight_prompt_lb else R.string.weight_prompt_kg) +
        (last?.let { " · " + stringResource(R.string.weight_last, Units.show(context, it.second, s.pounds)) } ?: "")
    TextPrompt(title, keyboard = KeyboardType.Decimal, onDone = { typed ->
        Units.parse(typed, s.pounds)?.let { Journal.setWeight(context, date, it); onClose() }
    }, onCancel = onClose)
}

private enum class Span(val days: Long?, val label: Int) {
    MONTH(30, R.string.span_30), QUARTER(90, R.string.span_90), YEAR(365, R.string.span_year), ALL(null, R.string.span_all)
}

@Composable
fun WeightScreen(nav: Nav, app: App) {
    val context = LocalContext.current
    val s by app.prefs.settings.collectAsState()
    val version by Journal.version.collectAsState()
    val today = remember(app.resumes.intValue) { LocalDate.now() }
    val all = remember(version) { Journal.weights(context) }
    var span by remember { mutableStateOf(Span.QUARTER) }
    var asking by remember { mutableStateOf<LocalDate?>(null) }
    var menu by remember { mutableStateOf<Pair<LocalDate, Double>?>(null) }
    // Straight to the point: no weight today yet, so the question is already open.
    LaunchedEffect(Unit) { if (all.none { it.first == today }) asking = today }
    BackHandler { nav.pop() }

    val shown = span.days?.let { d -> all.filter { it.first >= today.minusDays(d) } } ?: all
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.weight), onBack = { nav.pop() }, trailing = stringResource(span.label), onTrailing = { span = Span.entries[(span.ordinal + 1) % Span.entries.size] })
            LazyColumn(Modifier.weight(1f)) {
                item {
                    if (shown.size >= 2) {
                        Curve(shown, s.pounds)
                        val delta = shown.last().second - shown.first().second
                        Small(
                            Units.show(context, delta, s.pounds, sign = true) + " · " + Labels.day(context, shown.first().first, today) + " → " + Labels.day(context, shown.last().first, today),
                            Modifier.padding(horizontal = rowPadH).padding(bottom = 14.dp)
                        )
                    } else Small(stringResource(R.string.curve_needs_two), Modifier.padding(horizontal = rowPadH, vertical = 28.dp), maxLines = 3)
                    Rule()
                    val todays = all.firstOrNull { it.first == today }
                    TextRow(todays?.let { Units.show(context, it.second, s.pounds) } ?: stringResource(R.string.weight_enter), inverted = todays == null, secondary = stringResource(R.string.today)) { asking = today }
                    Rule()
                }
                items(all.filter { it.first != today }.asReversed(), key = { it.first.toEpochDay() }) { w ->
                    TextRow(Units.show(context, w.second, s.pounds), secondary = Labels.day(context, w.first, today), size = LocalTypo.current.title) { menu = w }
                }
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
    menu?.let { w ->
        TextMenu(Labels.day(context, w.first, today) + " · " + Units.show(context, w.second, s.pounds), listOf(
            MenuItem(stringResource(R.string.weight_change)) { asking = w.first },
            MenuItem(stringResource(R.string.delete)) { Journal.setWeight(context, w.first, null) }
        ), onDismiss = { menu = null })
    }
    asking?.let { d -> WeightPrompt(app, d) { asking = null } }
}

/** The curve: a line through the days, highest and lowest values on the left, first and last day below. */
@Composable
private fun Curve(points: List<Pair<LocalDate, Double>>, pounds: Boolean) {
    val context = LocalContext.current
    val colors = LocalColors.current
    val lo = points.minOf { it.second }
    val hi = points.maxOf { it.second }
    val pad = ((hi - lo) * 0.12).coerceAtLeast(0.3)
    val bottom = lo - pad
    val top = hi + pad
    val first = points.first().first
    val days = ChronoUnit.DAYS.between(first, points.last().first).coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().padding(horizontal = rowPadH).padding(top = 20.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Small(Units.show(context, hi, pounds), Modifier.weight(1f), maxLines = 1)
            Small(Units.show(context, points.last().second, pounds), color = colors.fg, maxLines = 1, align = TextAlign.End)
        }
        Canvas(Modifier.fillMaxWidth().height(220.dp).padding(vertical = 6.dp)) {
            val w = size.width
            val h = size.height
            fun x(d: LocalDate) = (ChronoUnit.DAYS.between(first, d).toFloat() / days) * w
            fun y(v: Double) = (h - ((v - bottom) / (top - bottom)) * h).toFloat()
            val rule = colors.fg.copy(alpha = 0.25f)
            drawLine(rule, Offset(0f, y(hi)), Offset(w, y(hi)), 1.dp.toPx())
            drawLine(rule, Offset(0f, y(lo)), Offset(w, y(lo)), 1.dp.toPx())
            for (i in 1 until points.size) {
                drawLine(colors.fg, Offset(x(points[i - 1].first), y(points[i - 1].second)), Offset(x(points[i].first), y(points[i].second)), 2.dp.toPx(), cap = StrokeCap.Round)
            }
            if (points.size <= 45) points.forEach { drawCircle(colors.fg, 3.5.dp.toPx(), Offset(x(it.first), y(it.second))) }
            val end = points.last()
            drawCircle(colors.bg, 6.dp.toPx(), Offset(x(end.first), y(end.second)))
            drawCircle(colors.fg, 6.dp.toPx(), Offset(x(end.first), y(end.second)), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        Small(Units.show(context, lo, pounds), maxLines = 1)
    }
}
