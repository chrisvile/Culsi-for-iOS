package com.chris.culsi.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class DueAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CulsiNotifications.ensureChannels(context)

        val id = intent.getLongExtra("logId", -1)
        val itemName = intent.getStringExtra("itemName") ?: "Item"
        val dueAt = intent.getLongExtra("dueAt", System.currentTimeMillis())

        val notif = CulsiNotifications.buildDueNotification(context, id, itemName, dueAt)
        NotificationManagerCompat.from(context).notify(id.toInt(), notif)
    }
}
