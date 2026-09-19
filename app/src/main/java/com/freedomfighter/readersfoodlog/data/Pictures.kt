package com.freedomfighter.readersfoodlog.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
}
