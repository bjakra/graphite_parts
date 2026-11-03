package org.graphite.parts.nowplaying

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R

/**
 * Now Playing settings, as Ambient Music Mod's Settings screen (settings/SettingsFragment there): recognition period,
 * buffer, adaptive period, screen-on trigger, and its Advanced options. Values and the choices of each list come from
 * Graphite Now Playing, which applies them through AMM's own repositories.
 */
class NowPlayingSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(preferenceManager.context)
        val context = requireContext()
        NowPlayingApi.load({ NowPlayingApi.settings(context) }) { settings ->
            if (isAdded && settings != null) build(settings)
        }
    }

    private fun build(settings: Bundle) {
        val screen = preferenceScreen
        list(settings, "recognitionPeriod", R.string.nowplaying_recognition_period,
            R.string.nowplaying_recognition_period_summary)?.let(screen::addPreference)
        list(settings, "recognitionBuffer", R.string.nowplaying_recognition_buffer,
            R.string.nowplaying_recognition_buffer_summary)?.let(screen::addPreference)
        switch(settings, "recognitionPeriodAdaptive", R.string.nowplaying_recognition_adaptive,
            R.string.nowplaying_recognition_adaptive_summary)?.let(screen::addPreference)
        switch(settings, "triggerWhenScreenOn", R.string.nowplaying_screen_on,
            R.string.nowplaying_screen_on_summary)?.let(screen::addPreference)

        val advanced = PreferenceCategory(preferenceManager.context).apply {
            title = getString(R.string.nowplaying_advanced)
        }
        screen.addPreference(advanced)
        switch(settings, "alternativeEncoding", R.string.nowplaying_alternative_encoding,
            R.string.nowplaying_alternative_encoding_summary)?.let(advanced::addPreference)
        switch(settings, "runOnSmallCores", R.string.nowplaying_small_cores,
            R.string.nowplaying_small_cores_summary)?.let(advanced::addPreference)
        switch(settings, "enableLogging", R.string.nowplaying_logging,
            R.string.nowplaying_logging_summary)?.let(advanced::addPreference)
    }

    private fun switch(settings: Bundle, key: String, title: Int, summary: Int): Preference? {
        val setting = settings.getBundle(key)?.takeIf { it.getString("type") == "bool" } ?: return null
        return SwitchPreferenceCompat(preferenceManager.context).apply {
            this.title = getString(title)
            this.summary = getString(summary)
            isPersistent = false
            isChecked = setting.getBoolean("value")
            setOnPreferenceChangeListener { _, value ->
                NowPlayingApi.set(context, key, value as Boolean)
                true
            }
        }
    }

    /** A list for one of AMM's enum settings, with AMM's own labels; [summary] takes the chosen label. */
    private fun list(settings: Bundle, key: String, title: Int, summary: Int): Preference? {
        val setting = settings.getBundle(key)?.takeIf { it.getString("type") == "enum" } ?: return null
        val options = setting.getStringArray("options") ?: return null
        val titles = setting.getStringArray("optionTitles") ?: options
        return ListPreference(preferenceManager.context).apply {
            this.title = getString(title)
            dialogTitle = this.title
            isPersistent = false
            entries = titles
            entryValues = options
            value = setting.getString("value")
            this.summary = getString(summary, entry ?: "")
            setOnPreferenceChangeListener { pref, newValue ->
                val index = options.indexOf(newValue as String)
                pref.summary = getString(summary, titles.getOrNull(index) ?: newValue)
                NowPlayingApi.set(context, key, newValue)
                true
            }
        }
    }
}
