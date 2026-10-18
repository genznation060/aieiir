package com.example.model

data class McqQuestion(
    val id: Int,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D" (or matching text)
    val explanation: String = "",
    val imageUrl: String? = null,
    val category: String = "CLINICAL MEDICINE"
) {
    /**
     * Resolves the correct option index (0 for A, 1 for B, 2 for C, 3 for D).
     */
    fun correctIndex(): Int {
        val trimmed = correctAnswer.trim().uppercase()
        return when {
            trimmed.startsWith("A") || trimmed == optionA.trim().uppercase() -> 0
            trimmed.startsWith("B") || trimmed == optionB.trim().uppercase() -> 1
            trimmed.startsWith("C") || trimmed == optionC.trim().uppercase() -> 2
            trimmed.startsWith("D") || trimmed == optionD.trim().uppercase() -> 3
            else -> 0
        }
    }

    fun getOption(index: Int): String {
        return when (index) {
            0 -> optionA
            1 -> optionB
            2 -> optionC
            3 -> optionD
            else -> ""
        }
    }

    companion object {
        val SAMPLE_QUESTIONS = listOf(
            McqQuestion(
                id = 1,
                category = "CARDIOLOGY",
                questionText = "A 58-year-old male presents with crushing retrosternal chest pain radiating to the jaw. ECG reveals ST elevation in leads II, III, and aVF. Which coronary artery is most likely occluded?",
                optionA = "Left Anterior Descending (LAD)",
                optionB = "Right Coronary Artery (RCA)",
                optionC = "Left Circumflex Artery (LCx)",
                optionD = "Left Main Coronary Artery (LMCA)",
                correctAnswer = "B",
                explanation = "ST elevation in II, III, and aVF indicates an inferior wall myocardial infarction, which is supplied by the Right Coronary Artery (RCA) in approximately 85-90% of individuals."
            ),
            McqQuestion(
                id = 2,
                category = "PHARMACOLOGY",
                questionText = "Which pharmacological agent acts primarily by inhibiting the Na+/K+ ATPase pump, thereby increasing intracellular calcium and myocardial contractility?",
                optionA = "Digoxin",
                optionB = "Metoprolol",
                optionC = "Amiodarone",
                optionD = "Lisinopril",
                correctAnswer = "A",
                explanation = "Digoxin binds reversibly to the alpha subunit of Na+/K+-ATPase, leading to increased intracellular Na+, secondary activation of Na+/Ca2+ exchanger, and positive inotropic effect."
            ),
            McqQuestion(
                id = 3,
                category = "NEUROLOGY",
                questionText = "A 65-year-old female presents with sudden onset right-sided hemiplegia, right facial droop, and expressive motor aphasia. Where is the most probable lesion?",
                optionA = "Right Posterior Cerebral Artery",
                optionB = "Left Middle Cerebral Artery (Superior Division)",
                optionC = "Vertebrobasilar Artery",
                optionD = "Left Anterior Cerebral Artery",
                correctAnswer = "B",
                explanation = "Broca's expressive motor aphasia combined with contralateral face and arm weakness is classic for an occlusion of the superior division of the dominant (left) Middle Cerebral Artery."
            ),
            McqQuestion(
                id = 4,
                category = "HEMATOLOGY",
                questionText = "A 24-year-old female presents with fatigue and microcytic hypochromic anemia. Lab tests show elevated ferritin and decreased total iron binding capacity (TIBC). What is the diagnosis?",
                optionA = "Iron Deficiency Anemia",
                optionB = "Anemia of Chronic Disease",
                optionC = "Thalassemia Minor",
                optionD = "Sideroblastic Anemia",
                correctAnswer = "B",
                explanation = "Anemia of Chronic Disease is mediated by hepcidin elevation, which traps iron inside macrophage stores—manifesting as elevated/normal ferritin and low TIBC."
            ),
            McqQuestion(
                id = 5,
                category = "PULMONOLOGY",
                questionText = "Which physical exam finding is most characteristically heard on auscultation over an area of complete lung consolidation?",
                optionA = "Vesicular breath sounds with wheezing",
                optionB = "Bronchial breath sounds and egophony ('E' to 'A')",
                optionC = "Decreased tactile fremitus and hyperresonance",
                optionD = "Inspiratory stridor with tracheal deviation",
                correctAnswer = "B",
                explanation = "Lobar consolidation conducts high-frequency tubular breath sounds directly to the chest wall, creating bronchial breath sounds, increased tactile fremitus, and egophony."
            )
        )
    }
}

/**
 * Robust CSV/TSV parser supporting quotes, commas, tabs, semicolons.
 */
object CsvParser {
    fun parse(
        content: String,
        mapping: ColumnMapping = ColumnMapping()
    ): List<McqQuestion> {
        val lines = splitCsvLines(content)
        if (lines.isEmpty()) return emptyList()

        val firstRow = parseCsvRow(lines[0])
        val hasHeader = isHeaderRow(firstRow)
        val dataRows = if (hasHeader) lines.drop(1) else lines

        val effectiveMapping = if (hasHeader) {
            autoDetectMapping(firstRow, mapping)
        } else {
            mapping
        }

        val questions = mutableListOf<McqQuestion>()
        var idCounter = 1

        for (line in dataRows) {
            if (line.isBlank()) continue
            val cols = parseCsvRow(line)
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            fun getCol(idx: Int): String = if (idx in cols.indices) cols[idx].trim() else ""

            val qText = getCol(effectiveMapping.questionIndex)
            val optA = getCol(effectiveMapping.optionAIndex)
            val optB = getCol(effectiveMapping.optionBIndex)
            val optC = getCol(effectiveMapping.optionCIndex)
            val optD = getCol(effectiveMapping.optionDIndex)
            val ans = getCol(effectiveMapping.correctAnswerIndex)
            val exp = getCol(effectiveMapping.explanationIndex)
            val cat = if (effectiveMapping.categoryIndex >= 0) getCol(effectiveMapping.categoryIndex) else "QUIZ"
            val img = if (effectiveMapping.imageIndex >= 0) getCol(effectiveMapping.imageIndex).takeIf { it.isNotBlank() } else null

            if (qText.isNotBlank()) {
                questions.add(
                    McqQuestion(
                        id = idCounter++,
                        questionText = qText,
                        optionA = optA.ifBlank { "Option A" },
                        optionB = optB.ifBlank { "Option B" },
                        optionC = optC.ifBlank { "Option C" },
                        optionD = optD.ifBlank { "Option D" },
                        correctAnswer = ans.ifBlank { "A" },
                        explanation = exp,
                        imageUrl = img,
                        category = cat.ifBlank { "CLINICAL MEDICINE" }
                    )
                )
            }
        }
        return questions
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val headerKeywords = setOf("question", "q", "option", "opt", "answer", "ans", "correct", "explanation", "exp", "category")
        return row.any { col ->
            val lower = col.lowercase().trim()
            headerKeywords.any { lower.contains(it) }
        }
    }

    fun autoDetectMapping(headers: List<String>, fallback: ColumnMapping): ColumnMapping {
        var qIdx = fallback.questionIndex
        var aIdx = fallback.optionAIndex
        var bIdx = fallback.optionBIndex
        var cIdx = fallback.optionCIndex
        var dIdx = fallback.optionDIndex
        var ansIdx = fallback.correctAnswerIndex
        var expIdx = fallback.explanationIndex
        var catIdx = fallback.categoryIndex
        var imgIdx = fallback.imageIndex

        headers.forEachIndexed { index, header ->
            val h = header.lowercase().trim()
            when {
                h.contains("quest") || h == "q" -> qIdx = index
                h.contains("opt a") || h == "option a" || h == "a" -> aIdx = index
                h.contains("opt b") || h == "option b" || h == "b" -> bIdx = index
                h.contains("opt c") || h == "option c" || h == "c" -> cIdx = index
                h.contains("opt d") || h == "option d" || h == "d" -> dIdx = index
                h.contains("correct") || h.contains("ans") || h == "key" -> ansIdx = index
                h.contains("expla") || h.contains("rationale") || h.contains("note") -> expIdx = index
                h.contains("categ") || h.contains("topic") || h.contains("subject") -> catIdx = index
                h.contains("image") || h.contains("img") || h.contains("photo") -> imgIdx = index
            }
        }

        return ColumnMapping(
            questionIndex = qIdx,
            optionAIndex = aIdx,
            optionBIndex = bIdx,
            optionCIndex = cIdx,
            optionDIndex = dIdx,
            correctAnswerIndex = ansIdx,
            explanationIndex = expIdx,
            categoryIndex = catIdx,
            imageIndex = imgIdx
        )
    }

    fun parseCsvRow(line: String): List<String> {
        val delimiter = detectDelimiter(line)
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun detectDelimiter(line: String): Char {
        val tabCount = line.count { it == '\t' }
        val commaCount = line.count { it == ',' }
        val semiCount = line.count { it == ';' }
        return when {
            tabCount > commaCount && tabCount > semiCount -> '\t'
            semiCount > commaCount && semiCount > tabCount -> ';'
            else -> ','
        }
    }

    private fun splitCsvLines(content: String): List<String> {
        val lines = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (c in content) {
            if (c == '\"') {
                inQuotes = !inQuotes
                sb.append(c)
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (sb.isNotEmpty()) {
                    lines.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) {
            lines.add(sb.toString())
        }
        return lines
    }
}

data class ColumnMapping(
    val questionIndex: Int = 0,
    val optionAIndex: Int = 1,
    val optionBIndex: Int = 2,
    val optionCIndex: Int = 3,
    val optionDIndex: Int = 4,
    val correctAnswerIndex: Int = 5,
    val explanationIndex: Int = 6,
    val categoryIndex: Int = -1,
    val imageIndex: Int = 7
)
