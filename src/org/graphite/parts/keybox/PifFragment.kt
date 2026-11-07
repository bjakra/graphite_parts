package org.graphite.parts.keybox

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/**
 * Play Integrity Fork controls, reached from the Play Integrity screen once the native hook is on.
 * GraphiteParts isn't root, so everything goes through Settings.Secure: this screen writes the
 * graphite_pif_* keys, and the root controller (vendor/graphite/prebuilt/PlayIntegrity/graphite-pi.sh)
 * applies them to the module and writes its status back, which is shown live here.
 */
class PifFragment : PreferenceFragmentCompat() {

    private lateinit var statusPref: Preference
    private lateinit var fingerprintPref: Preference

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    // Graphite key -> (title, PIF's default). The controller copies them into custom.pif.prop.
    private val advanced = listOf(
        Triple("graphite_pif_spoof_build", R.string.pif_spoof_build_title, true),
        Triple("graphite_pif_spoof_props", R.string.pif_spoof_props_title, true),
        Triple("graphite_pif_spoof_provider", R.string.pif_spoof_provider_title, true),
        Triple("graphite_pif_spoof_signature", R.string.pif_spoof_signature_title, false),
        Triple("graphite_pif_spoof_vending_finger", R.string.pif_spoof_vending_finger_title, false),
        Triple("graphite_pif_spoof_vending_sdk", R.string.pif_spoof_vending_sdk_title, false),
        Triple("graphite_pif_verbose", R.string.pif_verbose_title, false),
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val cr = context.contentResolver
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        statusPref = Preference(context).apply {
            title = getString(R.string.pif_status_title)
            isSelectable = false
        }
        screen.addPreference(statusPref)

        fingerprintPref = Preference(context).apply {
            title = getString(R.string.pif_fingerprint_title)
            isSelectable = false
        }
        screen.addPreference(fingerprintPref)

        screen.addPreference(requestPref(R.string.pif_fingerprint_update_title,
            R.string.pif_fingerprint_update_summary, "fingerprint"))
        screen.addPreference(requestPref(R.string.pif_update_now_title,
            R.string.pif_update_now_summary, "update"))

        screen.addPreference(SwitchPreferenceCompat(context).apply {
            title = getString(R.string.pif_auto_update_title)
            summary = getString(R.string.pif_auto_update_summary)
            isPersistent = false
            isChecked = Settings.Secure.getInt(cr, GraphiteSettings.PIF_AUTO_UPDATE, 1) == 1
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(cr, GraphiteSettings.PIF_AUTO_UPDATE, if (value as Boolean) 1 else 0)
            }
        })

        val category = PreferenceCategory(context).apply { title = getString(R.string.pif_advanced_title) }
        screen.addPreference(category)
        for ((key, titleRes, default) in advanced) {
            category.addPreference(SwitchPreferenceCompat(context).apply {
                title = getString(titleRes)
                isPersistent = false
                isChecked = Settings.Secure.getInt(cr, key, if (default) 1 else 0) == 1
                setOnPreferenceChangeListener { _, value ->
                    Settings.Secure.putInt(cr, key, if (value as Boolean) 1 else 0)
                }
            })
        }
    }

    private fun requestPref(titleRes: Int, summaryRes: Int, request: String) =
        Preference(preferenceManager.context).apply {
            title = getString(titleRes)
            summary = getString(summaryRes)
            setOnPreferenceClickListener {
                val cr = context.contentResolver
                Settings.Secure.putString(cr, GraphiteSettings.PIF_REQUEST, request)
                Settings.Secure.putString(cr, GraphiteSettings.PIF_STATUS, getString(R.string.pif_requested))
                true
            }
        }

    override fun onStart() {
        super.onStart()
        val cr = requireContext().contentResolver
        for (key in listOf(GraphiteSettings.PIF_STATUS, GraphiteSettings.PIF_FINGERPRINT_INFO)) {
            cr.registerContentObserver(Settings.Secure.getUriFor(key), false, observer)
        }
        refresh()
    }

    override fun onStop() {
        requireContext().contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }

    private fun refresh() {
        val cr = context?.contentResolver ?: return
        statusPref.summary = Settings.Secure.getString(cr, GraphiteSettings.PIF_STATUS)
            ?.takeIf { it.isNotBlank() } ?: getString(R.string.pif_status_waiting)
        fingerprintPref.summary = Settings.Secure.getString(cr, GraphiteSettings.PIF_FINGERPRINT_INFO)
            ?.takeIf { it.isNotBlank() } ?: getString(R.string.pif_fingerprint_none)
    }
}
