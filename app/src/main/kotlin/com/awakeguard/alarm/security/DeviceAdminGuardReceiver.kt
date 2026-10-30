package com.awakeguard.alarm.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awakeguard.alarm.data.AlarmRepository

class DeviceAdminGuardReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminGuard"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Proteção de Administrador do Dispositivo ATIVADA")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val repo = AlarmRepository(context)
        if (repo.isAlarmCurrentlySounding()) {
            return "AÇÃO NEGADA: O alarme do AwakeGuard está tocando! Você não pode desativar o administrador até completar sua missão de despertar."
        }
        return "Aviso: Desativar a proteção de administrador permitirá a desinstalação sem bloqueio anti-escape."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Proteção de Administrador do Dispositivo DESATIVADA")
    }
}
