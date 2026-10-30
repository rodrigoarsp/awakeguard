package com.awakeguard.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakeguard.alarm.data.AlarmItem
import com.awakeguard.alarm.data.AlarmRepository
import com.awakeguard.alarm.data.MissionType
import com.awakeguard.alarm.security.SecurityManagerHelper
import com.awakeguard.alarm.service.AlarmScheduler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D16)
                ) {
                    MainAlarmDashboardScreen()
                }
            }
        }
    }
}

@Composable
fun MainAlarmDashboardScreen() {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    var alarmsList by remember { mutableStateOf(repository.getAllAlarms()) }

    var isAccessibilityEnabled by remember { mutableStateOf(SecurityManagerHelper.isAccessibilityServiceEnabled(context)) }
    var isDeviceAdminActive by remember { mutableStateOf(SecurityManagerHelper.isDeviceAdminActive(context)) }
    var canScheduleExact by remember { mutableStateOf(SecurityManagerHelper.canScheduleExactAlarms(context)) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    fun refreshAlarms() {
        alarmsList = repository.getAllAlarms()
        isAccessibilityEnabled = SecurityManagerHelper.isAccessibilityServiceEnabled(context)
        isDeviceAdminActive = SecurityManagerHelper.isDeviceAdminActive(context)
        canScheduleExact = SecurityManagerHelper.canScheduleExactAlarms(context)
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAlarm = AlarmItem(
                        hour = 7,
                        minute = 0,
                        label = "Novo Despertador",
                        missionType = MissionType.SHAKE
                    )
                    showEditDialog = true
                },
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AwakeGuard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Despertador Forçado & Anti-Escape",
                        fontSize = 12.sp,
                        color = Color(0xFF80D8FF),
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { showSecurityDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAccessibilityEnabled && isDeviceAdminActive) Color(0xFF00C853) else Color(0xFFFF9100)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isAccessibilityEnabled && isDeviceAdminActive) "🛡️ Protegido" else "⚠️ Configurar Escudo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner informativo de Escudo Anti-Escape
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CAMADAS DE PERSISTÊNCIA ATIVAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SecurityBadge(
                            label = "Acessibilidade",
                            isActive = isAccessibilityEnabled,
                            activeText = "Bloqueia Power",
                            inactiveText = "Inativo"
                        )
                        SecurityBadge(
                            label = "Device Admin",
                            isActive = isDeviceAdminActive,
                            activeText = "Anti-Uninstall",
                            inactiveText = "Inativo"
                        )
                        SecurityBadge(
                            label = "Alarme Exato",
                            isActive = canScheduleExact,
                            activeText = "Doze Bypass",
                            inactiveText = "Inativo"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SEUS ALARMES",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Alarmes Cadastrados
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(alarmsList, key = { it.id }) { alarm ->
                    AlarmCardItem(
                        alarm = alarm,
                        onToggle = { enabled ->
                            repository.toggleAlarm(alarm.id, enabled)
                            if (enabled) {
                                AlarmScheduler.scheduleAlarm(context, alarm.copy(isEnabled = true))
                            } else {
                                AlarmScheduler.cancelAlarm(context, alarm.id)
                            }
                            refreshAlarms()
                        },
                        onTestNow = {
                            // Dispara alarme de teste imediatamente
                            AlarmScheduler.scheduleTestAlarmImmediately(context, alarm)
                        },
                        onEdit = {
                            editingAlarm = alarm
                            showEditDialog = true
                        },
                        onDelete = {
                            AlarmScheduler.cancelAlarm(context, alarm.id)
                            repository.deleteAlarm(alarm.id)
                            refreshAlarms()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Diálogo de Edição / Criação de Alarme
    if (showEditDialog && editingAlarm != null) {
        EditAlarmDialog(
            alarm = editingAlarm!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                repository.upsertAlarm(updated)
                if (updated.isEnabled) {
                    AlarmScheduler.scheduleAlarm(context, updated)
                }
                refreshAlarms()
                showEditDialog = false
            }
        )
    }

    // Diálogo de Configuração dos Escudos de Segurança
    if (showSecurityDialog) {
        SecurityStatusDialog(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isDeviceAdminActive = isDeviceAdminActive,
            canScheduleExact = canScheduleExact,
            onOpenAccessibility = { SecurityManagerHelper.openAccessibilitySettings(context) },
            onRequestDeviceAdmin = { SecurityManagerHelper.requestDeviceAdmin(context) },
            onOpenExactAlarm = { SecurityManagerHelper.openExactAlarmSettings(context) },
            onDismiss = {
                refreshAlarms()
                showSecurityDialog = false
            }
        )
    }
}

@Composable
fun SecurityBadge(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) Color(0x3300C853) else Color(0x33FF3D00))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isActive) activeText else inactiveText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFF00E676) else Color(0xFFFF5252)
            )
        }
    }
}

@Composable
fun AlarmCardItem(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onTestNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.getFormattedTime(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (alarm.isEnabled) Color(0xFF00E5FF) else Color.Gray
                    )
                    Text(
                        text = alarm.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF00E5FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag da Missão
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (alarm.missionType) {
                                    MissionType.SHAKE -> Color(0x33FF9100)
                                    MissionType.MATH -> Color(0x3300E5FF)
                                    MissionType.MEMORY -> Color(0x33E040FB)
                                    MissionType.TYPING -> Color(0x33FFD700)
                                    MissionType.QR_CODE -> Color(0x3376FF03)
                                    MissionType.NONE -> Color(0x339E9E9E)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 ${alarm.missionType.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (alarm.missionType) {
                                MissionType.SHAKE -> Color(0xFFFF9100)
                                MissionType.MATH -> Color(0xFF00E5FF)
                                MissionType.MEMORY -> Color(0xFFEA80FC)
                                MissionType.TYPING -> Color(0xFFFFD700)
                                MissionType.QR_CODE -> Color(0xFFB2FF59)
                                MissionType.NONE -> Color.LightGray
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alarm.getDaysDescription(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Botão de Teste Imediato
                Button(
                    onClick = onTestNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ Testar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
fun EditAlarmDialog(
    alarm: AlarmItem,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var label by remember { mutableStateOf(alarm.label) }
    var missionType by remember { mutableStateOf(alarm.missionType) }
    var missionParam by remember { mutableIntStateOf(alarm.missionParameter) }
    var missionDifficulty by remember { mutableStateOf(alarm.missionDifficulty) }
    var targetPhrase by remember { mutableStateOf(alarm.targetPhrase) }
    var preventPowerOff by remember { mutableStateOf(alarm.preventPowerOff) }
    var fallBackAsleep by remember { mutableStateOf(alarm.fallBackAsleepCheck) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131B2A),
        title = {
            Text("Configurar Alarme", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn {
                item {
                    // Horário
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", hour),
                            onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                            label = { Text("Hora") },
                            modifier = Modifier.width(80.dp)
                        )
                        Text(" : ", fontSize = 24.sp, color = Color.White)
                        OutlinedTextField(
                            value = String.format("%02d", minute),
                            onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                            label = { Text("Minuto") },
                            modifier = Modifier.width(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Nome do Alarme") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Tipo de Missão de Despertar:", fontSize = 13.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(6.dp))

                    MissionType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (missionType == type) Color(0x3300E5FF) else Color.Transparent)
                                .clickable { missionType = type }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (missionType == type) "🔘 " else "⚪ ",
                                color = Color(0xFF00E5FF)
                            )
                            Column {
                                Text(type.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(type.description, color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Parâmetros da missão
                    if (missionType == MissionType.SHAKE) {
                        OutlinedTextField(
                            value = missionParam.toString(),
                            onValueChange = { missionParam = it.toIntOrNull()?.coerceIn(5, 100) ?: 30 },
                            label = { Text("Número de Sacudidas (5 a 100)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (missionType == MissionType.MATH) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                                Button(
                                    onClick = { missionDifficulty = diff },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (missionDifficulty == diff) Color(0xFF00E5FF) else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier.weight(1f).padding(2.dp)
                                ) {
                                    Text(
                                        text = diff,
                                        fontSize = 11.sp,
                                        color = if (missionDifficulty == diff) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (missionType == MissionType.TYPING) {
                        OutlinedTextField(
                            value = targetPhrase,
                            onValueChange = { targetPhrase = it },
                            label = { Text("Frase Motivacional para Digitar") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opções Anti-Escape
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prevenir Desligamento", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Fecha menu power via acessibilidade", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = preventPowerOff, onCheckedChange = { preventPowerOff = it })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Anti-Voltar a Dormir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Checagem de vigília 5 min após acordar", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = fallBackAsleep, onCheckedChange = { fallBackAsleep = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        alarm.copy(
                            hour = hour,
                            minute = minute,
                            label = label.ifEmpty { "Despertador" },
                            missionType = missionType,
                            missionParameter = missionParam,
                            missionDifficulty = missionDifficulty,
                            targetPhrase = targetPhrase.ifBlank { "A disciplina é a ponte entre metas e realizações." },
                            preventPowerOff = preventPowerOff,
                            fallBackAsleepCheck = fallBackAsleep
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Salvar Alarme", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.LightGray)
            }
        }
    )
}

@Composable
fun SecurityStatusDialog(
    isAccessibilityEnabled: Boolean,
    isDeviceAdminActive: Boolean,
    canScheduleExact: Boolean,
    onOpenAccessibility: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131B2A),
        title = {
            Text("Escudos de Segurança & Persistência", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Para que o AwakeGuard garanta o despertar forçado e impeça o escape ou desligamento, ative as permissões abaixo:",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Item 1: Acessibilidade
                PermissionRow(
                    title = "1. Serviço de Acessibilidade",
                    desc = "Detecta e fecha instantaneamente o menu de desligar o aparelho enquanto o alarme estiver ativo.",
                    isGranted = isAccessibilityEnabled,
                    buttonLabel = "Ativar Acessibilidade",
                    onClick = onOpenAccessibility
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Item 2: Device Admin
                PermissionRow(
                    title = "2. Administrador do Dispositivo",
                    desc = "Bloqueia a desinstalação não autorizada do aplicativo enquanto o alarme estiver soando.",
                    isGranted = isDeviceAdminActive,
                    buttonLabel = "Ativar Administrador",
                    onClick = onRequestDeviceAdmin
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Item 3: Alarme Exato
                PermissionRow(
                    title = "3. Alarmes Exatos",
                    desc = "Garante disparo no segundo exato ignorando o Doze Mode e otimizações de bateria do Android.",
                    isGranted = canScheduleExact,
                    buttonLabel = "Permitir Alarme Exato",
                    onClick = onOpenExactAlarm
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PermissionRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = if (isGranted) "✅ ATIVO" else "❌ PENDENTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF5252)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = Color.LightGray, fontSize = 11.sp)
            if (!isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
