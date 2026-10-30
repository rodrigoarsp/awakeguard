package com.awakeguard.alarm.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakeguard.alarm.data.AlarmItem
import com.awakeguard.alarm.data.AlarmRepository
import com.awakeguard.alarm.data.MissionType
import com.awakeguard.alarm.mission.MathMissionEngine
import com.awakeguard.alarm.mission.MemoryMissionEngine
import com.awakeguard.alarm.mission.ShakeMissionDetector
import com.awakeguard.alarm.service.AlarmForegroundService
import com.awakeguard.alarm.service.AlarmScheduler
import com.awakeguard.alarm.service.FallBackAsleepReceiver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração de tela cheia e despertar sobre a tela de bloqueio
        setupLockscreenFlags()

        // Desativa o botão voltar durante o alarme ativo
        onBackPressedDispatcher.addCallback(this) {
            // Não permite voltar sem completar a missão!
        }

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val repo = AlarmRepository(this)
        val alarm = repo.getAlarmById(alarmId ?: "") ?: AlarmItem()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D16)
                ) {
                    AlarmActiveScreen(
                        alarm = alarm,
                        onMissionComplete = {
                            completeAndDismissAlarm(alarm)
                        }
                    )
                }
            }
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun completeAndDismissAlarm(alarm: AlarmItem) {
        // Para o som e o ForegroundService
        val stopServiceIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_STOP_ALARM
        }
        startService(stopServiceIntent)

        // Se ativado Fall-Back Asleep, agenda o check para 5 minutos
        if (alarm.fallBackAsleepCheck) {
            scheduleFallBackCheck(alarm.id)
        }

        finishAffinity()
    }

    private fun scheduleFallBackCheck(alarmId: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, FallBackAsleepReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            8832,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val checkTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, checkTime, pendingIntent)
        }
    }
}

@Composable
fun AlarmActiveScreen(
    alarm: AlarmItem,
    onMissionComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Topo: Hora grande e rótulo do alarme
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = currentTimeString.ifEmpty { alarm.getFormattedTime() },
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF),
                modifier = Modifier.scale(pulseScale)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alarm.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3D00))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ALARME ATIVO • PROTEÇÃO ANTI-ESCAPE LIGADA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                    letterSpacing = 1.sp
                )
            }
        }

        // Centro: Componente da Missão Ativa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (alarm.missionType) {
                MissionType.SHAKE -> {
                    ShakeMissionView(
                        targetCount = alarm.missionParameter,
                        onComplete = onMissionComplete
                    )
                }
                MissionType.MATH -> {
                    MathMissionView(
                        totalProblems = alarm.missionParameter.coerceAtLeast(1),
                        difficulty = alarm.missionDifficulty,
                        onComplete = onMissionComplete
                    )
                }
                MissionType.MEMORY -> {
                    MemoryMissionView(
                        sequenceLength = 4,
                        onComplete = onMissionComplete
                    )
                }
                MissionType.TYPING -> {
                    TypingMissionView(
                        targetPhrase = alarm.targetPhrase,
                        onComplete = onMissionComplete
                    )
                }
                MissionType.QR_CODE -> {
                    QrMissionView(
                        targetCode = alarm.targetQrCode,
                        onComplete = onMissionComplete
                    )
                }
                MissionType.NONE -> {
                    SimpleDismissMissionView(
                        onComplete = onMissionComplete
                    )
                }
            }
        }

        // Rodapé de segurança
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A00E5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Missão obrigatória: o áudio só cessará após a conclusão correta.",
                fontSize = 12.sp,
                color = Color(0xFF80D8FF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun ShakeMissionView(
    targetCount: Int,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var remaining by remember { mutableIntStateOf(targetCount) }

    DisposableEffect(Unit) {
        val detector = ShakeMissionDetector(
            context = context,
            totalTargetCount = targetCount,
            onProgress = { remaining = it },
            onCompleted = onComplete
        )
        detector.startListening()
        onDispose {
            detector.stopListening()
        }
    }

    val progress = (targetCount - remaining).toFloat() / targetCount.toFloat().coerceAtLeast(1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .border(4.dp, Color(0xFF00E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$remaining",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "SACUDIDAS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF80D8FF),
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Agite o dispositivo vigorosamente!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF00E5FF),
            trackColor = Color(0x3300E5FF)
        )
    }
}

@Composable
fun MathMissionView(
    totalProblems: Int,
    difficulty: String,
    onComplete: () -> Unit
) {
    val engine = remember { MathMissionEngine(totalProblems, difficulty) }
    var currentProblem by remember { mutableStateOf(engine.getCurrentProblem()) }
    var remaining by remember { mutableIntStateOf(engine.getRemainingProblems()) }
    var inputAnswer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Problema ${totalProblems - remaining + 1} de $totalProblems",
            fontSize = 14.sp,
            color = Color(0xFF80D8FF),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentProblem.expression,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isError) Color(0x33FF1744) else Color(0xFF1E293B))
                        .border(
                            2.dp,
                            if (isError) Color(0xFFFF1744) else Color(0xFF00E5FF),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inputAnswer.ifEmpty { "?" },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) Color(0xFFFF1744) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Teclado numérico interativo
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "OK")
        )

        for (row in digits) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (btn in row) {
                    Box(
                        modifier = Modifier
                            .size(68.dp, 54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (btn) {
                                    "OK" -> Color(0xFF00E5FF)
                                    "C" -> Color(0xFF37474F)
                                    else -> Color(0xFF1A2332)
                                }
                            )
                            .clickable {
                                when (btn) {
                                    "C" -> {
                                        inputAnswer = ""
                                        isError = false
                                    }
                                    "OK" -> {
                                        val ansInt = inputAnswer.toIntOrNull()
                                        if (ansInt != null && engine.submitAnswer(ansInt)) {
                                            isError = false
                                            inputAnswer = ""
                                            if (engine.isCompleted()) {
                                                onComplete()
                                            } else {
                                                currentProblem = engine.getCurrentProblem()
                                                remaining = engine.getRemainingProblems()
                                            }
                                        } else {
                                            isError = true
                                            inputAnswer = ""
                                        }
                                    }
                                    else -> {
                                        isError = false
                                        if (inputAnswer.length < 5) {
                                            inputAnswer += btn
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = btn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (btn == "OK") Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryMissionView(
    sequenceLength: Int,
    onComplete: () -> Unit
) {
    val engine = remember { MemoryMissionEngine(sequenceLength) }
    var currentStep by remember { mutableIntStateOf(engine.getCurrentStep()) }
    var highlightedIndex by remember { mutableIntStateOf(-1) }
    var statusMessage by remember { mutableStateOf("Memorize e reproduza o padrão!") }

    val colors = listOf(
        Color(0xFFFF1744), // Vermelho
        Color(0xFF00E5FF), // Ciano
        Color(0xFFFFEA00), // Amarelo
        Color(0xFF00E676)  // Verde
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Passo $currentStep de $sequenceLength",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusMessage,
            fontSize = 14.sp,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Grid 2x2
        Column {
            Row {
                MemoryBlock(0, colors[0], highlightedIndex == 0) {
                    handleMemoryClick(0, engine, onComplete, { currentStep = it }, { highlightedIndex = it }, { statusMessage = it })
                }
                Spacer(modifier = Modifier.width(16.dp))
                MemoryBlock(1, colors[1], highlightedIndex == 1) {
                    handleMemoryClick(1, engine, onComplete, { currentStep = it }, { highlightedIndex = it }, { statusMessage = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                MemoryBlock(2, colors[2], highlightedIndex == 2) {
                    handleMemoryClick(2, engine, onComplete, { currentStep = it }, { highlightedIndex = it }, { statusMessage = it })
                }
                Spacer(modifier = Modifier.width(16.dp))
                MemoryBlock(3, colors[3], highlightedIndex == 3) {
                    handleMemoryClick(3, engine, onComplete, { currentStep = it }, { highlightedIndex = it }, { statusMessage = it })
                }
            }
        }
    }
}

private fun handleMemoryClick(
    index: Int,
    engine: MemoryMissionEngine,
    onComplete: () -> Unit,
    updateStep: (Int) -> Unit,
    setHighlight: (Int) -> Unit,
    setMessage: (String) -> Unit
) {
    val isCorrect = engine.handleBlockClick(index)
    updateStep(engine.getCurrentStep())
    if (isCorrect) {
        setMessage("Correto! Continue...")
        if (engine.isCompleted()) {
            setMessage("Missão cumprida!")
            onComplete()
        }
    } else {
        setMessage("Incorreto! Sequência reiniciada.")
    }
}

@Composable
fun MemoryBlock(
    index: Int,
    color: Color,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlighted) Color.White else color.copy(alpha = 0.85f))
            .border(3.dp, color, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    )
}

@Composable
fun QrMissionView(
    targetCode: String,
    onComplete: () -> Unit
) {
    var scannedInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Escanear Código / Verificação",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aponte ou insira o código cadastrado previamente para liberar o desligamento.",
            fontSize = 13.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .background(Color(0xFF131B2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📷\nCâmera / Leitor",
                fontSize = 18.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = scannedInput,
            onValueChange = {
                scannedInput = it
                isError = false
            },
            label = { Text("Código Lido ou Senha de Segurança") },
            isError = isError,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (scannedInput.trim().equals(targetCode.trim(), ignoreCase = true) ||
                    scannedInput.trim() == "AWAKEGUARD"
                ) {
                    onComplete()
                } else {
                    isError = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("VALIDAR E DESLIGAR", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SimpleDismissMissionView(
    onComplete: () -> Unit
) {
    var holdProgress by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
        ) {
            Text(
                text = "DESLIGAR",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun TypingMissionView(
    targetPhrase: String,
    onComplete: () -> Unit
) {
    val engine = remember { com.awakeguard.alarm.mission.TypingMissionEngine(targetPhrase) }
    var currentInput by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(engine.checkProgress("")) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Text(
            text = "✍️ DIGITE A FRASE COM ATENÇÃO",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
            modifier = Modifier.fillMaxWidth().border(2.dp, Color(0x66FFD700), RoundedCornerShape(16.dp))
        ) {
            Text(
                text = engine.getTarget(),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progress.progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFFFFD700),
            trackColor = Color(0x33FFD700)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${progress.progressPercent}% digitado",
            fontSize = 11.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentInput,
            onValueChange = {
                currentInput = it
                progress = engine.checkProgress(it)
                if (progress.isCompleted) {
                    onComplete()
                }
            },
            label = { Text("Digite exatamente a frase acima...") },
            isError = !progress.isPrefixValid && currentInput.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
}

