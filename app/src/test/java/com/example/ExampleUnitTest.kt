package com.example

import com.example.audio.AudioSynthesizer
import com.example.model.ColumnMapping
import com.example.model.CsvParser
import com.example.model.McqQuestion
import com.example.model.TimingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMcqQuestionCorrectAnswerIndex() {
        val q1 = McqQuestion(
            id = 1,
            questionText = "Sample Question",
            optionA = "Apple",
            optionB = "Banana",
            optionC = "Cherry",
            optionD = "Date",
            correctAnswer = "B"
        )
        assertEquals(1, q1.correctIndex())

        val q2 = q1.copy(correctAnswer = "C")
        assertEquals(2, q2.correctIndex())

        val q3 = q1.copy(correctAnswer = "Date")
        assertEquals(3, q3.correctIndex())
    }

    @Test
    fun testCsvParserWithCommasAndQuotes() {
        val csv = """
            Question,Option A,Option B,Option C,Option D,Answer,Explanation
            "What is the first-line drug for hypertension in non-black patients?","Lisinopril","Metoprolol","Furosemide","Clonidine","A","ACE inhibitors such as Lisinopril are first-line."
            "Which heart sound corresponds to atrial contraction?","S1","S2","S3","S4","D","S4 is an atrial gallop."
        """.trimIndent()

        val parsed = CsvParser.parse(csv)
        assertEquals(2, parsed.size)
        assertEquals("What is the first-line drug for hypertension in non-black patients?", parsed[0].questionText)
        assertEquals("Lisinopril", parsed[0].optionA)
        assertEquals("A", parsed[0].correctAnswer)
        assertEquals(0, parsed[0].correctIndex())
        assertEquals("S4", parsed[1].optionD)
        assertEquals(3, parsed[1].correctIndex())
    }

    @Test
    fun testCsvParserWithTabs() {
        val tsv = "Question\tOption A\tOption B\tOption C\tOption D\tAnswer\tExplanation\nWhat is normal cardiac output?\t2-3 L/min\t4-8 L/min\t10-12 L/min\t15 L/min\tB\tResting CO is 4-8 L/min."
        val parsed = CsvParser.parse(tsv)
        assertEquals(1, parsed.size)
        assertEquals("What is normal cardiac output?", parsed[0].questionText)
        assertEquals("4-8 L/min", parsed[0].optionB)
        assertEquals(1, parsed[0].correctIndex())
    }

    @Test
    fun testTimingConfigCalculations() {
        val timing = TimingConfig(
            countdownSeconds = 5f,
            answerHoldSeconds = 3f,
            transitionSeconds = 0.6f,
            fps = 30
        )
        assertEquals(8.6f, timing.totalQuestionDuration, 0.001f)
        assertEquals(258, timing.totalFramesPerQuestion)
    }

    @Test
    fun testAudioSynthesizerSampleGeneration() {
        val tick = AudioSynthesizer.generateTickPcm(AudioSynthesizer.SAMPLE_RATE, 0.8f)
        assertTrue(tick.isNotEmpty())

        val chime = AudioSynthesizer.generateChimePcm(AudioSynthesizer.SAMPLE_RATE, 1.0f)
        assertTrue(chime.isNotEmpty())
        assertTrue(chime.size > tick.size)

        val whoosh = AudioSynthesizer.generateWhooshPcm(AudioSynthesizer.SAMPLE_RATE, 0.7f)
        assertTrue(whoosh.isNotEmpty())
    }
}
