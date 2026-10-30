package com.awakeguard.alarm

import com.awakeguard.alarm.data.AlarmItem
import com.awakeguard.alarm.data.MissionType
import com.awakeguard.alarm.mission.MathMissionEngine
import com.awakeguard.alarm.mission.MemoryMissionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEngineTest {

    @Test
    fun testMathMissionEngineGeneratesProblemsAndValidates() {
        val engine = MathMissionEngine(totalProblems = 2, difficulty = "EASY")
        assertEquals(2, engine.getTotalProblems())
        assertEquals(2, engine.getRemainingProblems())
        assertFalse(engine.isCompleted())

        val problem1 = engine.getCurrentProblem()
        assertNotNull(problem1.expression)

        // Resposta errada não avança
        val wrongAns = problem1.correctAnswer + 999
        val wrongResult = engine.submitAnswer(wrongAns)
        assertFalse(wrongResult)
        assertEquals(2, engine.getRemainingProblems())

        // Resposta certa avança
        val rightResult1 = engine.submitAnswer(problem1.correctAnswer)
        assertTrue(rightResult1)
        assertEquals(1, engine.getRemainingProblems())

        // Segundo problema
        val problem2 = engine.getCurrentProblem()
        val rightResult2 = engine.submitAnswer(problem2.correctAnswer)
        assertTrue(rightResult2)
        assertEquals(0, engine.getRemainingProblems())
        assertTrue(engine.isCompleted())
    }

    @Test
    fun testMemoryMissionEngine() {
        val engine = MemoryMissionEngine(sequenceLength = 3)
        assertEquals(3, engine.getSequenceLength())
        assertEquals(0, engine.getCurrentStep())
        assertFalse(engine.isCompleted())

        val sequence = engine.getSequence()
        assertEquals(3, sequence.size)

        // Testa acerto sequencial
        assertTrue(engine.handleBlockClick(sequence[0]))
        assertEquals(1, engine.getCurrentStep())

        assertTrue(engine.handleBlockClick(sequence[1]))
        assertEquals(2, engine.getCurrentStep())

        assertTrue(engine.handleBlockClick(sequence[2]))
        assertTrue(engine.isCompleted())
    }

    @Test
    fun testTypingMissionEngine() {
        val phrase = "Vença a cama hoje para vencer seus objetivos amanhã."
        val engine = com.awakeguard.alarm.mission.TypingMissionEngine(phrase)

        assertEquals(phrase, engine.getTarget())

        val initial = engine.checkProgress("")
        assertTrue(initial.isPrefixValid)
        assertEquals(0, initial.progressPercent)
        assertFalse(initial.isCompleted)

        val partial = engine.checkProgress("Vença a cama")
        assertTrue(partial.isPrefixValid)
        assertTrue(partial.progressPercent > 0)
        assertFalse(partial.isCompleted)

        val wrong = engine.checkProgress("Vença a sopa")
        assertFalse(wrong.isPrefixValid)

        val complete = engine.checkProgress(phrase)
        assertTrue(complete.isPrefixValid)
        assertEquals(100, complete.progressPercent)
        assertTrue(complete.isCompleted)
    }
}

