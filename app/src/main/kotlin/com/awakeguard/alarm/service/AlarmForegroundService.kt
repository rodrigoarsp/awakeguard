package com.awakeguard.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.awakeguard.alarm.R
import com.awakeguard.alarm.data.AlarmRepository
import com.awakeguard.alarm.ui.AlarmActivity

class AlarmForegroundService : Service() {

    companion object {
        private const val TAG = "AlarmForegroundService"
        const val CHANNEL_ID = "awakeguard_critical_alarm_channel"
        const val NOTIFICATION_ID = 88412
        const val ACTION_STOP_ALARM = "com.awakeguard.alarm.ACTION_STOP_ALARM"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var volumeObserver: ContentObserver? = null
    private var targetVolumeLevel: Int = 100
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        initVibrator()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)

        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarmInternal()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification(alarmId)
        startForeground(NOTIFICATION_ID, notification)

        startSoundAndVibration()
        startVolumeEnforcement()

        // Lança a AlarmActivity em tela cheia imediatamente
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(fullScreenIntent)

        return START_STICKY
    }

    private fun startSoundAndVibration() {
        if (isPlaying) return
        isPlaying = true

        requestAudioFocusExclusive()

        // Obtém o toque de alarme padrão do Android
        var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alertUri == null) {
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { e ->
            Log.e(TAG, "Falha ao iniciar MediaPlayer padrão: ${e.message}")
        }

        // Inicia padrão de vibração agressivo (1s vibrando, 0.5s pausa)
        val pattern = longArrayOf(0, 1000, 500, 1000, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun requestAudioFocusExclusive() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { /* Ignora perda de foco para não permitir que outros apps silenciem */ }
                .build()

            am.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun startVolumeEnforcement() {
        val am = audioManager ?: return
        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        targetVolumeLevel = maxVolume

        // Força imediatamente para o volume máximo
        am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        // Observa alterações no sistema de volume e reverte instantaneamente
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (isPlaying) {
                    val currentVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
                    if (currentVol < targetVolumeLevel) {
                        Log.w(TAG, "Tentativa de abaixar volume detectada! Forçando volume máximo.")
                        am.setStreamVolume(AudioManager.STREAM_ALARM, targetVolumeLevel, 0)
                    }
                }
            }
        }

        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )
    }

    private fun stopAlarmInternal() {
        isPlaying = false
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        vibrator?.cancel()

        volumeObserver?.let {
            contentResolver.unregisterContentObserver(it)
            volumeObserver = null
        }

        // Limpa estado de alarme tocando no repositório
        AlarmRepository(this).setCurrentSoundingAlarmId(null)
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun buildForegroundNotification(alarmId: String?): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            2001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AwakeGuard Despertando!")
            .setContentText("Complete a missão na tela para desligar o alarme.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmes Críticos AwakeGuard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações em tela cheia e execução sonora de alarmes"
                setSound(null, null) // O áudio é gerenciado pelo MediaPlayer dedicado
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarmInternal()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
