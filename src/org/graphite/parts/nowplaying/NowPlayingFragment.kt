package org.graphite.parts.nowplaying

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R

/**
 * Now Playing, the page Ambient Music Mod's main screen was (NowPlayingFragment there): the switch, history,
 * notifications and the settings page. Recognition runs in Graphite Now Playing; this page only shows its state.
 */
class NowPlayingFragment : PreferenceFragmentCompat() {

    private lateinit var enabled: SwitchPreferenceCompat
    private lateinit var history: Preference
    private lateinit var notifications: Preference
    private lateinit var settings: Preference

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        enabled = SwitchPreferenceCompat(context).apply {
            title = getString(R.string.nowplaying_switch)
            summary = getString(R.string.nowplaying_switch_summary)
            isPersistent = false
            isEnabled = false
            setOnPreferenceChangeListener { _, value ->
                NowPlayingApi.setEnabled(context, value as Boolean)
                true
            }
        }
        history = Preference(context).apply {
            title = getString(R.string.nowplaying_history)
            intent = Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY")
                .setPackage(NowPlayingApi.ENGINE_PACKAGE)
        }
        notifications = Preference(context).apply {
            title = getString(R.string.nowplaying_notifications)
            intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, NowPlayingApi.ENGINE_PACKAGE)
        }
        settings = Preference(context).apply {
            title = getString(R.string.nowplaying_settings)
            summary = getString(R.string.nowplaying_settings_summary)
            fragment = NowPlayingSettingsFragment::class.java.name
        }
        listOf(enabled, history, notifications, settings).forEach(screen::addPreference)
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver.registerContentObserver(NowPlayingApi.URI_STATE, false, observer)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(observer)
    }

    private fun refresh() {
        val context = context ?: return
        NowPlayingApi.load({ NowPlayingApi.state(context) }) { state ->
            if (!isAdded) return@load
            val available = state?.getString("state") == "available"
            enabled.isEnabled = available
            enabled.isChecked = available && state!!.getBoolean("mainEnabled")
            if (!available) enabled.summary = getString(R.string.nowplaying_unavailable)
            listOf(history, notifications, settings).forEach { it.isEnabled = available }
            val track = state?.getString("lastTrack")
            history.summary = if (track != null) {
                getString(R.string.nowplaying_history_last, track, state.getString("lastArtist"))
            } else {
                getString(R.string.nowplaying_history_empty)
            }
            notifications.summary = getString(
                if (state?.getBoolean("notificationsEnabled") == true) R.string.nowplaying_on
                else R.string.nowplaying_off
            )
        }
    }
}
