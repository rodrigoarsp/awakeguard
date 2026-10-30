package com.awakeguard.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.awakeguard.alarm.data.AlarmItem
import com.awakeguard.alarm.ui.AlarmActivity
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val EXTRA_ALARM_ID = "extra_alarm_id"

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMillis = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para exibir quando o usuário toca no ícone de despertador na barra de status
        val showIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val pendingShowIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 1000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingShowIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.i(TAG, "Alarme agendado com sucesso para id=${alarm.id} em ${Calendar.getInstance().apply { timeInMillis = triggerTimeMillis }.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Falha de permissão ao agendar alarme exato: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Alarme cancelado: $alarmId")
        }
    }

    fun scheduleTestAlarmImmediately(context: Context, alarm: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        context.sendBroadcast(intent)
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: List<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            // Disparo único: se já passou hoje, agenda para amanhã
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Caso haja repetição semanal nos dias selecionados
        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val currentDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK) // 1=Dom, 7=Sáb
            if (daysOfWeek.contains(currentDayOfWeek) && candidate.after(now)) {
                return candidate.timeInMillis
            }
        }

        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}
