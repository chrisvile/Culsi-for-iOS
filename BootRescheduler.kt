package com.chris.culsi.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chris.culsi.data.CulsiDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRescheduler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        CulsiNotifications.ensureChannels(context)

        val db = CulsiDb.get(context)
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = db.foodLogDao()
            val candidates = dao.getActiveDueAfter(now)

            for (log in candidates) {
                AlarmScheduler.scheduleDueAlarm(context, log.id, log.itemName, log.discardDueAt)
            }
        }
    }
}
