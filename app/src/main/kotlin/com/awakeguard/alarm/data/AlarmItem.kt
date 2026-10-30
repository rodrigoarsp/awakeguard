package com.awakeguard.alarm.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class MissionType(val displayName: String, val description: String) {
    NONE("Sem Missão", "Desliga com um toque simples"),
    SHAKE("Sacudir", "Agite o celular com energia até zerar o contador"),
    MATH("Matemática", "Resolva operações aritméticas para reativar o cérebro"),
    MEMORY("Memória", "Memorize e reproduza o padrão de blocos sonoros"),
    TYPING("Escrever Frase", "Digite uma frase motivacional para despertar a mente"),
    QR_CODE("Código QR / Barras", "Escaneie um código cadastrado (ex: pasta de dente no banheiro)")
}

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "Acordar!",
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf(2, 3, 4, 5, 6), // Seg a Sex
    val missionType: MissionType = MissionType.SHAKE,
    val missionParameter: Int = 30, // 30 shakes ou 3 contas de matemática
    val missionDifficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val targetPhrase: String = "A disciplina é a ponte entre metas e realizações.",
    val targetQrCode: String = "AWAKEGUARD_DEFAULT",
    val preventPowerOff: Boolean = true,
    val fallBackAsleepCheck: Boolean = true,
    val volume: Float = 1.0f,
    val vibrate: Boolean = true
) {
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getDaysDescription(): String {
        if (daysOfWeek.size == 7) return "Todos os dias"
        if (daysOfWeek.isEmpty()) return "Uma vez"
        if (daysOfWeek == listOf(2, 3, 4, 5, 6)) return "Dias úteis"
        if (daysOfWeek == listOf(1, 7)) return "Fim de semana"

        val dayNames = mapOf(
            1 to "Dom", 2 to "Seg", 3 to "Ter", 4 to "Qua", 5 to "Qui", 6 to "Sex", 7 to "Sáb"
        )
        return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("hour", hour)
            put("minute", minute)
            put("label", label)
            put("isEnabled", isEnabled)
            put("daysOfWeek", JSONArray(daysOfWeek))
            put("missionType", missionType.name)
            put("missionParameter", missionParameter)
            put("missionDifficulty", missionDifficulty)
            put("targetPhrase", targetPhrase)
            put("targetQrCode", targetQrCode)
            put("preventPowerOff", preventPowerOff)
            put("fallBackAsleepCheck", fallBackAsleepCheck)
            put("volume", volume.toDouble())
            put("vibrate", vibrate)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmItem {
            val daysJson = json.optJSONArray("daysOfWeek") ?: JSONArray()
            val days = mutableListOf<Int>()
            for (i in 0 until daysJson.length()) {
                days.add(daysJson.getInt(i))
            }

            return AlarmItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                hour = json.optInt("hour", 7),
                minute = json.optInt("minute", 0),
                label = json.optString("label", "Despertador"),
                isEnabled = json.optBoolean("isEnabled", true),
                daysOfWeek = days,
                missionType = runCatching {
                    MissionType.valueOf(json.optString("missionType", MissionType.SHAKE.name))
                }.getOrDefault(MissionType.SHAKE),
                missionParameter = json.optInt("missionParameter", 30),
                missionDifficulty = json.optString("missionDifficulty", "MEDIUM"),
                targetPhrase = json.optString("targetPhrase", "A disciplina é a ponte entre metas e realizações."),
                targetQrCode = json.optString("targetQrCode", "AWAKEGUARD_DEFAULT"),
                preventPowerOff = json.optBoolean("preventPowerOff", true),
                fallBackAsleepCheck = json.optBoolean("fallBackAsleepCheck", true),
                volume = json.optDouble("volume", 1.0).toFloat(),
                vibrate = json.optBoolean("vibrate", true)
            )
        }
    }
}
