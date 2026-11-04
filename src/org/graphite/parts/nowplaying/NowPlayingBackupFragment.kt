package org.graphite.parts.nowplaying

import android.app.AlertDialog
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Backup and restore, as Ambient Music Mod's screen: back up the history, favourites and settings to a file, restore
 * one (AMM's own backups too) with AMM's options, and periodic backups to a folder. Graphite Now Playing reads and
 * writes the files; this page picks them and gives it access to just those.
 */
class NowPlayingBackupFragment : PreferenceFragmentCompat() {

    private lateinit var status: Preference

    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri ?: return@registerForActivityResult
        grant(uri, persistable = false)
        NowPlayingApi.backup(requireContext(), uri)
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        grant(uri, persistable = false)
        askRestoreOptions(uri)
    }

    private val chooseFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        grant(uri, persistable = true)
        NowPlayingApi.setBackupFolder(requireContext(), uri)
    }

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refreshStatus()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen
        screen.addPreference(Preference(context).apply {
            title = getString(R.string.nowplaying_backup_now)
            summary = getString(R.string.nowplaying_backup_now_summary)
            setOnPreferenceClickListener {
                // AMM's file name, so its backups and Graphite's look the same
                val time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
                createBackup.launch("amm_backup_$time.ammbkp")
                true
            }
        })
        screen.addPreference(Preference(context).apply {
            title = getString(R.string.nowplaying_restore)
            summary = getString(R.string.nowplaying_restore_summary)
            setOnPreferenceClickListener {
                openBackup.launch(arrayOf("*/*"))
                true
            }
        })
        status = Preference(context).apply {
            isSelectable = false
            isVisible = false
        }
        screen.addPreference(status)
        NowPlayingApi.load({ NowPlayingApi.settings(context) }) { settings ->
            if (isAdded && settings != null) buildPeriodic(settings)
        }
    }

    private fun buildPeriodic(settings: Bundle) {
        val context = preferenceManager.context
        val enabled = settings.getBundle("periodicBackupEnabled") ?: return
        val interval = settings.getBundle("periodicBackupInterval")
        val folder = Preference(context).apply {
            title = getString(R.string.nowplaying_backup_folder)
            summary = getString(R.string.nowplaying_backup_folder_summary)
            isEnabled = enabled.getBoolean("value")
            setOnPreferenceClickListener {
                chooseFolder.launch(null)
                true
            }
        }
        val intervalPref = interval?.let { setting ->
            ListPreference(context).apply {
                key = "backup_interval"
                title = getString(R.string.nowplaying_backup_interval)
                dialogTitle = title
                isPersistent = false
                entries = setting.getStringArray("optionTitles")
                entryValues = setting.getStringArray("options")
                value = setting.getString("value")
                summary = entry
                isEnabled = enabled.getBoolean("value")
                setOnPreferenceChangeListener { pref, newValue ->
                    pref.summary = entries.getOrNull(entryValues.indexOf(newValue))
                    NowPlayingApi.set(context, "periodicBackupInterval", newValue)
                    true
                }
            }
        }
        preferenceScreen.addPreference(SwitchPreferenceCompat(context).apply {
            title = getString(R.string.nowplaying_backup_periodic)
            summary = getString(R.string.nowplaying_backup_periodic_summary)
            isPersistent = false
            isChecked = enabled.getBoolean("value")
            setOnPreferenceChangeListener { _, value ->
                val on = value as Boolean
                folder.isEnabled = on
                intervalPref?.isEnabled = on
                NowPlayingApi.set(context, "periodicBackupEnabled", on)
                true
            }
        })
        intervalPref?.let(preferenceScreen::addPreference)
        preferenceScreen.addPreference(folder)
    }

    /** AMM's restore options: what to restore, and whether to clear what's there first */
    private fun askRestoreOptions(uri: Uri) {
        val context = requireContext()
        val keys = arrayOf("restoreHistory", "clearHistory", "restoreFavourites", "clearFavourites", "restoreSettings")
        val labels = arrayOf(
            R.string.nowplaying_restore_history, R.string.nowplaying_restore_clear_history,
            R.string.nowplaying_restore_favourites, R.string.nowplaying_restore_clear_favourites,
            R.string.nowplaying_restore_settings,
        ).map { getString(it) }.toTypedArray()
        val checked = booleanArrayOf(true, false, true, false, true)
        AlertDialog.Builder(context)
            .setTitle(R.string.nowplaying_restore)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton(R.string.nowplaying_restore_start) { _, _ ->
                val options = Bundle().apply { keys.forEachIndexed { i, key -> putBoolean(key, checked[i]) } }
                NowPlayingApi.restore(context, uri, options)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun grant(uri: Uri, persistable: Boolean) {
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (persistable) flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        requireContext().grantUriPermission(NowPlayingApi.CORE_PACKAGE, uri, flags)
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver.registerContentObserver(NowPlayingApi.URI_BACKUP, false, observer)
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(observer)
    }

    private fun refreshStatus() {
        val context = context ?: return
        NowPlayingApi.load({ NowPlayingApi.backupState(context) }) { state ->
            if (!isAdded || state == null) return@load
            status.isVisible = state.getString("text") != null
            status.title = state.getString("text")
        }
    }
}
