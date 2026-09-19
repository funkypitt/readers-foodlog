package com.freedomfighter.readersfoodlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Size
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.freedomfighter.readersfoodlog.data.Journal
import com.freedomfighter.readersfoodlog.data.Meal
import com.freedomfighter.readersfoodlog.ui.Labels
import com.freedomfighter.readersfoodlog.ui.LocalColors
import com.freedomfighter.readersfoodlog.ui.LocalTypo
import com.freedomfighter.readersfoodlog.ui.ReaderTheme
import com.freedomfighter.readersfoodlog.ui.Small
import com.freedomfighter.readersfoodlog.ui.T
import com.freedomfighter.readersfoodlog.ui.noRippleClickable
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The whole gesture: the viewfinder fills the screen, a touch anywhere (or a volume key)
 * takes the photo, the label shows for a moment, and the screen closes by itself.
 */
class CaptureActivity : ComponentActivity() {
    private val app get() = application as App
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val handler = Handler(Looper.getMainLooper())
    private var capture: ImageCapture? = null
    private var busy = false
    private var granted by mutableStateOf(false)
    private var asked = false
    private var saved by mutableStateOf<String?>(null)
    private var failed by mutableStateOf(false)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) bind()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        // Over the lock screen too: it only ever adds a photo, it shows nothing of the journal.
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val finder = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        this.finder = finder
        if (granted) bind() else { asked = true; permission.launch(Manifest.permission.CAMERA) }

        setContent {
            val settings by app.prefs.settings.collectAsState()
            ReaderTheme(settings) {
                val colors = LocalColors.current
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    if (granted) AndroidView({ finder }, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().noRippleClickable { if (granted) shoot() else askAgain() })
                    val done = saved
                    when {
                        done != null -> Box(Modifier.fillMaxSize().background(colors.bg), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                T("✓", size = LocalTypo.current.big)
                                T(done, Modifier.padding(top = 12.dp))
                            }
                        }
                        !granted -> Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                            T(stringResource(R.string.camera_needed), color = Color.White, align = TextAlign.Center, maxLines = 6)
                        }
                        else -> {
                            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color.Black.copy(alpha = 0.45f)).windowInsetsPadding(WindowInsets.statusBars).padding(14.dp), contentAlignment = Alignment.Center) {
                                T(Labels.meal(this@CaptureActivity, Meal.at(LocalTime.now())), color = Color.White, maxLines = 1)
                            }
                            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.45f)).windowInsetsPadding(WindowInsets.navigationBars).padding(18.dp), contentAlignment = Alignment.Center) {
                                Small(stringResource(if (failed) R.string.failed else R.string.touch_anywhere), Modifier.fillMaxWidth(), color = Color.White, maxLines = 2, align = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    private var finder: PreviewView? = null

    private fun bind() {
        val view = finder ?: return
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                // A plate does not need twelve megapixels: light files, fast folders, small exports.
                val shot = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1600, 1200), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                    .setJpegQuality(88)
                    .build()
                val lens = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(this, lens, preview, shot)
                capture = shot
            }.onFailure { failed = true }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun shoot() {
        val shot = capture ?: return
        if (busy || saved != null) return
        busy = true
        val now = LocalDateTime.now()
        val file = Journal.newPhotoFile(this, now)
        window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        shot.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Journal.changed(this@CaptureActivity)
                saved = Labels.meal(this@CaptureActivity, Meal.at(now.toLocalTime())) + " · " + hm.format(now)
                handler.postDelayed({ finish() }, 900)
            }
            override fun onError(e: ImageCaptureException) {
                file.delete()
                file.parentFile?.let { if (it.list()?.isEmpty() == true) it.delete() }
                busy = false
                failed = true
            }
        })
    }

    /** Refused once already: Android no longer shows its dialog, so the app's page is the way. */
    private fun askAgain() {
        if (!asked || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) { asked = true; permission.launch(Manifest.permission.CAMERA) }
        else runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
    }

    override fun onResume() {
        super.onResume()
        if (!granted && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { granted = true; bind() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (granted && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA)) { shoot(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
