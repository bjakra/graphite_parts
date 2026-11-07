package org.graphite.parts

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Warning dialog for a switch that turns off a security protection. The confirm button stays disabled
 * for a 10 s countdown so the choice is deliberate.
 */
fun showConfirmCountdown(
    context: Context,
    titleRes: Int,
    messageRes: Int,
    confirmRes: Int,
    onConfirm: () -> Unit,
) {
    val dialog = AlertDialog.Builder(context)
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(confirmRes) { _, _ -> onConfirm() }
        .setNegativeButton(android.R.string.cancel, null)
        .create()
    dialog.show()

    val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    val label = context.getString(confirmRes)
    val handler = Handler(Looper.getMainLooper())
    var remaining = 10
    confirm.isEnabled = false
    val tick = object : Runnable {
        override fun run() {
            if (!dialog.isShowing) return
            if (remaining > 0) {
                confirm.text = "$label ($remaining)"
                remaining--
                handler.postDelayed(this, 1000)
            } else {
                confirm.text = label
                confirm.isEnabled = true
            }
        }
    }
    handler.post(tick)
}
