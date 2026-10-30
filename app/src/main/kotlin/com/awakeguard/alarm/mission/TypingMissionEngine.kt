package com.awakeguard.alarm.mission

class TypingMissionEngine(
    phrase: String? = null
) {
    companion object {
        val DEFAULT_MOTIVATIONAL_PHRASES = listOf(
            "A disciplina é a ponte entre metas e realizações.",
            "Vença a cama hoje para vencer seus objetivos amanhã.",
            "O sucesso nasce do esforço diário e da constância.",
            "Coragem é o domínio do foco e a persistência diária.",
            "Cada manhã traz uma nova chance de ser extraordinário.",
            "A preguiça cria desculpas, a disciplina cria resultados.",
            "Grandes conquistas são feitas de pequenos hábitos mantidos.",
            "Acorde com determinação e vá dormir com satisfação.",
            "O dia só começa a dar certo quando você decide levantar."
        )
    }

    val targetPhrase: String = phrase?.ifBlank { null } ?: DEFAULT_MOTIVATIONAL_PHRASES.random()

    fun getTarget(): String = targetPhrase

    fun checkProgress(userInput: String): TypingProgress {
        val isPrefix = targetPhrase.startsWith(userInput)
        val percent = if (targetPhrase.isEmpty()) 100 else {
            ((userInput.length.toFloat() / targetPhrase.length.toFloat()) * 100).toInt().coerceIn(0, 100)
        }
        val isCompleted = userInput == targetPhrase

        return TypingProgress(
            target = targetPhrase,
            current = userInput,
            isPrefixValid = isPrefix,
            progressPercent = percent,
            isCompleted = isCompleted
        )
    }
}

data class TypingProgress(
    val target: String,
    val current: String,
    val isPrefixValid: Boolean,
    val progressPercent: Int,
    val isCompleted: Boolean
)
