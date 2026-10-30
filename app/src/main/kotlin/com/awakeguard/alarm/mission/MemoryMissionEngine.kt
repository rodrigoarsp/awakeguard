package com.awakeguard.alarm.mission

import kotlin.random.Random

class MemoryMissionEngine(
    private val sequenceLength: Int = 4
) {
    private val generatedSequence = mutableListOf<Int>()
    private var currentStep = 0
    private var isSequenceCompleted = false

    init {
        generateSequence()
    }

    private fun generateSequence() {
        generatedSequence.clear()
        for (i in 0 until sequenceLength) {
            generatedSequence.add(Random.nextInt(0, 4)) // 4 blocos (0, 1, 2, 3)
        }
        currentStep = 0
        isSequenceCompleted = false
    }

    fun getSequence(): List<Int> = generatedSequence
    fun getCurrentStep(): Int = currentStep
    fun getSequenceLength(): Int = sequenceLength
    fun isCompleted(): Boolean = isSequenceCompleted

    fun handleBlockClick(blockIndex: Int): Boolean {
        if (isSequenceCompleted) return true

        if (generatedSequence[currentStep] == blockIndex) {
            currentStep++
            if (currentStep >= sequenceLength) {
                isSequenceCompleted = true
            }
            return true
        } else {
            // Errou o passo: reinicia o progresso da rodada
            currentStep = 0
            return false
        }
    }

    fun reset() {
        generateSequence()
    }
}
