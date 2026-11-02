package org.graphite.parts.statusbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/**
 * Status bar options. SystemUI's tuner still reads both settings used here:
 * `clock_seconds` and `icon_blacklist` (the comma-separated list of hidden slots).
 */
class StatusBarFragment : PreferenceFragmentCompat() {

    private fun secureSwitch(setting: String, titleRes: Int, summaryRes: Int) =
        SwitchPreferenceCompat(preferenceManager.context).apply {
            title = getString(titleRes)
            summary = getString(summaryRes)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver, setting, 0) != 0
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(context.contentResolver, setting, if (value as Boolean) 1 else 0)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(SwitchPreferenceCompat(context).apply {
            key = "clock_seconds"
            title = getString(R.string.statusbar_clock_seconds)
            summary = getString(R.string.statusbar_clock_seconds_summary)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver, GraphiteSettings.CLOCK_SECONDS, 0) == 1
            setOnPreferenceChangeListener { _, value ->
                Settings.Secure.putInt(
                    context.contentResolver, GraphiteSettings.CLOCK_SECONDS, if (value as Boolean) 1 else 0
                )
            }
        })

        screen.addPreference(secureSwitch(GraphiteSettings.CLOCK_AM_PM,
            R.string.statusbar_clock_am_pm, R.string.statusbar_clock_am_pm_summary))
        screen.addPreference(secureSwitch(GraphiteSettings.NETWORK_TRAFFIC,
            R.string.statusbar_network_traffic, R.string.statusbar_network_traffic_summary))
        screen.addPreference(secureSwitch(GraphiteSettings.HIDE_NOTIFICATION_ICONS,
            R.string.statusbar_hide_notification_icons, R.string.statusbar_hide_notification_icons_summary))
        screen.addPreference(SwitchPreferenceCompat(context).apply {
            title = getString(R.string.statusbar_battery_percent)
            isPersistent = false
            isChecked = Settings.System.getInt(
                context.contentResolver, Settings.System.SHOW_BATTERY_PERCENT, 0) != 0
            setOnPreferenceChangeListener { _, value ->
                Settings.System.putInt(context.contentResolver,
                    Settings.System.SHOW_BATTERY_PERCENT, if (value as Boolean) 1 else 0)
            }
        })

        val icons = PreferenceCategory(context).apply { title = getString(R.string.statusbar_icons) }
        screen.addPreference(icons)
        val hidden = StatusBarIcons.hiddenSlots(context)
        for (slot in StatusBarIcons.slots(context)) {
            icons.addPreference(SwitchPreferenceCompat(context).apply {
                key = "slot_$slot"
                title = StatusBarIcons.label(context, slot)
                icon = StatusBarIcons.icon(context, slot)?.uniform(context)
                isPersistent = false
                isChecked = slot !in hidden
                setOnPreferenceChangeListener { _, value ->
                    StatusBarIcons.setHidden(context, slot, !(value as Boolean))
                    true
                }
            })
        }
    }

    /**
     * Renders the icon into a fixed box. SystemUI's icons differ in intrinsic size, aspect ratio and
     * empty margins, so this crops to the visible pixels first and then fits them to the same size.
     */
    private fun Drawable.uniform(context: Context): Drawable {
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val tint = attrs.getColorStateList(0) ?: ColorStateList.valueOf(Color.GRAY)
        attrs.recycle()
        val source = mutate().apply { setTintList(tint) }

        val workSize = 192
        val work = Bitmap.createBitmap(workSize, workSize, Bitmap.Config.ARGB_8888)
        val w = source.intrinsicWidth.takeIf { it > 0 } ?: workSize
        val h = source.intrinsicHeight.takeIf { it > 0 } ?: workSize
        val scale = workSize.toFloat() / maxOf(w, h)
        val left = ((workSize - w * scale) / 2).toInt()
        val top = ((workSize - h * scale) / 2).toInt()
        source.setBounds(left, top, left + (w * scale).toInt(), top + (h * scale).toInt())
        source.draw(Canvas(work))

        val visible = visibleBounds(work) ?: return source
        val box = (context.resources.displayMetrics.density * ICON_BOX_DP).toInt()
        val inner = box * ICON_FILL
        val fit = inner / maxOf(visible.width(), visible.height())
        val outW = visible.width() * fit
        val outH = visible.height() * fit
        val destLeft = (box - outW) / 2
        val destTop = (box - outH) / 2
        val out = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            work, visible, RectF(destLeft, destTop, destLeft + outW, destTop + outH),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        work.recycle()
        return BitmapDrawable(context.resources, out)
    }

    private fun visibleBounds(bitmap: Bitmap): Rect? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(pixels[y * bitmap.width + x]) > 16) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        return if (right < 0) null else Rect(left, top, right + 1, bottom + 1)
    }

    private companion object {
        const val ICON_BOX_DP = 24
        const val ICON_FILL = 0.8f
    }
}
