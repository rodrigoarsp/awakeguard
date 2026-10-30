package com.awakeguard.alarm.security

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.awakeguard.alarm.data.AlarmRepository
import com.awakeguard.alarm.ui.AlarmActivity

class AntiPowerOffAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AntiPowerOffGuard"
        private val SUSPICIOUS_WINDOW_KEYWORDS = listOf(
            "globalactions",
            "shutdown",
            "powerdialog",
            "reboot",
            "poweroff",
            "power off",
            "desligar",
            "reiniciar"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val repo = AlarmRepository(this)
        // Somente intercepta se houver alarme tocando ativamente no momento!
        if (!repo.isAlarmCurrentlySounding()) {
            return
        }

        val className = event.className?.toString()?.lowercase() ?: ""
        val packageName = event.packageName?.toString()?.lowercase() ?: ""

        val isSystemUi = packageName.contains("systemui") || packageName.contains("android")
        val matchesKeyword = SUSPICIOUS_WINDOW_KEYWORDS.any { className.contains(it) }

        // Verifica texto dos nós se for evento de janela
        var hasPowerText = false
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                hasPowerText = checkNodeForKeywords(rootNode)
            }
        }

        if (matchesKeyword || (isSystemUi && hasPowerText)) {
            Log.w(TAG, "Tentativa de desligamento/saída detectada durante o alarme! Bloqueando...")

            // Fecha imediatamente o diálogo do sistema (Back Action)
            performGlobalAction(GLOBAL_ACTION_BACK)

            // Reabre a AlarmActivity e força o foco para primeiro plano
            val currentAlarmId = repo.getCurrentSoundingAlarmId()
            val forceFrontIntent = Intent(this, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(AlarmActivity.EXTRA_ALARM_ID, currentAlarmId)
            }
            startActivity(forceFrontIntent)
        }
    }

    private fun checkNodeForKeywords(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (SUSPICIOUS_WINDOW_KEYWORDS.any { text.contains(it) || contentDesc.contains(it) }) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (checkNodeForKeywords(child)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }
}
