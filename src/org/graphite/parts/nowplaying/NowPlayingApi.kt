package org.graphite.parts.nowplaying

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Talks to Graphite Now Playing (org.graphite.nowplaying), which keeps Ambient Music Mod's logic. This page only shows
 * and changes what that app exposes through its provider (GraphiteApiProvider in the bjakra/AmbientMusicMod fork).
 * Calls can wait on the recognition engine, so they run off the main thread.
 */
object NowPlayingApi {
    const val ENGINE_PACKAGE = "com.kieronquinn.app.pixelambientmusic"
    private const val AUTHORITY = "org.graphite.nowplaying.api"
    private val URI: Uri = Uri.parse("content://$AUTHORITY")
    val URI_STATE: Uri = Uri.parse("content://$AUTHORITY/state")

    private const val KEY_VALUE = "value"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun isInstalled(context: Context) =
        context.packageManager.resolveContentProvider(AUTHORITY, 0) != null

    /** Runs [call] on the worker thread and hands the result to [result] on the main thread. */
    fun <T> load(call: () -> T, result: (T) -> Unit) {
        executor.execute {
            val value = call()
            main.post { result(value) }
        }
    }

    val URI_RECOGNITION: Uri = Uri.parse("content://$AUTHORITY/recognition")

    fun state(context: Context): Bundle? = call(context, "getState")

    fun database(context: Context): Bundle? = call(context, "getDatabase")

    fun recognition(context: Context): Bundle? = call(context, "getRecognition")

    fun updateDatabase(context: Context) = executor.execute { call(context, "updateDatabase") }

    const val CORE_PACKAGE = "org.graphite.nowplaying"
    val URI_BACKUP: Uri = Uri.parse("content://$AUTHORITY/backup")

    fun tracks(context: Context, search: String): Bundle? = call(context, "getTracks", search)

    fun backupState(context: Context): Bundle? = call(context, "getBackup")

    fun backup(context: Context, uri: Uri) = executor.execute { call(context, "backup", uri.toString()) }

    fun restore(context: Context, uri: Uri, options: Bundle) =
        executor.execute { call(context, "restore", uri.toString(), options) }

    fun setBackupFolder(context: Context, uri: Uri) =
        executor.execute { call(context, "setBackupFolder", uri.toString()) }

    fun countries(context: Context): Bundle? = call(context, "getCountries")

    fun setCountry(context: Context, code: String) = executor.execute { call(context, "setCountry", code) }

    fun setExtraCountries(context: Context, codes: Array<String>) = executor.execute {
        call(context, "setExtraCountries", extras = Bundle().apply { putStringArray(KEY_VALUE, codes) })
    }

    fun recognise(context: Context) = executor.execute { call(context, "recognise") }

    fun cancelRecognition(context: Context) = executor.execute { call(context, "cancelRecognition") }

    fun settings(context: Context): Bundle? = call(context, "getSettings")

    fun setEnabled(context: Context, enabled: Boolean) {
        executor.execute { call(context, "setEnabled", extras = Bundle().apply { putBoolean(KEY_VALUE, enabled) }) }
    }

    fun set(context: Context, key: String, value: Any) {
        val extras = Bundle().apply {
            when (value) {
                is Boolean -> putBoolean(KEY_VALUE, value)
                is Int -> putInt(KEY_VALUE, value)
                is Long -> putLong(KEY_VALUE, value)
                is Float -> putFloat(KEY_VALUE, value)
                else -> putString(KEY_VALUE, value.toString())
            }
        }
        executor.execute { call(context, "setSetting", key, extras) }
    }

    private fun call(context: Context, method: String, arg: String? = null, extras: Bundle? = null): Bundle? =
        runCatching { context.contentResolver.call(URI, method, arg, extras) }.getOrNull()
}
