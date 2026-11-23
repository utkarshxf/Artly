package com.orion.templete.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Debug builds only: when the app dies, save the real stack trace to a file, then offer it
 * for sharing on the next launch. This is how we get crash reports without a logcat setup.
 * Everything here is a no-op in release builds (isDebug() is false there).
 */
object DebugCrashCapture {

    private const val TAG = "DebugCrashCapture"
    private const val FILE_NAME = "last_crash.txt"

    fun isDebug(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun install(context: Context) {
        if (!isDebug(context)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val writer = StringWriter()
                error.printStackTrace(PrintWriter(writer))
                val report = buildString {
                    append("Thread: ").append(thread.name).append('\n')
                    append(error.toString()).append("\n\n")
                    append(writer.toString())
                }
                File(context.getExternalFilesDir(null), FILE_NAME).writeText(report)
            } catch (e: Exception) {
                Log.e(TAG, "couldn't save crash report", e)
            }
            if (previous != null) previous.uncaughtException(thread, error) else exitProcess(2)
        }
    }

    /** The saved report from the last crash, or null when there isn't one. */
    fun pending(context: Context): String? {
        if (!isDebug(context)) return null
        val file = File(context.getExternalFilesDir(null), FILE_NAME)
        if (!file.exists()) return null
        return try {
            file.readText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "couldn't read crash report", e)
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.getExternalFilesDir(null), FILE_NAME).delete()
        } catch (e: Exception) {
            Log.e(TAG, "couldn't delete crash report", e)
        }
    }

    fun share(context: Context, report: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Artistry debug crash report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        val chooser = Intent.createChooser(send, "Share crash report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
