package org.graphite.parts

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarAppCompatActivity

/**
 * Hosts one settings screen. The fragment class comes from [EXTRA_FRAGMENT], or the main screen.
 * Sub-screens open in a new activity so the collapsing toolbar shows their title.
 */
class GraphiteActivity : CollapsingToolbarAppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_TITLE)?.let { title = it }
        if (savedInstanceState == null) {
            // The activity is exported (Settings opens it), so only our own screens may be
            // requested; anything else would be fragment injection.
            val fragment = intent.getStringExtra(EXTRA_FRAGMENT)
                ?.takeIf { it in FRAGMENTS }
                ?.let { supportFragmentManager.fragmentFactory.instantiate(classLoader, it) }
                ?: MainFragment()
            supportFragmentManager.beginTransaction()
                .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment)
                .commit()
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        startActivity(
            Intent(this, GraphiteActivity::class.java)
                .putExtra(EXTRA_FRAGMENT, pref.fragment)
                .putExtra(EXTRA_TITLE, pref.title?.toString())
        )
        return true
    }

    companion object {
        const val EXTRA_FRAGMENT = "org.graphite.parts.FRAGMENT"
        const val EXTRA_TITLE = "org.graphite.parts.TITLE"

        private val FRAGMENTS = setOf(
            "org.graphite.parts.statusbar.StatusBarFragment",
            "org.graphite.parts.lockscreen.LockScreenFragment",
            "org.graphite.parts.powermenu.PowerMenuFragment",
            "org.graphite.parts.privacy.SecureOverrideFragment",
            "org.graphite.parts.nowplaying.NowPlayingFragment",
            "org.graphite.parts.nowplaying.NowPlayingSettingsFragment",
        )
    }
}
