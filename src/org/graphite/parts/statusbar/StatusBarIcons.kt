package org.graphite.parts.statusbar

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.provider.Settings
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/** Status bar slots, their real icons, and the hide list SystemUI reads. */
object StatusBarIcons {
    private const val SYSTEMUI = "com.android.systemui"

    /** Drawable candidates per slot, as "package:name". The first one that exists wins. */
    private val ICONS = mapOf(
        "airplane" to listOf("$SYSTEMUI:stat_sys_airplane_mode"),
        "alarm_clock" to listOf("$SYSTEMUI:stat_sys_alarm"),
        "battery" to listOf("android:ic_battery", "$SYSTEMUI:ic_battery_unknown"),
        "bluetooth" to listOf("$SYSTEMUI:stat_sys_data_bluetooth_connected"),
        "camera" to listOf("android:perm_group_camera"),
        "cast" to listOf("$SYSTEMUI:stat_sys_cast"),
        "clock" to listOf("android:ic_clock", "android:perm_group_calendar"),
        "connected_display" to listOf("$SYSTEMUI:stat_sys_connected_display"),
        "data_saver" to listOf("$SYSTEMUI:stat_sys_data_saver"),
        "ethernet" to listOf("$SYSTEMUI:stat_sys_ethernet"),
        "headset" to listOf("$SYSTEMUI:stat_sys_headset"),
        "hotspot" to listOf("$SYSTEMUI:stat_sys_hotspot"),
        "location" to listOf("$SYSTEMUI:ic_location", "android:perm_group_location"),
        "managed_profile" to listOf("android:stat_sys_managed_profile_status"),
        "microphone" to listOf("android:perm_group_microphone"),
        "mobile" to listOf("android:ic_signal_cellular_4_4_bar", "$SYSTEMUI:ic_qs_no_sim"),
        "mute" to listOf("$SYSTEMUI:stat_sys_ringer_silent"),
        "nfc" to listOf("$SYSTEMUI:ic_qs_nfc"),
        "rotate" to listOf("$SYSTEMUI:stat_sys_rotate_portrait"),
        "screen_record" to listOf("$SYSTEMUI:stat_sys_screen_record"),
        "sensors_off" to listOf("$SYSTEMUI:stat_sys_sensors_off"),
        "sync_active" to listOf("android:stat_notify_sync"),
        "sync_failing" to listOf("android:stat_notify_sync_error"),
        "tty" to listOf("$SYSTEMUI:stat_sys_tty_mode"),
        "volume" to listOf("$SYSTEMUI:stat_sys_ringer_vibrate"),
        "vpn" to listOf("$SYSTEMUI:stat_sys_vpn_ic"),
        "wifi" to listOf("android:ic_wifi_signal_4"),
        "zen" to listOf("$SYSTEMUI:stat_sys_dnd"),
    )

    private val LABELS = mapOf(
        "airplane" to R.string.slot_airplane,
        "alarm_clock" to R.string.slot_alarm_clock,
        "battery" to R.string.slot_battery,
        "bluetooth" to R.string.slot_bluetooth,
        "camera" to R.string.slot_camera,
        "cast" to R.string.slot_cast,
        "clock" to R.string.slot_clock,
        "connected_display" to R.string.slot_connected_display,
        "data_saver" to R.string.slot_data_saver,
        "ethernet" to R.string.slot_ethernet,
        "headset" to R.string.slot_headset,
        "hotspot" to R.string.slot_hotspot,
        "location" to R.string.slot_location,
        "managed_profile" to R.string.slot_managed_profile,
        "microphone" to R.string.slot_microphone,
        "mobile" to R.string.slot_mobile,
        "mute" to R.string.slot_mute,
        "nfc" to R.string.slot_nfc,
        "rotate" to R.string.slot_rotate,
        "screen_record" to R.string.slot_screen_record,
        "sensors_off" to R.string.slot_sensors_off,
        "sync_active" to R.string.slot_sync_active,
        "sync_failing" to R.string.slot_sync_failing,
        "tty" to R.string.slot_tty,
        "volume" to R.string.slot_volume,
        "vpn" to R.string.slot_vpn,
        "wifi" to R.string.slot_wifi,
        "zen" to R.string.slot_zen,
    )

    /** Slots that are obsolete or never shown on Pixels; listing them would only confuse. */
    private val HIDDEN_FROM_LIST = setOf(
        "cdma_eri", "data_connection", "phone_evdo_signal", "phone_signal", "stacked_mobile",
        "call_strength", "no_calling", "secure", "ime", "speakerphone",
    )

    /** The slots SystemUI knows (framework config_statusBarIcons) plus the clock. */
    fun slots(context: Context): List<String> {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_statusBarIcons", "array", "android")
        val all = if (id != 0) res.getStringArray(id).toList() else ICONS.keys.toList()
        return (all + "clock").filter { it !in HIDDEN_FROM_LIST }.distinct()
            .sortedBy { label(context, it).lowercase() }
    }

    fun label(context: Context, slot: String): String =
        LABELS[slot]?.let { context.getString(it) }
            ?: slot.replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun icon(context: Context, slot: String): Drawable? {
        for (candidate in ICONS[slot].orEmpty()) {
            val (pkg, name) = candidate.split(':', limit = 2)
            val res = try {
                if (pkg == "android") Resources.getSystem()
                else context.packageManager.getResourcesForApplication(pkg)
            } catch (e: Exception) {
                continue
            }
            val id = res.getIdentifier(name, "drawable", pkg)
            if (id != 0) {
                try {
                    return res.getDrawable(id, null)
                } catch (e: Resources.NotFoundException) {
                    // try the next candidate
                }
            }
        }
        return context.getDrawable(R.drawable.ic_slot_generic)
    }

    /** The currently hidden slots. Unset means SystemUI's default exclude list applies. */
    fun hiddenSlots(context: Context): MutableSet<String> {
        val value = Settings.Secure.getString(context.contentResolver, GraphiteSettings.ICON_BLACKLIST)
        if (value != null) return value.split(',').filter { it.isNotBlank() }.toMutableSet()
        return try {
            val res = context.packageManager.getResourcesForApplication(SYSTEMUI)
            val id = res.getIdentifier("config_statusBarIconsToExclude", "array", SYSTEMUI)
            if (id != 0) res.getStringArray(id).toMutableSet() else mutableSetOf()
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    fun setHidden(context: Context, slot: String, hide: Boolean) {
        val hidden = hiddenSlots(context)
        if (hide) hidden.add(slot) else hidden.remove(slot)
        Settings.Secure.putString(context.contentResolver, GraphiteSettings.ICON_BLACKLIST, hidden.sorted().joinToString(","))
    }
}
