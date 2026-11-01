package org.graphite.parts.caffeine

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.graphite.parts.R

/**
 * Quick Settings tile that keeps the screen on. Each tap moves to the next duration
 * (5 min → 10 min → 30 min → until turned off → off).
 */
class CaffeineTileService : TileService() {

    override fun onStartListening() = updateTile()

    override fun onClick() {
        val next = CaffeineService.nextDuration(CaffeineService.currentDuration)
        if (next == null) {
            CaffeineService.stop(this)
        } else {
            CaffeineService.start(this, next)
        }
        updateTile()
    }

    fun updateTile() {
        val tile = qsTile ?: return
        val duration = CaffeineService.currentDuration
        tile.state = if (duration == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.subtitle = when (duration) {
            null -> getString(R.string.caffeine_off)
            CaffeineService.INFINITE -> getString(R.string.caffeine_infinite)
            else -> getString(R.string.caffeine_minutes, duration / 60_000)
        }
        tile.updateTile()
    }
}
