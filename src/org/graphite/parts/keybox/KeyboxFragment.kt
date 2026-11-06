package org.graphite.parts.keybox

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.content.pm.PackageManager
import android.ext.settings.app.AswUseExecSpawning
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R

/**
 * Play Integrity settings. The keybox (import, turn on, delete) and the device-fingerprint controls
 * live in GmsCompat's own screen; this fragment adds the system-level switches GraphiteParts is
 * privileged to set: the keybox "apply to all apps" switch, and the "strong integrity native hook"
 * switch, which turns off exec-spawning for sandboxed Google Play services so a Zygisk module
 * (bundled ReZygisk + Play Integrity Fork) can hook DroidGuard. The latter disables a GrapheneOS
 * exploit mitigation for that one process, so it is gated behind a warning with a countdown.
 */
class KeyboxFragment : PreferenceFragmentCompat() {

    // Keep in sync with PlayKeyboxHooks.SIGNER_SERVICE.
    private val signer = ComponentName("app.grapheneos.gmscompat",
        "app.grapheneos.gmscompat.keybox.KeyboxSignerService")

    private val gmsPackage = "com.google.android.gms"

    private lateinit var nativeHookPref: SwitchPreferenceCompat

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
                    true
                }
            }
        }
        screen.addPreference(nativeHookPref)
    }

    override fun onResume() {
        super.onResume()
        if (::nativeHookPref.isInitialized) {
            nativeHookPref.isChecked = isNativeHookOn()
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
        val context = preferenceManager.context
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.native_hook_warn_title)
            .setMessage(R.string.native_hook_warn_message)
            .setPositiveButton(R.string.native_hook_warn_confirm) { _, _ ->
                setExecSpawning(gmsEnabled = false) // turn exec-spawning OFF for GMS
                nativeHookPref.isChecked = isNativeHookOn()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()

        // Keep the confirm button disabled for a 10 s countdown so the choice is deliberate.
        val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val label = getString(R.string.native_hook_warn_confirm)
        val handler = Handler(Looper.getMainLooper())
        var remaining = 10
        confirm.isEnabled = false
        val tick = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) return
                if (remaining > 0) {
                    confirm.text = "$label ($remaining)"
                    remaining--
                    handler.postDelayed(this, 1000)
                } else {
                    confirm.text = label
                    confirm.isEnabled = true
                }
            }
        }
        handler.post(tick)
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
}
