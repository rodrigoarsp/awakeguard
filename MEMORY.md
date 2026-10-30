# MEMORY.md — MEMÓRIA VIVA DO PROJETO AWAKEGUARD

> Fonte única de contexto para qualquer IA (Gemini/Antigravity, Claude, ChatGPT, etc.) ou desenvolvedor que atuar neste projeto.
> **Última atualização:** 2026-10-30 · **Versão do sistema:** v1.1.1 · **Status:** Repositório oficial publicado no GitHub (`rodrigoarsp/awakeguard`).

---

## 0. PROTOCOLO DESTE ARQUIVO (OBRIGATÓRIO)

1. **Ler este arquivo inteiro no início de toda sessão**, antes de editar código, rodar builds ou alterar serviços.
2. **Atualizar ao fim de toda tarefa** que altere: arquitetura de persistência, novas missões, permissões, dependências, versão de SDK/JDK, pendências ou comandos de build.
   - Manter as datas em formato ISO (AAAA-MM-DD).
   - Resolver pendências: retirar da Seção 11 e adicionar no Histórico (Seção 12).
3. **Nunca expor segredos ou chaves privadas** neste arquivo.
4. **Conflito entre este arquivo e o código:** o código em execução é a verdade — corrigir este arquivo imediatamente.
5. Arquivo codificado estritamente em **UTF-8 sem BOM**.
6. **Regra de Idioma:** Todas as respostas, documentações e logs voltados ao usuário devem ser entregues em **Português do Brasil (`pt-BR`)**.

---

## 1. O QUE É O AWAKEGUARD

O **AwakeGuard** é um ecossistema de despertador forçado de segurança máxima e anti-escape (*forced awakening & extreme persistence*), concebido a partir de um relatório aprofundado de engenharia reversa do aplicativo **Alarmy** (`droom.sleepIfUCan`).

### Propósito Central:
Eliminar 100% dos escapes inconscientes do usuário sonolento. O alarme **NÃO PODE** ser desligado ou silenciado por métodos triviais (abaixar volume, colocar no silencioso, fechar o app, forçar parada, desinstalar ou desligar o smartphone).

### Componentes do Ecossistema:
1. **AwakeGuard Android Nativo** (`awakeguard/`): App Android nativo em Kotlin e Jetpack Compose com serviços em primeiro plano, interceptores de acessibilidade, Device Administrator, exact alarms e integração de sensores.
2. **AwakeGuard Web Companion** (`awakeguard-web/`): Simulador PWA reativo com sintetizador de áudio via Web Audio API, Web Speech API para briefing matinal, persistência via LocalStorage e emulação das travas de acessibilidade.

---

## 2. REGRAS MANDATÓRIAS (ENGENHARIA E DIRETRIZES)

- **Permissão Total Autônoma:** O usuário concedeu permissão irrestrita para manutenções, builds, testes e comandos no sistema sem necessidade de confirmação prévia.
- **Primeiros Princípios (Karpathy Style):** Código limpo, enxuto, sem boilerplate desnecessário ou abstrações vazias.
- **Segurança Ética e Pesquisa:** O software é estritamente voltado a auxílio de disciplina pessoal e interoperabilidade técnica; não contém exploits de kernel nem rootkit malicioso.
- **Preservação de Dados de Alarmes:** SharedPreferences e JSON com estrutura estável e migração idempotente.
- **Resiliência Extrema a Falhas:** Toda chamada a APIs externas (ex: Open-Meteo) possui timeout curto (5s) e fallback offline gracioso para nunca travar o disparo do alarme.

---

## 3. ARQUITETURA DE PERSISTÊNCIA & ENGENHARIA REVERSA

A engenharia reversa do Alarmy revelou 5 pilares fundamentais, todos reproduzidos no AwakeGuard:

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

### 3.1. Exact Alarm & Doze Mode Bypass
- **Classe:** `AlarmScheduler.kt`
- Utiliza `AlarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(...), pendingIntent)`.
- É a **única** API do Android que ignora as restrições mais agressivas de economia de energia (Doze Mode, App Standby Buckets).
- Exibe o ícone nativo de despertador na barra de status do sistema e na tela de bloqueio.

### 3.2. Foreground Service & Enquadramento de Áudio
- **Classe:** `AlarmForegroundService.kt`
- Registrado no manifesto com os tipos `mediaPlayback|specialUse`.
- Solicita foco de áudio exclusivo com `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` via `AudioFocusRequestCompat`.
- Cria e mantém um `WAKE_LOCK` de nível `PowerManager.PARTIAL_WAKE_LOCK` para impedir que a CPU hiberne enquanto o alarme toca.
- Registra um `ContentObserver` monitorando `Settings.System.CONTENT_URI`: se o usuário tentar diminuir o volume físico, o serviço detecta em milissegundos e restaura o `STREAM_ALARM` para `getStreamMaxVolume()`.

### 3.3. Interceptação Anti-Power-Off (Menu Desligar)
- **Classe:** `AntiPowerOffAccessibilityService.kt`
- Configuração XML: `accessibility_service_config.xml` com flag `flagRetrieveInteractiveWindows`.
- Ouve eventos `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`.
- Ao detectar abertura de janelas do pacote `com.android.systemui` contendo `GlobalActionsDialog`, `PowerDialog`, "Desligar", "Reiniciar" ou "Power off":
  1. Dispara imediatamente `performGlobalAction(GLOBAL_ACTION_BACK)` para fechar o menu do sistema.
  2. Dispara uma `Intent` explícita trazendo a `AlarmActivity` de volta ao topo com flags `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REORDER_TO_FRONT`.

### 3.4. Proteção contra Desinstalação (Device Admin)
- **Classe:** `DeviceAdminGuardReceiver.kt`
- Declarado em `AndroidManifest.xml` com permissão `android.permission.BIND_DEVICE_ADMIN` e política `device_admin_policy.xml`.
- No método `onDisableRequested(context, intent)`:
  - Verifica se há algum alarme ativo no momento.
  - Se houver alarme tocando, retorna uma mensagem de aviso bloqueando a revogação e impedindo que o Android desinstale o app durante a crise matinal.

### 3.5. Reinicialização e Boot Recovery
- **Classe:** `BootReceiver.kt`
- Ouve `Intent.ACTION_BOOT_COMPLETED` e `ACTION_LOCKED_BOOT_COMPLETED`.
- Ao ligar o aparelho, lê o `AlarmRepository` e re-agenda todos os alarmes ativos via `AlarmScheduler`.

---

## 4. SUBSISTEMA DE MISSÕES COGNITIVAS & MOTORAS

Para desativar o alarme, o usuário é forçado a completar uma missão. Não há botão "Soneca" ou "Desligar" direto.

| Missão | Classe / Arquivo | Funcionamento Técnico |
| :--- | :--- | :--- |
| **Agitar (Shake)** | `ShakeMissionDetector.kt` / `missions.js` | Utiliza o sensor acelerômetro (`Sensor.TYPE_ACCELEROMETER`). Aplica filtro passa-alta (`alpha = 0.8f`) para subtrair a gravidade e detectar somente oscilações abruptas com delta > 12 m/s². Exige N repetições (ex: 20 a 50). |
| **Matemática** | `MathMissionEngine.kt` / `missions.js` | Gera expressões dinâmicas: Fácil (`A + B`), Médio (`A * B + C`), Difícil (`(A * B) - (C * D)`). Números aleatórios sem repetição imediata. Exige 3 a 5 acertos consecutivos. |
| **Memória (Simon)** | `MemoryMissionEngine.kt` / `missions.js` | Jogo estilo Simon Genius com 4 blocos coloridos. A IA pisca uma sequência de comprimento progressivo (ex: 4 a 6 passos) que o usuário precisa reproduzir sem errar. |
| **Digitação de Frases** | `TypingMissionEngine.kt` / `missions.js` | Frases motivacionais ou passagens filosóficas. Implementa **normalização Unicode NFD** (`Normalizer.normalize`) para ignorar diferenças de teclado (ex: `c` vs `ç`, `a` vs `ã`), garantindo que o usuário digite a frase completa sem travar por layout de teclado. |
| **QR Code / Código de Barras** | Arquitetura pronta (`AlarmActivity.kt`) | Leitura de código físico previamente cadastrado (ex: código do creme dental no banheiro). Força o usuário a levantar da cama. |

---

## 5. VIGILÂNCIA PÓS-DESPERTAR & INFORMATIVO MATINAL

### 5.1. Prevenção de Recaída no Sono (*Fall-Back Asleep Prevention*)
- **Classe:** `FallBackAsleepReceiver.kt`
- Ao concluir a missão e desligar o alarme principal, o sistema agenda silenciosamente um alarme secundário para **5 minutos depois**.
- Se o usuário não abrir o aplicativo e tocar no botão de "Estou 100% Acordado", o alarme volta a disparar no volume máximo com uma nova missão obrigatória.

### 5.2. Informativo Matinal & Previsão do Tempo (Open-Meteo REST API)
- **Classes:** `WeatherRepository.kt` (Android) e `src/weather.js` (Web)
- **API Externa:** Open-Meteo (`https://api.open-meteo.com/v1/forecast`)
  - Endpoint gratuito, sem exigência de API Key, alta confiabilidade e sem limite abusivo de requisições.
  - Coleta: `temperature_2m`, `relative_humidity_2m`, `apparent_temperature`, `weather_code`, `wind_speed_10m`.
  - Converte códigos WMO em descrições claras em português (ex: Céu limpo, Chuva moderada, Tempestade).
- Fallback automático para geolocalização IP ou coordenadas padrão de Brasília/Luziânia (`-16.25, -47.95`) caso GPS esteja desativado.

### 5.3. Morning Voice Briefing (Text-to-Speech)
- **Classes:** `TextToSpeech` nativo do Android e `window.speechSynthesis` (Web).
- Fala automaticamente:
  1. Saudação inicial com nome e hora atual.
  2. Previsão do tempo detalhada (temperatura e sensação térmica).
  3. Sequência de dias invictos acordando no horário (*Streak Counter*).
  4. Frase inspiradora do dia.

---

## 6. STACK TECNOLÓGICA & PRÉ-REQUISITOS DE AMBIENTE

### 6.1. Android Nativo (`awakeguard/`)
- **Linguagem:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (BOM 2024.09.00), Material 3
- **Build System:** Gradle 9.0 com Android Gradle Plugin (AGP) 9.0.1
- **Compilação:**
  - `compileSdk = 36`
  - `targetSdk = 36`
  - `minSdk = 26` (Android 8.0 Oreo ou superior)
- **Java / JDK:** **Java 21 Obrigatório** (utilizar o JDK Bundled do Android Studio JBR):
  - Caminho no Windows: `C:\Program Files\Android\Android Studio\jbr`
  - Comando PowerShell: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- **Persistência Local:** SharedPreferences com GSON para serialização de listas de alarmes.

### 6.2. Web Companion (`awakeguard-web/`)
- **Core:** HTML5 Semântico, CSS3 Moderno (Vanilla com Variáveis e Glassmorphism), JavaScript ES2022.
- **Bundler:** Vite 6 (`npx vite --port 5173 --host`).
- **Áudio:** Web Audio API (OscillatorNode com waveforms `square` e `sawtooth` e GainNode dinâmico).
- **Voz:** Web Speech API (`SpeechSynthesisUtterance`).

---

## 7. MAPA COMPLETO DE ARQUIVOS E COMPONENTES

### 7.1. Android (`awakeguard/`)
```
awakeguard/
├── app/
│   ├── build.gradle.kts                     # Dependências Compose, Core KTX, Lifecycle, GSON
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Permissões críticas, Services, Receivers e Activities
│   │   │   ├── kotlin/com/awakeguard/alarm/
│   │   │   │   ├── MainActivity.kt          # Dashboard Compose, gestão de alarmes e checagem de permissões
│   │   │   │   ├── data/
│   │   │   │   │   ├── AlarmItem.kt         # Data class de alarme (hora, dias, missão, tom, volume)
│   │   │   │   │   ├── AlarmRepository.kt   # Armazenamento e persistência SharedPreferences/GSON
│   │   │   │   │   └── WeatherRepository.kt # Cliente HTTP REST Open-Meteo
│   │   │   │   ├── mission/
│   │   │   │   │   ├── ShakeMissionDetector.kt  # Sensor acelerômetro + filtro passa-alta
│   │   │   │   │   ├── MathMissionEngine.kt     # Gerador de equações matemáticas
│   │   │   │   │   ├── MemoryMissionEngine.kt   # Jogo de memória sequencial
│   │   │   │   │   └── TypingMissionEngine.kt   # Digitação com normalização Unicode
│   │   │   │   ├── security/
│   │   │   │   │   ├── AntiPowerOffAccessibilityService.kt # Bloqueio de Power Menu / GlobalActions
│   │   │   │   │   ├── DeviceAdminGuardReceiver.kt         # Bloqueio de desinstalação
│   │   │   │   │   └── SecurityManagerHelper.kt            # Verificação e abertura de Settings intents
│   │   │   │   ├── service/
│   │   │   │   │   ├── AlarmScheduler.kt           # Agendamento exato com setAlarmClock
│   │   │   │   │   ├── AlarmReceiver.kt            # BroadcastReceiver de disparo
│   │   │   │   │   ├── AlarmForegroundService.kt   # Foco de áudio, loop de volume e toque
│   │   │   │   │   ├── BootReceiver.kt             # Reagendamento pós-reinicialização
│   │   │   │   │   └── FallBackAsleepReceiver.kt   # Vigilância pós-despertar
│   │   │   │   └── ui/
│   │   │   │       ├── AlarmActivity.kt            # Tela cheia de bloqueio (Lockdown Mode)
│   │   │   │       └── theme/ (Color.kt, Theme.kt, Type.kt)
│   │   │   └── res/
│   │   │       ├── xml/accessibility_service_config.xml
│   │   │       ├── xml/device_admin_policy.xml
│   │   │       └── values/strings.xml
│   │   └── test/kotlin/com/awakeguard/alarm/
│   │       └── MissionEngineTest.kt                # Testes unitários (Math, Memory, Typing)
└── build.gradle.kts
```

### 7.2. Web Companion (`awakeguard-web/`)
```
awakeguard-web/
├── index.html                   # Estrutura do dashboard, card meteorológico e modal lockdown
├── package.json                 # Scripts de desenvolvimento e build
├── src/
│   ├── main.js                  # Orquestração do app, relógio vivo, triggers de teste
│   ├── style.css                # Paleta Cyberpunk Dark (#070a12, #00e5ff, #ffd700), Glassmorphism
│   ├── audio.js                 # Sintetizador Web Audio API com som agudo de alarme
│   ├── alarms.js                # Gestão de alarmes e cálculo de streak no LocalStorage
│   ├── weather.js               # Integração Open-Meteo REST API e tradução WMO
│   ├── morningBriefing.js       # Fala matinal via Web Speech API
│   └── missions.js              # Implementação web das 4 missões (Shake, Math, Memory, Typing)
```

---

## 8. PERMISSÕES CRÍTICAS & COMO ATIVÁ-LAS (ANDROID MANIFEST)

O manifesto do AwakeGuard exige permissões de nível de sistema para garantir a persistência:

| Permissão | Finalidade | Como Conceder / Ativar |
| :--- | :--- | :--- |
| `SCHEDULE_EXACT_ALARM` | Disparar alarmes precisos no milissegundo | Automático no Android ≤ 11; Ativar em "Alarmes e Lembretes" no Android 12+ |
| `USE_FULL_SCREEN_INTENT` | Abrir a tela de alarme sobre a tela bloqueada | Padrão do sistema para apps de despertador |
| `WAKE_LOCK` | Manter a CPU ativa durante a missão | Concedida na instalação |
| `FOREGROUND_SERVICE` (`mediaPlayback\|specialUse`) | Tocar som contínuo sem ser morto pela memória | Padrão do sistema para serviços declarados |
| `SYSTEM_ALERT_WINDOW` | Sobrepor outras janelas se o app estiver em 2º plano | Configurações → Apps especiais → Sobrepor a outros apps |
| `BIND_ACCESSIBILITY_SERVICE` | Interceptar menus de desligamento e botões de sistema | Configurações → Acessibilidade → Apps Instalados → Ativar "AwakeGuard Anti-Escape" |
| `BIND_DEVICE_ADMIN` | Impedir desinstalação durante o toque do alarme | Configurações → Segurança → Apps do Administrador do Dispositivo → Ativar |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Isentar de otimização agressiva de bateria OEM | Configurações → Otimização de bateria → Não otimizar |

---

## 9. GUIA DE EXECUÇÃO, TESTES E COMPILAÇÃO (CLI)

### 9.1. Compilar e Testar o Android Nativo
Executar sempre no PowerShell a partir da raiz `awakeguard/`:

```powershell
# 1. Definir o Java 21 (essencial para evitar conflito com AGP 9)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# 2. Rodar a suíte de testes unitários das missões
.\gradlew.bat testDebugUnitTest

# 3. Compilar o APK Debug assinado
.\gradlew.bat assembleDebug
```

> **Localização do APK gerado:**  
> `C:\Users\rodri\.gemini\antigravity-ide\scratch\awakeguard\app\build\outputs\apk\debug\app-debug.apk`

### 9.2. Instalação no Celular / Emulador via ADB
Com o dispositivo conectado (ou emulador Android aberto):
```powershell
& "C:\Users\rodri\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\rodri\.gemini\antigravity-ide\scratch\awakeguard\app\build\outputs\apk\debug\app-debug.apk"
```

### 9.3. Rodar o Web Companion
Executar na pasta `awakeguard-web/`:
```powershell
npm run dev
# Ou via npx
npx vite --port 5173 --host
```
Acesse em: `http://localhost:5173/`

---

## 10. COMPANION WEB / PWA (AWAKEGUARD-WEB)

O companion web funciona como bancada de testes interativa para validação de lógica:
- **Simulador de Menu Desligar:** Botão que abre um modal simulando o menu do Android ("Desligar" / "Reiniciar"). O listener de acessibilidade simulado cancela o modal em 400ms e vibra a tela.
- **Sintetizador Web Audio:** Toca onda quadrada estridente alternada em 880Hz e 1760Hz, impossível de ignorar.
- **Morning Briefing:** Botão "Ouvir Briefing Matinal" que sintetiza por voz a previsão climática de hoje e parabeniza o usuário pelo despertar.

---

## 11. PENDÊNCIAS E PRÓXIMOS PASSOS (ROADMAP)

- [ ] **Leitura de Câmera Real (QR/Barcode):** Integrar a biblioteca CameraX + Google ML Kit Barcode Scanning no `AlarmActivity.kt` do Android para leitura ao vivo pela lente traseira.
- [ ] **Seleção de Toques Personalizados:** Adicionar seletor de arquivos de áudio externos (`.mp3`/`.wav`) ou ringtones nativos do sistema Android.
- [ ] **Sincronização Nuvem (Opcional):** Sincronizar dias acordados e configurações de alarme entre múltiplos dispositivos via Firebase ou Supabase.

---

## 12. HISTÓRICO DE MUDANÇAS (MAIS RECENTE PRIMEIRO)

| Data | Versão | O quê foi implementado / corrigido |
| :--- | :--- | :--- |
| **2026-10-30** | v1.1.1 | **Publicação do Projeto no GitHub:**<br>1. Inicialização do repositório Git e criação do repositório público `rodrigoarsp/awakeguard` via GitHub CLI.<br>2. Adição de `.gitignore` otimizado para Android/Gradle, `README.md` completo com arquitetura e manual de build, e licença MIT.<br>3. Push da branch `main` com tracking remoto configurado. |
| **2026-10-28** | v1.1.0 | **Adição de Novas Funções Inerentes e Previsão do Tempo:**<br>1. Criado `TypingMissionEngine.kt` e `TypingMission` no web companion para digitação de frases motivacionais com normalização de acentos Unicode.<br>2. Criado `WeatherRepository.kt` e `src/weather.js` integrando a API REST do Open-Meteo para previsão meteorológica em tempo real (temperatura, umidade, vento, código WMO traduzido).<br>3. Criado `morningBriefing.js` com síntese de voz TTS matinal apresentando hora, clima, sequência de dias e frase de incentivo.<br>4. Suíte de testes unitários `MissionEngineTest.kt` validando todas as missões (Math, Memory, Typing).<br>5. Compilação do APK debug final concluída com sucesso (11.97 MB) em `app/build/outputs/apk/debug/app-debug.apk`. |
| **2026-10-28** | v1.0.0 | **Criação Inicial do Sistema AwakeGuard Baseado na Engenharia Reversa do Alarmy:**<br>1. Estruturação completa do app nativo Kotlin/Compose com `AlarmForegroundService`, `AlarmScheduler` (`setAlarmClock`), `AntiPowerOffAccessibilityService`, `DeviceAdminGuardReceiver` e `FallBackAsleepReceiver`.<br>2. Criação do simulador interativo web companion `awakeguard-web` com sintetizador Web Audio API e missões motoras/cognitivas. |
