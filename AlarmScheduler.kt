package com.chris.culsi.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    private fun pendingFlags(): Int {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0) or
                PendingIntent.FLAG_UPDATE_CURRENT
    }

    /** True when we’re allowed to use exact alarms on this device/state. */
    private fun canUseExactAlarms(context: Context, am: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Public helper for UI code (e.g., to show a banner). */
    fun hasExactAlarmAccess(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return canUseExactAlarms(context, am)
    }

    /** Deep link into system settings where the user can grant exact alarm access (Android 12+). */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }.onFailure {
                Log.w(TAG, "Unable to open exact alarm settings", it)
            }
        }
    }

    fun scheduleDueAlarm(context: Context, logId: Long, itemName: String, dueAt: Long) {
        if (dueAt <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DueAlarmReceiver::class.java).apply {
            action = "com.chris.culsi.ACTION_FOOD_DUE"
            putExtra("logId", logId)
            putExtra("itemName", itemName)
            putExtra("dueAt", dueAt)
        }
        val pi = PendingIntent.getBroadcast(context, logId.toInt(), intent, pendingFlags())

        val tryExact = canUseExactAlarms(context, am)
        if (tryExact) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, dueAt, pi)
                }
                return
            } catch (sec: SecurityException) {
                Log.w(TAG, "Exact alarm not permitted at runtime; falling back to inexact.", sec)
            }
        }

        // Fallback path (won’t crash; may fire a little late)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
        }
    }

    fun cancelDueAlarm(context: Context, logId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DueAlarmReceiver::class.java).apply {
            action = "com.chris.culsi.ACTION_FOOD_DUE"
        }
        val pi = PendingIntent.getBroadcast(context, logId.toInt(), intent, pendingFlags())
        am.cancel(pi)
        CulsiNotifications.cancel(context, logId)
    }
}
