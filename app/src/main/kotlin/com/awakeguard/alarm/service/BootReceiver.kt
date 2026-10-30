package com.awakeguard.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awakeguard.alarm.data.AlarmRepository

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Dispositivo reiniciado. Reagendando todos os alarmes ativos...")
            val repo = AlarmRepository(context)
            val alarms = repo.getAllAlarms().filter { it.isEnabled }

            for (alarm in alarms) {
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
            Log.i(TAG, "${alarms.size} alarmes reagendados após boot.")
        }
    }
}
