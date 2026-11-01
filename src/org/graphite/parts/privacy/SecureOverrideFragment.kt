package org.graphite.parts.privacy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/**
 * Apps whose FLAG_SECURE windows may be captured by *system* screenshots. The apps keep
 * FLAG_SECURE for everything else (third-party capture, casting, recents thumbnails).
 */
class SecureOverrideFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(Preference(context).apply {
            summary = getString(R.string.secure_override_explanation)
            isSelectable = false
        })
        screen.addPreference(SwitchPreferenceCompat(context).apply {
            key = GraphiteSettings.SECURE_OVERRIDE_STEALTH
            title = getString(R.string.secure_override_stealth)
            summary = getString(R.string.secure_override_stealth_summary)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver, key, 0) == 1
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(context.contentResolver, key, if (value as Boolean) 1 else 0)
            }
        })

        val apps = PreferenceCategory(context).apply { title = getString(R.string.secure_override_apps) }
        screen.addPreference(apps)
        val selected = selectedPackages(context)
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { it to it.loadLabel(pm).toString() }
            .sortedBy { it.second.lowercase() }
            .forEach { (info, label) ->
                apps.addPreference(SwitchPreferenceCompat(context).apply {
                    key = "pkg_" + info.packageName
                    title = label
                    summary = info.packageName
                    icon = info.loadIcon(pm)
                    isPersistent = false
                    isChecked = info.packageName in selected
                    setOnPreferenceChangeListener { _, value ->
                        setSelected(context, info.packageName, value as Boolean)
                        true
                    }
                })
            }
    }

    private fun selectedPackages(context: Context): MutableSet<String> =
        Settings.Secure.getString(context.contentResolver, GraphiteSettings.SECURE_OVERRIDE_PACKAGES)
            ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableSet()
            ?: mutableSetOf()

    private fun setSelected(context: Context, packageName: String, selected: Boolean) {
        val packages = selectedPackages(context)
        if (selected) packages.add(packageName) else packages.remove(packageName)
        Settings.Secure.putString(
            context.contentResolver, GraphiteSettings.SECURE_OVERRIDE_PACKAGES,
            packages.sorted().joinToString(",").ifEmpty { null },
        )
    }
}
