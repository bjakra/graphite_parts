package org.graphite.parts.caffeine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import org.graphite.parts.R

/** Holds a screen wake lock for the chosen duration and shows an ongoing notification. */
class CaffeineService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private val stopRunnable = Runnable { stopSelf() }

    // Turning the screen off by hand ends caffeine, like on LineageOS
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val duration = intent?.getLongExtra(EXTRA_DURATION, INFINITE) ?: INFINITE
        currentDuration = duration
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        if (wakeLock == null) {
            @Suppress("DEPRECATION")
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Graphite:Caffeine")
                .apply { setReferenceCounted(false) }
        }
        handler.removeCallbacks(stopRunnable)
        if (duration == INFINITE) {
            wakeLock?.acquire()
        } else {
            wakeLock?.acquire(duration)
            handler.postDelayed(stopRunnable, duration)
        }
        requestTileUpdate()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        unregisterReceiver(screenOffReceiver)
        wakeLock?.takeIf { it.isHeld }?.release()
        currentDuration = null
        requestTileUpdate()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.caffeine_title), NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 0, Intent(this, CaffeineService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_caffeine)
            .setContentTitle(getString(R.string.caffeine_title))
            .setContentText(getString(R.string.caffeine_notification))
            .setOngoing(true)
            .setContentIntent(stop)
            .build()
    }

    private fun requestTileUpdate() =
        TileService.requestListeningState(this, ComponentName(this, CaffeineTileService::class.java))

    companion object {
        const val INFINITE = -1L
        private const val EXTRA_DURATION = "duration"
        private const val ACTION_STOP = "org.graphite.parts.caffeine.STOP"
        private const val CHANNEL = "caffeine"
        private const val NOTIFICATION_ID = 1

        private val DURATIONS = listOf(5 * 60_000L, 10 * 60_000L, 30 * 60_000L, INFINITE)

        /** Active duration in ms ([INFINITE] for no limit), or null when off. */
        @Volatile var currentDuration: Long? = null
            private set

        fun nextDuration(current: Long?): Long? {
            if (current == null) return DURATIONS.first()
            val i = DURATIONS.indexOf(current)
            return if (i in 0 until DURATIONS.lastIndex) DURATIONS[i + 1] else null
        }

        fun start(context: Context, duration: Long) {
            context.startForegroundService(
                Intent(context, CaffeineService::class.java).putExtra(EXTRA_DURATION, duration)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CaffeineService::class.java))
        }
    }
}
