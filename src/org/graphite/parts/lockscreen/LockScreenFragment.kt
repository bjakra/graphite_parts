package org.graphite.parts.lockscreen

import android.os.Bundle
import android.provider.Settings
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

class LockScreenFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
            addPreference(SwitchPreferenceCompat(context).apply {
                key = GraphiteSettings.CHARGING_INFO
                title = getString(R.string.charging_info_title)
                summary = getString(R.string.charging_info_summary)
                isPersistent = false
                isChecked = Settings.Secure.getInt(context.contentResolver, key, 0) == 1
                setOnPreferenceChangeListener { _, value ->
                    Settings.Secure.putInt(context.contentResolver, key, if (value as Boolean) 1 else 0)
                }
            })
        }
    }
}
