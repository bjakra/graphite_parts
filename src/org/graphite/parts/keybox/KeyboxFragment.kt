package org.graphite.parts.keybox

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R

/**
 * Play Integrity keybox entry. The keybox itself (import, turn on, delete) lives in GmsCompat's own
 * screen, next to the private key. This fragment adds the "apply to all apps" switch, which is the
 * per-user enabled state of GmsCompat's signer service (off in its manifest). GraphiteParts holds
 * CHANGE_COMPONENT_ENABLED_STATE; the framework hook only reads the state.
 */
class KeyboxFragment : PreferenceFragmentCompat() {

    // Keep in sync with PlayKeyboxHooks.SIGNER_SERVICE.
    private val signer = ComponentName("app.grapheneos.gmscompat",
        "app.grapheneos.gmscompat.keybox.KeyboxSignerService")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val pm = context.packageManager
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(Preference(context).apply {
            title = getString(R.string.keybox_manage_title)
            summary = getString(R.string.keybox_manage_summary)
            setOnPreferenceClickListener {
                startActivity(Intent().setClassName(
                    "app.grapheneos.gmscompat",
                    "app.grapheneos.gmscompat.keybox.KeyboxActivity"))
                true
            }
        })

        screen.addPreference(SwitchPreferenceCompat(context).apply {
            title = getString(R.string.keybox_all_apps_title)
            summary = getString(R.string.keybox_all_apps_summary)
            isPersistent = false
            isChecked = runCatching {
                pm.getComponentEnabledSetting(signer) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }.getOrDefault(false)
            setOnPreferenceChangeListener { _, value ->
                val state = if (value as Boolean) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                runCatching {
                    pm.setComponentEnabledSetting(signer, state, PackageManager.DONT_KILL_APP)
                }.isSuccess
            }
        })
    }
}
