package com.secondbrain.app.domain.memory

import com.secondbrain.app.core.model.DayPart
import com.secondbrain.app.core.model.MemoryType
import com.secondbrain.app.core.model.TemporalContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Local interpreter used before the AI reasoning layer. */
class MemoryInterpreter {
    data class Interpretation(
        val type: MemoryType,
        val summary: String?,
        val importance: Float,
        val confidence: Float,
        val temporalContext: TemporalContext?
    )

    fun interpret(text: String, today: LocalDate = LocalDate.now()): Interpretation {
        val normalized = text.trim()
        val lower = normalized.lowercase()
        val typeHint = classify(lower, normalized)
        val temporal = extractTemporalContext(lower, today, typeHint == MemoryType.TASK)
        val type = if (typeHint == MemoryType.OBSERVATION && temporal != null) MemoryType.EVENT else typeHint
        val importance = when (type) {
            MemoryType.TASK, MemoryType.DECISION, MemoryType.GOAL -> 0.8f
            MemoryType.EVENT -> 0.75f
            MemoryType.IDEA, MemoryType.QUESTION -> 0.65f
            else -> 0.5f
        }
        return Interpretation(type, buildSummary(normalized, type), importance,
            if (type == MemoryType.OBSERVATION) 0.55f else 0.82f, temporal)
    }

    private fun classify(text: String, original: String): MemoryType = when {
        containsAny(text, "tengo que", "debo ", "necesito ", "hay que", "recordar ", "recordarle", "acuérdame", "acuerdame", "pendiente", "pagar ") -> MemoryType.TASK
        containsAny(text, "decidí", "decidi", "decidimos", "queda decidido", "definitivamente", "vamos a hacer") -> MemoryType.DECISION
        containsAny(text, "idea:", "se me ocurrió", "se me ocurrio", "podríamos", "podriamos") -> MemoryType.IDEA
        containsAny(text, "quiero lograr", "mi objetivo", "meta:") -> MemoryType.GOAL
        original.endsWith("?") || containsAny(text, "pregunta:", "me pregunto") -> MemoryType.QUESTION
        containsAny(text, "prefiero", "me gusta", "no me gusta", "odio ", "me encanta") -> MemoryType.PREFERENCE
        else -> MemoryType.OBSERVATION
    }

    private fun extractTemporalContext(text: String, today: LocalDate, task: Boolean): TemporalContext? {
        val dayPart = extractDayPart(text)

        // For tasks, a leading weekday+day is the action date. This must win over
        // later dates that merely describe what the task is about.
        if (task) {
            val actionDate = Regex("\\b(lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)\\s+(?:el\\s+)?(\\d{1,2})\\b")
                .find(text)
            actionDate?.let { match ->
                val expectedWeekday = dayOfWeek(match.groupValues[1]) ?: return@let
                val day = match.groupValues[2].toIntOrNull() ?: return@let
                val date = resolveNextDayOfMonth(day, today, expectedWeekday) ?: return@let
                return TemporalContext(date, dayPart = dayPart, sourceExpression = match.value + dayPartSuffix(dayPart))
            }

            // A date phrase before the task verb is normally the scheduling date.
            val verbIndex = listOf("debo ", "tengo que", "necesito ", "recordar ", "recordarle", "pagar ")
                .map { text.indexOf(it) }.filter { it >= 0 }.minOrNull()
            if (verbIndex != null && verbIndex > 0) {
                extractSingleTemporal(text.substring(0, verbIndex), today, dayPart)?.let { return it }
            }
        }

        return extractSingleTemporal(text, today, dayPart)
    }

    private fun extractSingleTemporal(text: String, today: LocalDate, dayPart: DayPart?): TemporalContext? {
        val relative = when {
            Regex("\\bpasado mañana\\b").containsMatchIn(text) -> today.plusDays(2) to "pasado mañana"
            Regex("\\bmañana\\b").containsMatchIn(text) -> today.plusDays(1) to "mañana"
            Regex("\\bhoy\\b").containsMatchIn(text) -> today to "hoy"
            else -> null
        }
        if (relative != null) return TemporalContext(relative.first, dayPart = dayPart, sourceExpression = relative.second + dayPartSuffix(dayPart))

        val rangeRegex = Regex("\\b(?:el\\s+)?(\\d{1,2})\\s+y\\s+(?:el\\s+)?(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?\\b")
        rangeRegex.find(text)?.let { m ->
            val month = monthFromSpanish(m.groupValues[3]) ?: return@let
            val start = resolveDate(m.groupValues[1].toInt(), month, m.groupValues[4].toIntOrNull(), today) ?: return@let
            val end = runCatching { LocalDate.of(start.year, month, m.groupValues[2].toInt()) }.getOrNull() ?: return@let
            return TemporalContext(start, end, dayPart, m.value + dayPartSuffix(dayPart))
        }

        val explicitRegex = Regex("\\b(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?\\b")
        explicitRegex.find(text)?.let { m ->
            val month = monthFromSpanish(m.groupValues[2]) ?: return@let
            val date = resolveDate(m.groupValues[1].toInt(), month, m.groupValues[3].toIntOrNull(), today) ?: return@let
            return TemporalContext(date, dayPart = dayPart, sourceExpression = m.value + dayPartSuffix(dayPart))
        }

        val numericRegex = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
        numericRegex.find(text)?.let { m ->
            val month = runCatching { Month.of(m.groupValues[2].toInt()) }.getOrNull() ?: return@let
            val rawYear = m.groupValues[3].toIntOrNull()
            val year = rawYear?.let { if (it < 100) 2000 + it else it }
            val date = resolveDate(m.groupValues[1].toInt(), month, year, today) ?: return@let
            return TemporalContext(date, dayPart = dayPart, sourceExpression = m.value + dayPartSuffix(dayPart))
        }

        val weekday = weekdayFromSpanish(text)
        if (weekday != null) {
            val date = today.with(TemporalAdjusters.nextOrSame(weekday.first))
            return TemporalContext(date, dayPart = dayPart, sourceExpression = weekday.second + dayPartSuffix(dayPart))
        }

        val shortDayRegex = Regex("\\b((?:para\\s+)?el|este|pr[oó]ximo)\\s+(\\d{1,2})\\b")
        shortDayRegex.find(text)?.let { m ->
            val day = m.groupValues[2].toIntOrNull() ?: return@let
            if (day !in 1..31) return@let
            val date = resolveNextDayOfMonth(day, today) ?: return@let
            return TemporalContext(date, dayPart = dayPart, sourceExpression = m.value + dayPartSuffix(dayPart))
        }
        return null
    }

    private fun resolveNextDayOfMonth(day: Int, today: LocalDate, expectedWeekday: DayOfWeek? = null): LocalDate? {
        var month = YearMonth.from(today)
        repeat(24) {
            if (day <= month.lengthOfMonth()) {
                val candidate = month.atDay(day)
                if (!candidate.isBefore(today) && (expectedWeekday == null || candidate.dayOfWeek == expectedWeekday)) return candidate
            }
            month = month.plusMonths(1)
        }
        return null
    }

    private fun resolveDate(day: Int, month: Month, explicitYear: Int?, today: LocalDate): LocalDate? {
        if (explicitYear != null) return runCatching { LocalDate.of(explicitYear, month, day) }.getOrNull()
        val thisYear = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
    }

    private fun dayOfWeek(value: String): DayOfWeek? = when (value.replace('é','e').replace('á','a')) {
        "lunes" -> DayOfWeek.MONDAY; "martes" -> DayOfWeek.TUESDAY; "miercoles" -> DayOfWeek.WEDNESDAY
        "jueves" -> DayOfWeek.THURSDAY; "viernes" -> DayOfWeek.FRIDAY; "sabado" -> DayOfWeek.SATURDAY; "domingo" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun weekdayFromSpanish(text: String): Pair<DayOfWeek, String>? {
        val values = listOf("lunes","martes","miércoles","miercoles","jueves","viernes","sábado","sabado","domingo")
        return values.firstOrNull { Regex("\\b$it\\b").containsMatchIn(text) }?.let { dayOfWeek(it)!! to it }
    }

    private fun extractDayPart(text: String): DayPart? = when {
        containsAny(text, "temprano", "en la mañana", "por la mañana") -> DayPart.MORNING
        containsAny(text, "en la tarde", "por la tarde") -> DayPart.AFTERNOON
        containsAny(text, "en la noche", "por la noche", "esta noche") -> DayPart.NIGHT
        containsAny(text, "al atardecer", "en la tarde-noche") -> DayPart.EVENING
        else -> null
    }

    private fun monthFromSpanish(v: String): Month? = when (v) {
        "enero" -> Month.JANUARY; "febrero" -> Month.FEBRUARY; "marzo" -> Month.MARCH; "abril" -> Month.APRIL
        "mayo" -> Month.MAY; "junio" -> Month.JUNE; "julio" -> Month.JULY; "agosto" -> Month.AUGUST
        "septiembre", "setiembre" -> Month.SEPTEMBER; "octubre" -> Month.OCTOBER; "noviembre" -> Month.NOVEMBER; "diciembre" -> Month.DECEMBER
        else -> null
    }

    private fun dayPartSuffix(p: DayPart?): String = when (p) {
        DayPart.MORNING -> " · mañana"; DayPart.AFTERNOON -> " · tarde"; DayPart.EVENING -> " · tarde-noche"; DayPart.NIGHT -> " · noche"; null -> ""
    }

    private fun buildSummary(text: String, type: MemoryType): String? {
        if (text.length <= 72) return null
        val clipped = text.take(96).trimEnd()
        return "${type.name.lowercase().replaceFirstChar { it.uppercase() }}: $clipped${if (text.length > 96) "…" else ""}"
    }

    private fun containsAny(text: String, vararg terms: String) = terms.any { text.contains(it) }
}
