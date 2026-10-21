package com.freedomfighter.readersfoodlog.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Photos decoded at screen width, upright, kept in memory while they fit. */
object Pictures {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 5).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun cached(file: File): Bitmap? = cache.get(file.path)

    /** Blocking: call it off the main thread. */
    fun load(file: File, widthPx: Int): Bitmap? {
        cache.get(file.path)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        val rotation = runCatching { ExifInterface(file.path).rotationDegrees }.getOrDefault(0)
        val shownWidth = if (rotation % 180 == 0) bounds.outWidth else bounds.outHeight
        var sample = 1
        while (shownWidth / (sample * 2) >= widthPx) sample *= 2
        val raw = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val upright = if (rotation == 0) raw else
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true).also { if (it !== raw) raw.recycle() }
        cache.put(file.path, upright)
        return upright
    }

    /**
     * Copies a picture chosen outside the app into [target] the way the camera would have
     * saved it: upright, at most [maxSide] pixels on its longer side, JPEG at [quality].
     * Blocking: call it off the main thread. False when the picture could not be read.
     */
    fun save(context: Context, source: Uri, target: File, maxSide: Int = 1600, quality: Int = 88): Boolean = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (resolver.openInputStream(source) ?: return false).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        val rotation = resolver.openInputStream(source)?.use { runCatching { ExifInterface(it).rotationDegrees }.getOrDefault(0) } ?: 0
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / (sample * 2) >= maxSide) sample *= 2
        val raw = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return false
        val scale = maxSide.toFloat() / maxOf(raw.width, raw.height)
        val matrix = Matrix().apply { if (scale < 1f) postScale(scale, scale); if (rotation != 0) postRotate(rotation.toFloat()) }
        val ready = if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { if (it !== raw) raw.recycle() }
        target.parentFile?.mkdirs()
        val ok = target.outputStream().use { ready.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        ready.recycle()
        if (!ok) target.delete()
        ok
    }.getOrElse { Log.w("FoodLog", "picture not saved", it); target.delete(); false }
}
