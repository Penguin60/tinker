package com.tinker.app.sms

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat

object SmsSender {
    private const val CHANNEL_ID = "tinker_status"

    @SuppressLint("MissingPermission")
    fun send(context: Context, phone: String, message: String) {
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            @Suppress("DEPRECATION") SmsManager.getDefault()
        val parts = sms.divideMessage(message)
        sms.sendMultipartTextMessage(phone, null, parts, null, null)
    }

    /** Confirmation notification so you know the trigger fired. No-ops silently if POST_NOTIFICATIONS is denied. */
    fun notify(context: Context, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Status", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
    }
}
