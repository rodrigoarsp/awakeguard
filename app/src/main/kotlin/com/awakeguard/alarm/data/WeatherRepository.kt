package com.awakeguard.alarm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val city: String = "Luziânia / GO",
    val temperature: Int = 23,
    val condition: String = "Céu Limpo",
    val icon: String = "☀️",
    val feelsLike: Int = 23,
    val maxTemp: Int = 28,
    val minTemp: Int = 18
)

class WeatherRepository {

    suspend fun getWeatherData(lat: Double = -15.793889, lon: Double = -47.882778): WeatherData {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val current = json.getJSONObject("current")
                    val daily = json.getJSONObject("daily")

                    val code = current.optInt("weather_code", 0)
                    val info = translateWeatherCode(code)

                    WeatherData(
                        city = "Luziânia / Centro-Oeste",
                        temperature = Math.round(current.optDouble("temperature_2m", 24.0)).toInt(),
                        condition = info.first,
                        icon = info.second,
                        feelsLike = Math.round(current.optDouble("apparent_temperature", 24.0)).toInt(),
                        maxTemp = Math.round(daily.getJSONArray("temperature_2m_max").optDouble(0, 28.0)).toInt(),
                        minTemp = Math.round(daily.getJSONArray("temperature_2m_min").optDouble(0, 18.0)).toInt()
                    )
                } else {
                    getFallbackWeather()
                }
            } catch (e: Exception) {
                getFallbackWeather()
            }
        }
    }

    private fun translateWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Céu Limpo", "☀️")
            1 -> Pair("Predominantemente Ensolarado", "🌤️")
            2 -> Pair("Parcialmente Nublado", "⛅")
            3 -> Pair("Encoberto", "☁️")
            45, 48 -> Pair("Nevoeiro", "🌫️")
            51, 53, 55 -> Pair("Chuvisco", "🌦️")
            61, 63, 65 -> Pair("Chuva", "🌧️")
            80, 81, 82 -> Pair("Pancadas de Chuva", "🌧️")
            95, 96, 99 -> Pair("Tempestade", "⛈️")
            else -> Pair("Tempo Bom", "☀️")
        }
    }

    private fun getFallbackWeather(): WeatherData {
        return WeatherData(
            city = "Luziânia / GO",
            temperature = 24,
            condition = "Céu Limpo",
            icon = "☀️",
            feelsLike = 24,
            maxTemp = 29,
            minTemp = 19
        )
    }
}
