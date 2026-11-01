package org.graphite.parts.statusbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/**
 * Status bar options. SystemUI's tuner still reads both settings used here:
 * `clock_seconds` and `icon_blacklist` (the comma-separated list of hidden slots).
 */
class StatusBarFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(SwitchPreferenceCompat(context).apply {
            key = "clock_seconds"
            title = getString(R.string.statusbar_clock_seconds)
            summary = getString(R.string.statusbar_clock_seconds_summary)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver, GraphiteSettings.CLOCK_SECONDS, 0) == 1
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(
                    context.contentResolver, GraphiteSettings.CLOCK_SECONDS, if (value as Boolean) 1 else 0
                )
            }
        })

        val icons = PreferenceCategory(context).apply { title = getString(R.string.statusbar_icons) }
        screen.addPreference(icons)
        val hidden = StatusBarIcons.hiddenSlots(context)
        for (slot in StatusBarIcons.slots(context)) {
            icons.addPreference(SwitchPreferenceCompat(context).apply {
                key = "slot_$slot"
                title = StatusBarIcons.label(context, slot)
                icon = StatusBarIcons.icon(context, slot)?.tinted(context)
                isPersistent = false
                isChecked = slot !in hidden
                setOnPreferenceChangeListener { _, value ->
                    StatusBarIcons.setHidden(context, slot, !(value as Boolean))
                    true
                }
            })
        }
    }

    private fun Drawable.tinted(context: Context): Drawable = mutate().apply {
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorControlNormal))
        attrs.getColorStateList(0)?.let { setTintList(it) } ?: setTintList(ColorStateList.valueOf(0xFF808080.toInt()))
        attrs.recycle()
    }
}
