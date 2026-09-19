package com.freedomfighter.readersfoodlog.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** The journal as one zip of its day folders, to carry it to another phone. */
object Backup {
    private val ENTRY = Regex("""\d{4}-\d{2}-\d{2}/(\d{6}_[a-z]+\.jpg|activities\.txt|weight\.txt)""")

    /** Blocking. Returns the number of files written, or -1 when it failed. */
    fun export(context: Context, target: Uri): Int = runCatching {
        val root = File(context.filesDir, "journal")
        var n = 0
        context.contentResolver.openOutputStream(target, "wt")!!.use { out ->
            ZipOutputStream(out.buffered()).use { zip ->
                zip.setLevel(0) // JPEGs do not compress
                root.listFiles()?.sortedBy { it.name }?.forEach { day ->
                    day.listFiles()?.sortedBy { it.name }?.forEach { f ->
                        val name = "${day.name}/${f.name}"
                        if (!ENTRY.matches(name)) return@forEach
                        zip.putNextEntry(ZipEntry(name).apply { time = f.lastModified() })
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        n++
                    }
                }
            }
        }
        n
    }.getOrDefault(-1)

    /**
     * Blocking. Merges the zip into the journal: photos already here are kept as they are,
     * activities are added line by line unless the same line exists. Returns the number of
     * new photos and activities, or -1 when the file is not a journal.
     */
    fun import(context: Context, source: Uri): Int = runCatching {
        val root = File(context.filesDir, "journal")
        var added = 0
        var recognised = 0
        context.contentResolver.openInputStream(source)!!.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory || !ENTRY.matches(e.name)) continue
                    recognised++
                    val target = File(root, e.name)
                    target.parentFile?.mkdirs()
                    if (target.name == "activities.txt") {
                        val have = if (target.exists()) target.readLines().toMutableList() else mutableListOf()
                        val fresh = zip.readBytes().decodeToString().lines().filter { it.isNotBlank() && it !in have }
                        if (fresh.isNotEmpty()) {
                            target.writeText((have + fresh).sorted().joinToString("\n", postfix = "\n"))
                            added += fresh.size
                        }
                    } else if (!target.exists() && !labelledTwin(target)) {
                        val tmp = File(target.parentFile, target.name + ".part")
                        tmp.outputStream().use { zip.copyTo(it) }
                        if (tmp.renameTo(target)) added++ else tmp.delete()
                    }
                }
            }
        }
        if (added > 0) Journal.changed(context)
        if (recognised == 0) -1 else added
    }.getOrDefault(-1)

    /** The same shot already here under another meal label (relabelled on one of the phones). */
    private fun labelledTwin(target: File): Boolean {
        val stamp = target.name.substringBefore('_')
        return target.parentFile?.list()?.any { it.startsWith(stamp + "_") && it.endsWith(".jpg") } == true
    }
}
