package com.awakeguard.alarm.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("awakeguard_alarms_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALARMS = "saved_alarms_list"
        private const val KEY_ANTI_POWER_OFF_GLOBAL = "anti_power_off_global"
        private const val KEY_CURRENT_ALARM_ID = "current_sounding_alarm_id"
    }

    fun getAllAlarms(): List<AlarmItem> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return getDefaultAlarms()
        return runCatching {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<AlarmItem>()
            for (i in 0 until jsonArray.length()) {
                list.add(AlarmItem.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }.getOrDefault(getDefaultAlarms())
    }

    fun saveAlarms(alarms: List<AlarmItem>) {
        val jsonArray = JSONArray()
        for (item in alarms) {
            jsonArray.put(item.toJson())
        }
        prefs.edit().putString(KEY_ALARMS, jsonArray.toString()).apply()
    }

    fun getAlarmById(id: String): AlarmItem? {
        return getAllAlarms().firstOrNull { it.id == id }
    }

    fun upsertAlarm(alarm: AlarmItem) {
        val current = getAllAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        saveAlarms(current)
    }

    fun deleteAlarm(id: String) {
        val current = getAllAlarms().filterNot { it.id == id }
        saveAlarms(current)
    }

    fun toggleAlarm(id: String, enabled: Boolean): AlarmItem? {
        val current = getAllAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = current[index].copy(isEnabled = enabled)
            current[index] = updated
            saveAlarms(current)
            return updated
        }
        return null
    }

    fun setCurrentSoundingAlarmId(id: String?) {
        prefs.edit().putString(KEY_CURRENT_ALARM_ID, id).apply()
    }

    fun getCurrentSoundingAlarmId(): String? {
        return prefs.getString(KEY_CURRENT_ALARM_ID, null)
    }

    fun isAlarmCurrentlySounding(): Boolean {
        return getCurrentSoundingAlarmId() != null
    }

    private fun getDefaultAlarms(): List<AlarmItem> {
        return listOf(
            AlarmItem(
                id = "default_1",
                hour = 6,
                minute = 30,
                label = "Acordar e Treinar",
                isEnabled = true,
                daysOfWeek = listOf(2, 3, 4, 5, 6),
                missionType = MissionType.SHAKE,
                missionParameter = 25,
                preventPowerOff = true,
                fallBackAsleepCheck = true
            ),
            AlarmItem(
                id = "default_2",
                hour = 7,
                minute = 15,
                label = "Estudo & Trabalho",
                isEnabled = false,
                daysOfWeek = listOf(2, 3, 4, 5, 6),
                missionType = MissionType.MATH,
                missionParameter = 3,
                missionDifficulty = "MEDIUM",
                preventPowerOff = true,
                fallBackAsleepCheck = false
            )
        )
    }
}
