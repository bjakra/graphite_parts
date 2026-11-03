package org.graphite.parts.nowplaying

import android.app.AlertDialog
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R
import java.text.NumberFormat

/**
 * The music database, as Ambient Music Mod's Updates screen and its database options: the installed version, Update
 * now, automatic updates (Graphite: daily, weekly or monthly), Wi-Fi only and charging only. Updates are the only time
 * Now Playing connects to the internet; Graphite Now Playing opens and closes that connection.
 */
class NowPlayingDatabaseFragment : PreferenceFragmentCompat() {

    private lateinit var installed: Preference
    private lateinit var updateNow: Preference

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refreshDatabase()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen
        installed = Preference(context).apply {
            title = getString(R.string.nowplaying_database_installed)
            isSelectable = false
        }
        updateNow = Preference(context).apply {
            title = getString(R.string.nowplaying_database_update_now)
            summary = getString(R.string.nowplaying_database_update_now_summary)
            setOnPreferenceClickListener {
                AlertDialog.Builder(context)
                    .setTitle(R.string.nowplaying_database_warning_title)
                    .setMessage(R.string.nowplaying_database_warning)
                    .setPositiveButton(R.string.nowplaying_database_update) { _, _ ->
                        NowPlayingApi.updateDatabase(context)
                        summary = getString(R.string.nowplaying_database_updating)
                        isEnabled = false
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }
        screen.addPreference(installed)
        screen.addPreference(updateNow)
        NowPlayingApi.load({ NowPlayingApi.settings(context) }) { settings ->
            if (isAdded && settings != null) buildSettings(settings)
        }
    }

    private fun buildSettings(settings: Bundle) {
        val context = preferenceManager.context
        val automatic = settings.getBundle("automaticMusicDatabaseUpdates") ?: return
        val interval = settings.getBundle("automaticMusicDatabaseUpdatesInterval")
        val intervalPref = interval?.let { setting ->
            ListPreference(context).apply {
                title = getString(R.string.nowplaying_database_interval)
                dialogTitle = title
                isPersistent = false
                entries = setting.getStringArray("optionTitles")
                entryValues = setting.getStringArray("options")
                value = setting.getString("value")
                summary = entry
                isEnabled = automatic.getBoolean("value")
                setOnPreferenceChangeListener { pref, newValue ->
                    val index = entryValues.indexOf(newValue)
                    pref.summary = entries.getOrNull(index)
                    NowPlayingApi.set(context, "automaticMusicDatabaseUpdatesInterval", newValue)
                    true
                }
            }
        }
        val automaticPref = SwitchPreferenceCompat(context).apply {
            title = getString(R.string.nowplaying_database_automatic)
            summary = getString(R.string.nowplaying_database_automatic_summary)
            isPersistent = false
            isChecked = automatic.getBoolean("value")
            setOnPreferenceChangeListener { _, value ->
                val on = value as Boolean
                if (on) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.nowplaying_database_warning_title)
                        .setMessage(R.string.nowplaying_database_warning)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            isChecked = true
                            intervalPref?.isEnabled = true
                            NowPlayingApi.set(context, "automaticMusicDatabaseUpdates", true)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    false
                } else {
                    intervalPref?.isEnabled = false
                    NowPlayingApi.set(context, "automaticMusicDatabaseUpdates", false)
                    true
                }
            }
        }
        preferenceScreen.addPreference(automaticPref)
        intervalPref?.let(preferenceScreen::addPreference)
        switch(settings, "superpacksRequireWiFi", R.string.nowplaying_database_wifi,
            R.string.nowplaying_database_wifi_summary)
        switch(settings, "superpacksRequireCharging", R.string.nowplaying_database_charging,
            R.string.nowplaying_database_charging_summary)
        preferenceScreen.addPreference(Preference(context).apply {
            summary = getString(R.string.nowplaying_database_footer)
            isSelectable = false
        })
    }

    private fun switch(settings: Bundle, key: String, title: Int, summary: Int) {
        val setting = settings.getBundle(key) ?: return
        preferenceScreen.addPreference(SwitchPreferenceCompat(preferenceManager.context).apply {
            this.title = getString(title)
            this.summary = getString(summary)
            isPersistent = false
            isChecked = setting.getBoolean("value")
            setOnPreferenceChangeListener { _, value ->
                NowPlayingApi.set(context, key, value as Boolean)
                true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver.registerContentObserver(NowPlayingApi.URI_STATE, false, observer)
        refreshDatabase()
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(observer)
    }

    private fun refreshDatabase() {
        val context = context ?: return
        NowPlayingApi.load({ NowPlayingApi.database(context) }) { db ->
            if (!isAdded) return@load
            val version = db?.getInt("version") ?: 0
            installed.summary = if (db == null || version == 0) {
                getString(R.string.nowplaying_database_none)
            } else {
                getString(
                    R.string.nowplaying_database_installed_summary, version, db.getString("date"),
                    NumberFormat.getIntegerInstance().format(db.getInt("tracks")),
                    db.getStringArray("countries")?.joinToString().orEmpty()
                )
            }
            val updating = db?.getBoolean("updating") == true
            updateNow.isEnabled = !updating
            updateNow.summary = getString(
                if (updating) R.string.nowplaying_database_updating else R.string.nowplaying_database_update_now_summary
            )
        }
    }
}
