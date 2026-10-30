# AwakeGuard ⏰🛡️

> **Despertador Anti-Escape e Forçado de Segurança Máxima para Android**  
> *Inspirado na engenharia reversa do Alarmy, projetado para eliminar 100% dos escapes inconscientes do sono.*

---

## 📌 Visão Geral

O **AwakeGuard** é um aplicativo Android nativo desenvolvido em **Kotlin** e **Jetpack Compose** focado em disciplina e combate à recaída no sono. O sistema impossibilita o desligamento acidental ou proposital do alarme através de métodos triviais como abaixar o volume, colocar no modo silencioso, fechar o app, forçar parada ou reiniciar o dispositivo.

Para desativar o alarme, o usuário é obrigado a concluir missões cognitivas ou motoras configuráveis.

---

## 🚀 Funcionalidades & Arquitetura

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AWAKEGUARD CORE ENGINE                          │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Exact Alarm Clock   → setAlarmClock() [Bypass Doze Mode]            │
│ 2. Wakelock & Screen   → PARTIAL_WAKE_LOCK + FullScreenIntent          │
│ 3. Audio & Volume Loop → AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE           │
│                          + ContentObserver forçando Volume 100%        │
│ 4. Anti-Power-Off      → AccessibilityService interceptando UI Dialogs │
│ 5. Anti-Uninstall      → DeviceAdminReceiver interceptando Remoção     │
│ 6. Vigilância Fallback → Alarme 5min pós-despertar (Anti-Recaída)     │
└────────────────────────────────────────────────────────────────────────┘
```

### 1. Despertador Exato & Bypass de Economia de Energia
- Utiliza a API `AlarmManager.setAlarmClock()`, que ignora restrições do Doze Mode e Standby Buckets.
- Exibe o ícone nativo de despertador no sistema e garante pontualidade absoluta no despertar.

### 2. Áudio Ininterrupto e Travamento de Volume
- Serviço em primeiro plano com permissão de mídia e foco sonoro transitório exclusivo (`AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE`).
- Monitoramento em tempo real via `ContentObserver`: se o usuário tentar abaixar o volume pelas teclas físicas, o app detecta e restaura instantaneamente para o volume máximo (100%).

### 3. Proteção Anti-Desligamento (Anti-Power-Off)
- `AccessibilityService` dedicado que monitora o `com.android.systemui`.
- Se o menu de desligar/reiniciar for acionado com o alarme ativo, o serviço fecha o diálogo do sistema imediatamente e reabre a tela de missão.

### 4. Proteção contra Desinstalação (Device Administrator)
- Implementado via `DeviceAdminReceiver`.
- Impede a desinstalação ou revogação de privilégios durante o disparo do alarme.

### 5. Missões Cognitivas e Motoras Obrigatórias
- 📳 **Agitar (Shake):** Detecção via acelerômetro com filtro passa-alta e contagem de repetições.
- 🧮 **Matemática:** Desafios matemáticos dinâmicos com múltiplos níveis de dificuldade.
- 🧠 **Memória (Simon):** Reprodução de sequências visuais e sonoras progressivas.
- ✍️ **Digitação:** Frases reflexivas com suporte a normalização Unicode (NFD) para flexibilidade de teclado.
- 📷 **Código de Barras / QR Code:** Validação de código físico (ex: pasta de dentes no banheiro).

### 6. Vigilância Pós-Despertar & Morning Briefing
- **Prevenção de Recaída:** Alarme de verificação silencioso 5 minutos após a conclusão da missão. Se o usuário voltar a dormir, o alarme toca novamente.
- **Morning Voice Briefing:** Síntese de voz (TTS) com saudação, frase do dia e previsão meteorológica em tempo real via **Open-Meteo REST API**.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Kotlin 2.0.21
- **Interface:** Jetpack Compose (BOM 2024.09.00) & Material 3
- **Arquitetura:** Clean Architecture / MVVM / StateFlow
- **Build System:** Gradle 9.0 com Android Gradle Plugin (AGP) 9.0.1
- **Compilação:** `compileSdk 36`, `targetSdk 36`, `minSdk 26` (Android 8.0+)
- **JDK:** Java 21 (Recomendado Android Studio JBR)

---

## 📦 Como Compilar e Executar

### Pré-requisitos
1. **JDK 21** instalado ou configurado pelo Android Studio:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   ```
2. Android SDK com build-tools e platform-tools instalados.

### Compilação do APK Debug
Para gerar o APK de debug:
```bash
./gradlew assembleDebug
```
O arquivo `.apk` compilado estará em:
`app/build/outputs/apk/debug/app-debug.apk`

### Executar Testes Unitários
```bash
./gradlew test
```

---

## 📄 Licença

Distribuído sob a licença MIT. Consulte `LICENSE` para mais detalhes.
