package org.graphite.parts

/** Settings keys shared by Graphite's features. All live in Settings.Secure. */
object GraphiteSettings {
    // Read by SystemUI's tuner (AOSP keys)
    const val CLOCK_SECONDS = "clock_seconds"
    const val ICON_BLACKLIST = "icon_blacklist"

    // Read by Graphite's SystemUI patches
    const val CHARGING_INFO = "graphite_charging_info"
    const val CLOCK_AM_PM = "graphite_clock_am_pm"
    const val NETWORK_TRAFFIC = "graphite_network_traffic"
    const val DOUBLE_TAP_TO_SLEEP = "double_tap_to_sleep"
    const val DOUBLE_TAP_TO_WAKE = "graphite_double_tap_to_wake"
    const val HIDE_NOTIFICATION_ICONS = "graphite_hide_notification_icons"
    const val POWER_MENU_RESTART_SUBMENU = "graphite_power_menu_restart_submenu"
    const val POWER_MENU_ACTIONS = "graphite_power_menu_actions"
    const val SECURE_OVERRIDE_PACKAGES = "graphite_secure_override_packages"
    const val SECURE_OVERRIDE_STEALTH = "graphite_secure_override_stealth"
}
