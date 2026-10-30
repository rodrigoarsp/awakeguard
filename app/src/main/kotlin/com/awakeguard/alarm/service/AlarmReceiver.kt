package com.awakeguard.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.awakeguard.alarm.data.AlarmRepository

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val WAKE_LOCK_TAG = "awakeguard:alarm_receiver_wakelock"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        Log.i(TAG, "Alarme recebido pelo BroadcastReceiver! id=$alarmId")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )

        wakeLock?.acquire(30_000) // 30 segundos de segurança até o Foreground Service assumir

        val repo = AlarmRepository(context)
        repo.setCurrentSoundingAlarmId(alarmId)

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
