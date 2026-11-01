package org.graphite.parts

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarAppCompatActivity

/** Hosts one settings screen. The fragment class comes from [EXTRA_FRAGMENT], or the main screen. */
class GraphiteActivity : CollapsingToolbarAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = intent.getStringExtra(EXTRA_FRAGMENT)
                ?.let { supportFragmentManager.fragmentFactory.instantiate(classLoader, it) }
                ?: MainFragment()
            supportFragmentManager.beginTransaction()
                .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_FRAGMENT = "org.graphite.parts.FRAGMENT"
    }
}
