package org.graphite.parts.nowplaying

import android.app.AlertDialog
import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.graphite.parts.R

/**
 * "Recognise now", what Ambient Music Mod's recognition screen did for on-device recognition: start it, show
 * listening and recognising, then the song or why nothing matched, with Retry. The recognition runs in Graphite
 * Now Playing; this follows its state.
 */
class RecognitionDialog(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val dialog: AlertDialog = AlertDialog.Builder(context)
        .setTitle(R.string.nowplaying_recognise)
        .setMessage(R.string.nowplaying_recognise_recording)
        .setNegativeButton(android.R.string.cancel) { _, _ -> NowPlayingApi.cancelRecognition(context) }
        .setPositiveButton(R.string.nowplaying_recognise_retry, null)
        .setOnDismissListener { context.contentResolver.unregisterContentObserver(observer) }
        .create()

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    fun show() {
        context.contentResolver.registerContentObserver(NowPlayingApi.URI_RECOGNITION, false, observer)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            isEnabled = false
            setOnClickListener { start() }
        }
        start()
    }

    private fun start() {
        dialog.setMessage(context.getString(R.string.nowplaying_recognise_recording))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        NowPlayingApi.recognise(context)
    }

    private fun refresh() {
        NowPlayingApi.load({ NowPlayingApi.recognition(context) }) { state ->
            if (state == null || !dialog.isShowing) return@load
            val finished = state.getString("phase") in setOf("recognised", "failed", "error")
            dialog.setMessage(message(state))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = finished
        }
    }

    private fun message(state: Bundle): String = when (state.getString("phase")) {
        "recording" -> context.getString(R.string.nowplaying_recognise_recording)
        "recognising" -> context.getString(R.string.nowplaying_recognise_recognising)
        "recognised" -> context.getString(
            R.string.nowplaying_history_last, state.getString("track"), state.getString("artist")
        )
        "failed" -> when (state.getString("reason")) {
            "NoMatch" -> context.getString(R.string.nowplaying_recognise_no_match)
            "SkippedOnCall" -> skipped(R.string.nowplaying_recognise_on_call)
            "SkippedMusicPlaying" -> skipped(R.string.nowplaying_recognise_music_playing)
            "SkippedSystemUserNotInForeground" -> skipped(R.string.nowplaying_recognise_system_user)
            "SkippedAudioRecordFailed" -> context.getString(R.string.nowplaying_recognise_audio_failed)
            else -> context.getString(R.string.nowplaying_recognise_failed)
        }
        "error" -> if (state.getString("reason") == "DISABLED") {
            context.getString(R.string.nowplaying_recognise_disabled)
        } else {
            context.getString(R.string.nowplaying_recognise_failed)
        }
        else -> context.getString(R.string.nowplaying_recognise_recording)
    }

    private fun skipped(reason: Int) =
        context.getString(R.string.nowplaying_recognise_skipped, context.getString(reason))
}
