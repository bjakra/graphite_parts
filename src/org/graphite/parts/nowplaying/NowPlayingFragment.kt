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
    private lateinit var recognise: Preference
    private lateinit var database: Preference
    private lateinit var showSong: SwitchPreferenceCompat
    private lateinit var backup: Preference

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    /**
     * Graphite: Now Playing can only recognise songs once its database is on the phone. After switching
     * it on without one, offer the download, with the same notice as "Update now".
     */
    private fun offerDownloadIfMissing() {
        val context = context ?: return
        NowPlayingApi.load({ NowPlayingApi.database(context) }) { db ->
            if (!isAdded || db == null || db.getBoolean("installed", true) || db.getBoolean("updating")) return@load
            NowPlayingApi.load({ NowPlayingApi.countries(context) }) { countries ->
                if (isAdded && countries != null) NowPlayingCountries.setupCountries(requireContext(), countries)
            }
        }
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
                if (value) offerDownloadIfMissing()
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
        recognise = Preference(context).apply {
            title = getString(R.string.nowplaying_recognise)
            summary = getString(R.string.nowplaying_recognise_summary)
            setOnPreferenceClickListener {
                RecognitionDialog(requireContext()).show()
                true
            }
        }
        database = Preference(context).apply {
            title = getString(R.string.nowplaying_database)
            fragment = NowPlayingDatabaseFragment::class.java.name
        }
        settings = Preference(context).apply {
            title = getString(R.string.nowplaying_settings)
            summary = getString(R.string.nowplaying_settings_summary)
            fragment = NowPlayingSettingsFragment::class.java.name
        }
        showSong = SwitchPreferenceCompat(context).apply {
            title = getString(R.string.nowplaying_show_song)
            summary = getString(R.string.nowplaying_show_song_summary)
            isPersistent = false
            isEnabled = false
            setOnPreferenceChangeListener { _, value ->
                NowPlayingApi.set(context, "graphiteShowSong", value as Boolean)
                true
            }
        }
        backup = Preference(context).apply {
            title = getString(R.string.nowplaying_backup)
            summary = getString(R.string.nowplaying_backup_summary)
            fragment = NowPlayingBackupFragment::class.java.name
        }
        listOf(enabled, recognise, history, showSong, notifications, database, settings, backup)
            .forEach(screen::addPreference)

        // Opened from the Quick Settings tile (through Graphite Now Playing): start "Recognise now"
        if (savedInstanceState == null &&
            activity?.intent?.getBooleanExtra("org.graphite.nowplaying.RECOGNISE", false) == true) {
            activity?.intent?.removeExtra("org.graphite.nowplaying.RECOGNISE")
            RecognitionDialog(requireContext()).show()
        }
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
            // Before the first switch-on, Now Playing reports "notSetup" (AMM's setup state): it's
            // available and off; turning it on here completes the setup
            val available = state?.getString("state") in setOf("available", "notSetup")
            enabled.isEnabled = available
            enabled.isChecked = state?.getString("state") == "available" && state.getBoolean("mainEnabled")
            enabled.summary = when {
                !available -> getString(R.string.nowplaying_unavailable)
                state?.getBoolean("microphoneDisabled") == true -> getString(R.string.nowplaying_microphone_off)
                else -> getString(R.string.nowplaying_switch_summary)
            }
            listOf(history, notifications, settings, database, backup).forEach { it.isEnabled = available }
            recognise.isEnabled = enabled.isChecked
            val track = state?.getString("lastTrack")
            history.summary = if (track != null) {
                getString(R.string.nowplaying_history_last, track, state.getString("lastArtist"))
            } else {
                getString(R.string.nowplaying_history_empty)
            }
            NowPlayingApi.load({ NowPlayingApi.settings(context) }) { settings ->
                if (!isAdded) return@load
                val setting = settings?.getBundle("graphiteShowSong")
                showSong.isEnabled = available && setting != null
                showSong.isChecked = setting?.getBoolean("value") ?: true
            }
            NowPlayingApi.load({ NowPlayingApi.database(context) }) { db ->
                if (!isAdded) return@load
                val tracks = db?.getInt("tracks") ?: 0
                database.summary = when {
                    db?.getBoolean("updating") == true -> getString(R.string.nowplaying_database_downloading)
                    // Graphite: the songs of this version were never downloaded, so nothing can be recognised
                    db != null && !db.getBoolean("installed", true) ->
                        getString(R.string.nowplaying_database_missing)
                    tracks > 0 -> getString(
                        R.string.nowplaying_database_summary,
                        java.text.NumberFormat.getIntegerInstance().format(tracks), db!!.getString("date")
                    )
                    else -> getString(R.string.nowplaying_database_none)
                }
            }
            notifications.summary = getString(
                if (state?.getBoolean("notificationsEnabled") == true) R.string.nowplaying_on
                else R.string.nowplaying_off
            )
        }
    }
}
