package org.graphite.parts

/** Settings keys shared by Graphite's features. They live in Settings.Secure unless noted. */
object GraphiteSettings {
    // Read by SystemUI's tuner (AOSP keys)
    const val CLOCK_SECONDS = "clock_seconds"
    const val ICON_BLACKLIST = "icon_blacklist"

    // Read by Graphite's SystemUI patches
    const val CHARGING_INFO = "graphite_charging_info"
    /** Default on; read by LockscreenSmartspaceGeneralView (patch 0610) */
    const val LOCKSCREEN_AT_A_GLANCE = "graphite_lockscreen_at_a_glance"
    /** Default on; read by LockscreenSmartspaceGeneralView (patch 0640) */
    const val AOD_AT_A_GLANCE = "graphite_aod_at_a_glance"
    const val CLOCK_AM_PM = "graphite_clock_am_pm"
    const val NETWORK_TRAFFIC = "graphite_network_traffic"
    const val DOUBLE_TAP_TO_SLEEP = "double_tap_to_sleep"
    const val DOUBLE_TAP_TO_WAKE = "graphite_double_tap_to_wake"
    const val HIDE_NOTIFICATION_ICONS = "graphite_hide_notification_icons"
    const val POWER_MENU_RESTART_SUBMENU = "graphite_power_menu_restart_submenu"
    const val POWER_MENU_ACTIONS = "graphite_power_menu_actions"
    const val SECURE_OVERRIDE_PACKAGES = "graphite_secure_override_packages"
    const val SECURE_OVERRIDE_STEALTH = "graphite_secure_override_stealth"

    /** Settings.Global, read by AswRestrictMemoryDynCodeLoading (patch 0655). Default off. */
    const val SYSTEM_APP_MEMORY_DCL = "graphite_system_app_memory_dcl"

    // Play Integrity Fork, read and written back by vendor/graphite/prebuilt/PlayIntegrity/graphite-pi.sh (root)
    /** 1 = install and enable the bundled Zygisk modules, 0 = disable them. Follows the native hook switch. */
    const val PIF_ENABLED = "graphite_pif_enabled"
    /** Default on */
    const val PIF_AUTO_UPDATE = "graphite_pif_auto_update"
    /** "fingerprint" or "update"; the controller clears it once taken */
    const val PIF_REQUEST = "graphite_pif_request"
    /** Written by the controller */
    const val PIF_STATUS = "graphite_pif_status"
    const val PIF_FINGERPRINT_INFO = "graphite_pif_fingerprint_info"
}
