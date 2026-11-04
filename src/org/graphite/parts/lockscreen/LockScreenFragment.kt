package org.graphite.parts.lockscreen

import android.os.Bundle
import android.provider.Settings
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

class LockScreenFragment : PreferenceFragmentCompat() {

    private fun secureSwitch(context: android.content.Context, setting: String, titleRes: Int, summaryRes: Int) =
        SwitchPreferenceCompat(context).apply {
            title = getString(titleRes)
            summary = getString(summaryRes)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver, setting, 0) == 1
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(context.contentResolver, setting, if (value as Boolean) 1 else 0)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
            addPreference(secureSwitch(context, GraphiteSettings.DOUBLE_TAP_TO_SLEEP,
                R.string.double_tap_sleep_title, R.string.double_tap_sleep_summary))
            addPreference(SwitchPreferenceCompat(context).apply {
                title = getString(R.string.double_tap_wake_title)
                summary = getString(R.string.double_tap_wake_summary)
                isPersistent = false
                isChecked = Settings.Secure.getInt(context.contentResolver, GraphiteSettings.DOUBLE_TAP_TO_WAKE, 0) == 1
                setOnPreferenceChangeListener { _, value ->
                    val on = if (value as Boolean) 1 else 0
                    // Uses the tap sensor that "Tap to check phone" uses, so that one is switched on too
                    Settings.Secure.putInt(context.contentResolver, GraphiteSettings.DOUBLE_TAP_TO_WAKE, on)
                    if (on == 1) Settings.Secure.putInt(context.contentResolver, DOZE_TAP_GESTURE, 1)
                    true
                }
            })
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
            val glance = PreferenceCategory(context).apply { title = getString(R.string.at_a_glance_category) }
            addPreference(glance)
            glance.addPreference(SwitchPreferenceCompat(context).apply {
                key = GraphiteSettings.LOCKSCREEN_AT_A_GLANCE
                title = getString(R.string.lockscreen_at_a_glance_title)
                summary = getString(R.string.lockscreen_at_a_glance_summary)
                isPersistent = false
                isChecked = Settings.Secure.getInt(context.contentResolver, key, 1) == 1
                setOnPreferenceChangeListener { _, value ->
                    Settings.Secure.putInt(context.contentResolver, key, if (value as Boolean) 1 else 0)
                }
            })
        }
    }
}

private const val DOZE_TAP_GESTURE = "doze_tap_gesture"
