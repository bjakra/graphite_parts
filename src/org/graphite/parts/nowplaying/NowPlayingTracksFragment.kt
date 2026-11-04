package org.graphite.parts.nowplaying

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.graphite.parts.R
import java.text.NumberFormat

/**
 * The songs in the music database, as Ambient Music Mod's track list: search by title or artist. Graphite Now Playing
 * searches AMM's cached list of the installed database; while it builds that list the progress is shown.
 */
class NowPlayingTracksFragment : PreferenceFragmentCompat() {

    private lateinit var search: EditTextPreference
    private lateinit var results: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen
        search = EditTextPreference(context).apply {
            // Dialog preferences need a key: the dialog finds its preference by it when recreated
            key = "tracks_search"
            title = getString(R.string.nowplaying_tracks_search)
            dialogTitle = title
            isPersistent = false
            summary = getString(R.string.nowplaying_tracks_search_summary)
            setOnPreferenceChangeListener { pref, value ->
                val query = (value as String).trim()
                pref.summary = query.ifEmpty { getString(R.string.nowplaying_tracks_search_summary) }
                load(query)
                true
            }
        }
        results = PreferenceCategory(context)
        screen.addPreference(search)
        screen.addPreference(results)
        load("")
    }

    private fun load(query: String) {
        val context = context ?: return
        results.title = getString(R.string.nowplaying_tracks_loading)
        NowPlayingApi.load({ NowPlayingApi.tracks(context, query) }) { result ->
            if (!isAdded || result == null) return@load
            results.removeAll()
            if (result.getBoolean("loading")) {
                val of = result.getInt("of")
                results.title = if (of > 0) {
                    getString(R.string.nowplaying_tracks_loading_progress, result.getInt("current"), of)
                } else {
                    getString(R.string.nowplaying_tracks_loading)
                }
                view?.postDelayed({ load(query) }, 1500)
                return@load
            }
            val format = NumberFormat.getIntegerInstance()
            val matches = result.getInt("matches")
            val tracks = result.getStringArray("tracks").orEmpty()
            val artists = result.getStringArray("artists").orEmpty()
            results.title = if (query.isEmpty()) {
                getString(R.string.nowplaying_tracks_total, format.format(result.getInt("total")))
            } else {
                getString(R.string.nowplaying_tracks_matches, format.format(matches))
            }
            tracks.forEachIndexed { i, track ->
                results.addPreference(Preference(preferenceManager.context).apply {
                    title = track
                    summary = artists.getOrNull(i)
                    isSelectable = false
                })
            }
            if (matches > tracks.size) {
                results.addPreference(Preference(preferenceManager.context).apply {
                    summary = getString(R.string.nowplaying_tracks_more, format.format(matches - tracks.size))
                    isSelectable = false
                })
            }
        }
    }
}
