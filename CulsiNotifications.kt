package com.chris.culsi.alerts


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chris.culsi.R

object CulsiNotifications {
    const val CHANNEL_ID_DUE = "due_alerts"
    const val GROUP_KEY_DUE = "culsi.due.group"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = mgr.getNotificationChannel(CHANNEL_ID_DUE)
        if (existing != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID_DUE,
            "Discard Due Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when food items reach discard time"
            enableVibration(true)
            setSound(soundUri, attrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        mgr.createNotificationChannel(channel)
    }

    fun buildDueNotification(
        context: Context,
        id: Long,
        itemName: String,
        dueMillis: Long
    ): Notification {
        val title = "Discard Now"
        val text = "$itemName is past its hold time."

        val contentIntent = context.packageManager?.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(
                context, 1000 + id.toInt(), it,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingFlags()
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_DUE)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun pendingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notify(context: Context, id: Long, notification: Notification) {
        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
    }

    fun cancel(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }
}
