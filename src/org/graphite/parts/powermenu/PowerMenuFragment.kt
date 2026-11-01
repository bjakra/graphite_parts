package org.graphite.parts.powermenu

import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.graphite.parts.GraphiteSettings
import org.graphite.parts.R

/**
 * Power menu editor: which actions SystemUI shows and in which order. Written to
 * graphite_power_menu_actions (comma-separated config keys, see GlobalActionType in SystemUI).
 */
class PowerMenuFragment : Fragment() {

    private data class Item(val key: String, val label: Int, var enabled: Boolean)

    private val items = mutableListOf<Item>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        val view = inflater.inflate(R.layout.power_menu_editor, container, false)
        load()
        val list = view.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(requireContext())
        val adapter = Adapter()
        list.adapter = adapter
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0,
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val item = items.removeAt(from.bindingAdapterPosition)
                items.add(to.bindingAdapterPosition, item)
                adapter.notifyItemMoved(from.bindingAdapterPosition, to.bindingAdapterPosition)
                save()
                return true
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = true
        })
        touchHelper.attachToRecyclerView(list)
        adapter.touchHelper = touchHelper
        view.findViewById<View>(R.id.reset).setOnClickListener {
            Settings.Secure.putString(requireContext().contentResolver, GraphiteSettings.POWER_MENU_ACTIONS, null)
            load()
            adapter.notifyDataSetChanged()
        }
        return view
    }

    private fun load() {
        val custom = Settings.Secure.getString(requireContext().contentResolver, GraphiteSettings.POWER_MENU_ACTIONS)
        val enabled = custom?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: defaultActions()
        items.clear()
        enabled.filter { it in LABELS }.forEach { items.add(Item(it, LABELS.getValue(it), true)) }
        LABELS.keys.filter { it !in enabled }.forEach { items.add(Item(it, LABELS.getValue(it), false)) }
    }

    private fun save() {
        val keys = items.filter { it.enabled }.map { it.key }
        // SystemUI ignores lists without "power"; keep the editor consistent with that
        val value = if ("power" in keys) keys.joinToString(",") else null
        Settings.Secure.putString(requireContext().contentResolver, GraphiteSettings.POWER_MENU_ACTIONS, value)
    }

    private fun defaultActions(): List<String> {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_globalActionsList", "array", "android")
        return if (id != 0) res.getStringArray(id).toList() else listOf("power", "restart")
    }

    private inner class Adapter : RecyclerView.Adapter<Holder>() {
        var touchHelper: ItemTouchHelper? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.power_menu_item, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.title.setText(item.label)
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = item.enabled
            // Power off must stay; without it the menu has no way to shut down
            holder.check.isEnabled = item.key != "power"
            holder.check.setOnCheckedChangeListener { _, checked ->
                item.enabled = checked
                save()
            }
            holder.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper?.startDrag(holder)
                false
            }
        }
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val check: CheckBox = view.findViewById(R.id.check)
        val handle: ImageView = view.findViewById(R.id.handle)
    }

    companion object {
        private val LABELS = linkedMapOf(
            "power" to R.string.action_power,
            "restart" to R.string.action_restart,
            "restart_recovery" to R.string.action_restart_recovery,
            "restart_bootloader" to R.string.action_restart_bootloader,
            "restart_systemui" to R.string.action_restart_systemui,
            "screenshot" to R.string.action_screenshot,
            "wallet" to R.string.action_wallet,
            "emergency" to R.string.action_emergency,
            "lockdown" to R.string.action_lockdown,
            "airplane" to R.string.action_airplane,
            "silent" to R.string.action_silent,
            "settings" to R.string.action_settings,
            "bugreport" to R.string.action_bugreport,
            "logout" to R.string.action_logout,
        )
    }
}
