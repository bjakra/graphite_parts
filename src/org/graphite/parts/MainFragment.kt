package org.graphite.parts

import android.os.Bundle
import android.os.SystemProperties
import androidx.preference.PreferenceFragmentCompat

class MainFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.graphite_main, rootKey)
        findPreference<androidx.preference.Preference>("about")?.summary =
            SystemProperties.get("ro.build.version.incremental") +
                " · GrapheneOS " + SystemProperties.get("ro.build.id")
    }
}
