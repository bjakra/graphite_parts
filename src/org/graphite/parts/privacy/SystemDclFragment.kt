package org.graphite.parts.privacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R
import org.graphite.parts.showConfirmCountdown

/**
 * System app memory code loading (patch 0655). Off keeps GrapheneOS's immutable restriction. On gives
 * system apps the normal per-app "Dynamic code loading via memory" switch, which LSPosed/Vector's
 * manager in Shell and modules scoped to system apps need. It weakens an exploit protection, so turning
 * it on is gated behind a warning with a countdown.
 */
class SystemDclFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val cr = context.contentResolver
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(Preference(context).apply {
            summary = getString(R.string.system_dcl_explanation)
            isSelectable = false
        })

        val shell = Preference(context).apply {
            title = getString(R.string.system_dcl_shell_title)
            summary = getString(R.string.system_dcl_shell_summary)
            setOnPreferenceClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:com.android.shell")))
                true
            }
        }

        val switch = SwitchPreferenceCompat(context).apply {
            title = getString(R.string.system_dcl_switch_title)
            summary = getString(R.string.system_dcl_switch_summary)
            isPersistent = false
            isChecked = Settings.Global.getInt(cr, GraphiteSettings.SYSTEM_APP_MEMORY_DCL, 0) == 1
        }
        switch.setOnPreferenceChangeListener { _, value ->
            if (value as Boolean) {
                showConfirmCountdown(context, R.string.system_dcl_warn_title,
                    R.string.system_dcl_warn_message, R.string.system_dcl_warn_confirm) {
                    if (Settings.Global.putInt(cr, GraphiteSettings.SYSTEM_APP_MEMORY_DCL, 1)) {
                        switch.isChecked = true
                        shell.isVisible = true
                    }
                }
                false
            } else {
                shell.isVisible = false
                Settings.Global.putInt(cr, GraphiteSettings.SYSTEM_APP_MEMORY_DCL, 0)
            }
        }
        shell.isVisible = switch.isChecked
        screen.addPreference(switch)
        screen.addPreference(shell)
    }
}
