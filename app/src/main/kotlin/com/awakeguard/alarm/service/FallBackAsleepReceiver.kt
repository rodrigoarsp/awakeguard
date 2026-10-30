package com.awakeguard.alarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.awakeguard.alarm.R
import com.awakeguard.alarm.data.AlarmRepository

class FallBackAsleepReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "FallBackAsleepReceiver"
        const val ACTION_CHECK_AWAKE = "com.awakeguard.alarm.ACTION_CHECK_AWAKE"
        const val ACTION_CONFIRM_AWAKE = "com.awakeguard.alarm.ACTION_CONFIRM_AWAKE"
        const val CHANNEL_ID = "awakeguard_fallback_channel"
        const val NOTIFICATION_ID = 99281
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return

        if (intent.action == ACTION_CONFIRM_AWAKE) {
            Log.i(TAG, "Usuário confirmou que está acordado! Cancelando reativação do alarme.")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        Log.w(TAG, "Verificação pós-despertar disparada! Checando se o usuário ainda está acordado...")
        createNotificationChannel(context)

        val confirmIntent = Intent(context, FallBackAsleepReceiver::class.java).apply {
            action = ACTION_CONFIRM_AWAKE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            1005,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Você ainda está acordado?")
            .setContentText("Toque em 'Estou Acordado!' para confirmar, ou o alarme tocará novamente.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "ESTOU ACORDADO!", confirmPendingIntent)
            .setTimeoutAfter(180_000) // Se em 3 minutos não confirmar, timeout
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Verificação Anti-Voltar a Dormir",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Checagem de vigília pós-despertar"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
