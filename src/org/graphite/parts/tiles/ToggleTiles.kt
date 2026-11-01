package org.graphite.parts.tiles

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** A Quick Settings tile that flips one setting and follows its changes while visible. */
abstract class SettingToggleTile : TileService() {
    protected abstract val uri: Uri
    protected abstract fun isOn(): Boolean
    protected abstract fun setOn(on: Boolean)

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    override fun onStartListening() {
        contentResolver.registerContentObserver(uri, false, observer)
        refresh()
    }

    override fun onStopListening() = contentResolver.unregisterContentObserver(observer)

    override fun onClick() {
        setOn(!isOn())
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        tile.state = if (isOn()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}

/** Heads-up (pop-up) notifications on/off. */
class HeadsUpTile : SettingToggleTile() {
    override val uri: Uri = Settings.Global.getUriFor(KEY)
    override fun isOn() = Settings.Global.getInt(contentResolver, KEY, 1) == 1
    override fun setOn(on: Boolean) { Settings.Global.putInt(contentResolver, KEY, if (on) 1 else 0) }

    private companion object { const val KEY = "heads_up_notifications_enabled" }
}

/** Always-on display on/off. */
class AodTile : SettingToggleTile() {
    override val uri: Uri = Settings.Secure.getUriFor(KEY)
    override fun isOn() = Settings.Secure.getInt(contentResolver, KEY, 0) == 1
    override fun setOn(on: Boolean) { Settings.Secure.putInt(contentResolver, KEY, if (on) 1 else 0) }

    private companion object { const val KEY = "doze_always_on" }
}

/** Account auto-sync on/off. */
class SyncTile : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        ContentResolver.setMasterSyncAutomatically(!ContentResolver.getMasterSyncAutomatically())
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        tile.state = if (ContentResolver.getMasterSyncAutomatically()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
