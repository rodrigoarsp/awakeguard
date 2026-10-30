package com.awakeguard.alarm.mission

import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val correctAnswer: Int
)

class MathMissionEngine(
    private val totalProblems: Int = 3,
    private val difficulty: String = "MEDIUM"
) {
    private var solvedProblemsCount = 0
    private var currentProblem: MathProblem = generateProblem()

    fun getCurrentProblem(): MathProblem = currentProblem
    fun getRemainingProblems(): Int = (totalProblems - solvedProblemsCount).coerceAtLeast(0)
    fun getTotalProblems(): Int = totalProblems
    fun isCompleted(): Boolean = solvedProblemsCount >= totalProblems

    fun submitAnswer(userAnswer: Int): Boolean {
        if (userAnswer == currentProblem.correctAnswer) {
            solvedProblemsCount++
            if (!isCompleted()) {
                currentProblem = generateProblem()
            }
            return true
        }
        return false
    }

    private fun generateProblem(): MathProblem {
        return when (difficulty.uppercase()) {
            "EASY" -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(5, 30)
                if (Random.nextBoolean()) {
                    MathProblem("$a + $b", a + b)
                } else {
                    val max = maxOf(a, b)
                    val min = minOf(a, b)
                    MathProblem("$max - $min", max - min)
                }
            }
            "HARD" -> {
                val a = Random.nextInt(11, 25)
                val b = Random.nextInt(4, 9)
                val c = Random.nextInt(15, 60)
                val product = a * b
                if (Random.nextBoolean()) {
                    MathProblem("($a × $b) + $c", product + c)
                } else {
                    MathProblem("($a × $b) - $c", product - c)
                }
            }
            else -> { // "MEDIUM"
                val a = Random.nextInt(15, 60)
                val b = Random.nextInt(10, 40)
                val c = Random.nextInt(5, 20)
                val isAddition = Random.nextBoolean()
                if (isAddition) {
                    MathProblem("$a + $b - $c", a + b - c)
                } else {
                    val mulA = Random.nextInt(4, 9)
                    val mulB = Random.nextInt(4, 9)
                    MathProblem("$mulA × $mulB + $a", (mulA * mulB) + a)
                }
            }
        }
    }
}
