package org.graphite.parts.keybox

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.content.pm.PackageManager
import android.ext.settings.app.AswUseExecSpawning
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R
import org.graphite.parts.showConfirmCountdown

/**
 * Play Integrity settings. The keybox (import, turn on, delete) lives in GmsCompat's own screen;
 * this fragment adds the system-level switches GraphiteParts is privileged to set: the keybox "apply
 * to all apps" switch, and the "strong integrity native hook" switch, which turns off exec-spawning
 * for sandboxed Google Play services and turns on the bundled Zygisk modules (ReZygisk + Play
 * Integrity Fork) so they can hook DroidGuard. The latter disables a GrapheneOS exploit mitigation
 * for that one process, so it is gated behind a warning with a countdown. Once it is on, the Play
 * Integrity Fork submenu ([PifFragment]) appears.
 */
class KeyboxFragment : PreferenceFragmentCompat() {

    // Keep in sync with PlayKeyboxHooks.SIGNER_SERVICE.
    private val signer = ComponentName("app.grapheneos.gmscompat",
        "app.grapheneos.gmscompat.keybox.KeyboxSignerService")

    private val gmsPackage = "com.google.android.gms"

    private lateinit var nativeHookPref: SwitchPreferenceCompat
    private lateinit var pifPref: Preference

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

        nativeHookPref = SwitchPreferenceCompat(context).apply {
            title = getString(R.string.native_hook_title)
            summary = getString(R.string.native_hook_summary)
            isPersistent = false
            isChecked = isNativeHookOn()
            setOnPreferenceChangeListener { _, value ->
                if (value as Boolean) {
                    // Don't flip yet; confirm through the countdown dialog first.
                    confirmEnableNativeHook()
                    false
                } else {
                    setExecSpawning(gmsEnabled = true) // restore the default (exec-spawning on)
                    setModulesEnabled(false)
                    pifPref.isVisible = false
                    true
                }
            }
        }
        screen.addPreference(nativeHookPref)

        pifPref = Preference(context).apply {
            title = getString(R.string.pif_title)
            summary = getString(R.string.pif_summary)
            fragment = PifFragment::class.java.name
            isVisible = nativeHookPref.isChecked
        }
        screen.addPreference(pifPref)
    }

    override fun onResume() {
        super.onResume()
        if (::nativeHookPref.isInitialized) {
            nativeHookPref.isChecked = isNativeHookOn()
            pifPref.isVisible = nativeHookPref.isChecked
        }
    }

    /** The hook is "on" when exec-spawning is turned off for Google Play services. */
    private fun isNativeHookOn(): Boolean {
        val ctx = preferenceManager.context
        return runCatching {
            val appInfo = gmsAppInfo() ?: return false
            val ps = GosPackageState.get(gmsPackage, ctx.userId)
            !AswUseExecSpawning.I.get(ctx, ctx.userId, appInfo, ps)
        }.getOrDefault(false)
    }

    private fun confirmEnableNativeHook() {
        showConfirmCountdown(preferenceManager.context, R.string.native_hook_warn_title,
            R.string.native_hook_warn_message, R.string.native_hook_warn_confirm) {
            setExecSpawning(gmsEnabled = false) // turn exec-spawning OFF for GMS
            nativeHookPref.isChecked = isNativeHookOn()
            setModulesEnabled(nativeHookPref.isChecked)
            pifPref.isVisible = nativeHookPref.isChecked
        }
    }

    private fun gmsAppInfo(): ApplicationInfo? = runCatching {
        preferenceManager.context.packageManager.getApplicationInfo(gmsPackage, 0)
    }.getOrNull()

    /**
     * Sets exec-spawning for Google Play services. gmsEnabled = true restores the GrapheneOS default
     * (exec-spawning on, hook off); false turns it off (hook on). GMS is restarted so the change
     * takes effect immediately.
     */
    private fun setExecSpawning(gmsEnabled: Boolean) {
        val ctx = preferenceManager.context
        runCatching {
            val editor = GosPackageState.edit(gmsPackage, ctx.userId)
            if (gmsEnabled) {
                AswUseExecSpawning.I.setUseDefaultValue(editor)
            } else {
                AswUseExecSpawning.I.set(editor, false)
            }
            editor.killUidAfterApply()
            editor.apply()
        }
    }

    /** Tells the root controller (graphite-pi.sh) to install + enable, or disable, the modules. */
    private fun setModulesEnabled(enabled: Boolean) {
        Settings.Secure.putInt(preferenceManager.context.contentResolver,
            GraphiteSettings.PIF_ENABLED, if (enabled) 1 else 0)
    }
}
