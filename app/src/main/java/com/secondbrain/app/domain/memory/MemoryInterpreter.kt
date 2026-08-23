package com.secondbrain.app.domain.memory

import com.secondbrain.app.core.model.DayPart
import com.secondbrain.app.core.model.MemoryType
import com.secondbrain.app.core.model.TemporalContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Lightweight local interpreter used before the AI reasoning layer.
 * It classifies memories and extracts common Spanish temporal expressions.
 */
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
        val temporal = extractTemporalContext(lower, today)

        val type = when {
            containsAny(lower, "tengo que", "debo ", "necesito ", "hay que", "recordar ", "acuérdame", "acuerdame", "pendiente") -> MemoryType.TASK
            containsAny(lower, "decidí", "decidi", "decidimos", "queda decidido", "definitivamente", "vamos a hacer") -> MemoryType.DECISION
            containsAny(lower, "idea:", "se me ocurrió", "se me ocurrio", "podríamos", "podriamos", "quizás podríamos", "quizas podriamos") -> MemoryType.IDEA
            containsAny(lower, "quiero lograr", "mi objetivo", "meta:") -> MemoryType.GOAL
            normalized.endsWith("?") || containsAny(lower, "pregunta:", "me pregunto") -> MemoryType.QUESTION
            containsAny(lower, "prefiero", "me gusta", "no me gusta", "odio ", "me encanta") -> MemoryType.PREFERENCE
            temporal != null -> MemoryType.EVENT
            else -> MemoryType.OBSERVATION
        }

        val importance = when (type) {
            MemoryType.TASK, MemoryType.DECISION, MemoryType.GOAL -> 0.8f
            MemoryType.EVENT -> 0.75f
            MemoryType.IDEA, MemoryType.QUESTION -> 0.65f
            else -> 0.5f
        }

        return Interpretation(
            type = type,
            summary = buildSummary(normalized, type),
            importance = importance,
            confidence = if (type == MemoryType.OBSERVATION) 0.55f else 0.82f,
            temporalContext = temporal
        )
    }

    private fun extractTemporalContext(text: String, today: LocalDate): TemporalContext? {
        val dayPart = extractDayPart(text)

        val relative = when {
            Regex("\\bpasado mañana\\b").containsMatchIn(text) -> today.plusDays(2) to "pasado mañana"
            Regex("\\bmañana\\b").containsMatchIn(text) -> today.plusDays(1) to "mañana"
            Regex("\\bhoy\\b").containsMatchIn(text) -> today to "hoy"
            else -> null
        }
        if (relative != null) {
            return TemporalContext(
                startDate = relative.first,
                dayPart = dayPart,
                sourceExpression = relative.second + dayPartSuffix(dayPart)
            )
        }

        val rangeRegex = Regex("\\b(\\d{1,2})\\s+y\\s+(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?\\b")
        rangeRegex.find(text)?.let { match ->
            val firstDay = match.groupValues[1].toInt()
            val secondDay = match.groupValues[2].toInt()
            val month = monthFromSpanish(match.groupValues[3]) ?: return@let
            val explicitYear = match.groupValues[4].toIntOrNull()
            val start = resolveDate(firstDay, month, explicitYear, today) ?: return@let
            val endYear = explicitYear ?: start.year
            val end = runCatching { LocalDate.of(endYear, month, secondDay) }.getOrNull() ?: return@let
            return TemporalContext(start, end, dayPart, match.value + dayPartSuffix(dayPart))
        }

        val explicitRegex = Regex("\\b(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?\\b")
        explicitRegex.find(text)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = monthFromSpanish(match.groupValues[2]) ?: return@let
            val year = match.groupValues[3].toIntOrNull()
            val date = resolveDate(day, month, year, today) ?: return@let
            return TemporalContext(date, dayPart = dayPart, sourceExpression = match.value + dayPartSuffix(dayPart))
        }

        val numericRegex = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
        numericRegex.find(text)?.let { match ->
            val day = match.groupValues[1].toInt()
            val monthNumber = match.groupValues[2].toInt()
            val rawYear = match.groupValues[3].toIntOrNull()
            val year = rawYear?.let { if (it < 100) 2000 + it else it }
            val month = runCatching { Month.of(monthNumber) }.getOrNull() ?: return@let
            val date = resolveDate(day, month, year, today) ?: return@let
            return TemporalContext(date, dayPart = dayPart, sourceExpression = match.value + dayPartSuffix(dayPart))
        }

        val weekday = weekdayFromSpanish(text)
        if (weekday != null) {
            val date = today.with(TemporalAdjusters.nextOrSame(weekday.first))
            return TemporalContext(date, dayPart = dayPart, sourceExpression = weekday.second + dayPartSuffix(dayPart))
        }

        return null
    }

    private fun resolveDate(day: Int, month: Month, explicitYear: Int?, today: LocalDate): LocalDate? {
        if (explicitYear != null) return runCatching { LocalDate.of(explicitYear, month, day) }.getOrNull()
        val thisYear = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
    }

    private fun extractDayPart(text: String): DayPart? = when {
        containsAny(text, "temprano", "en la mañana", "por la mañana") -> DayPart.MORNING
        containsAny(text, "en la tarde", "por la tarde") -> DayPart.AFTERNOON
        containsAny(text, "en la noche", "por la noche", "esta noche") -> DayPart.NIGHT
        containsAny(text, "al atardecer", "en la tarde-noche") -> DayPart.EVENING
        else -> null
    }

    private fun weekdayFromSpanish(text: String): Pair<DayOfWeek, String>? {
        val days = listOf(
            Triple("lunes", DayOfWeek.MONDAY, "lunes"),
            Triple("martes", DayOfWeek.TUESDAY, "martes"),
            Triple("miércoles", DayOfWeek.WEDNESDAY, "miércoles"),
            Triple("miercoles", DayOfWeek.WEDNESDAY, "miercoles"),
            Triple("jueves", DayOfWeek.THURSDAY, "jueves"),
            Triple("viernes", DayOfWeek.FRIDAY, "viernes"),
            Triple("sábado", DayOfWeek.SATURDAY, "sábado"),
            Triple("sabado", DayOfWeek.SATURDAY, "sabado"),
            Triple("domingo", DayOfWeek.SUNDAY, "domingo")
        )
        return days.firstOrNull { Regex("\\b${it.first}\\b").containsMatchIn(text) }
            ?.let { it.second to it.third }
    }

    private fun monthFromSpanish(value: String): Month? = when (value) {
        "enero" -> Month.JANUARY
        "febrero" -> Month.FEBRUARY
        "marzo" -> Month.MARCH
        "abril" -> Month.APRIL
        "mayo" -> Month.MAY
        "junio" -> Month.JUNE
        "julio" -> Month.JULY
        "agosto" -> Month.AUGUST
        "septiembre", "setiembre" -> Month.SEPTEMBER
        "octubre" -> Month.OCTOBER
        "noviembre" -> Month.NOVEMBER
        "diciembre" -> Month.DECEMBER
        else -> null
    }

    private fun dayPartSuffix(dayPart: DayPart?): String = when (dayPart) {
        DayPart.MORNING -> " · mañana"
        DayPart.AFTERNOON -> " · tarde"
        DayPart.EVENING -> " · tarde-noche"
        DayPart.NIGHT -> " · noche"
        null -> ""
    }

    private fun buildSummary(text: String, type: MemoryType): String? {
        if (text.length <= 72) return null
        val clipped = text.take(96).trimEnd()
        return "${type.displayName}: $clipped${if (text.length > 96) "…" else ""}"
    }

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any { text.contains(it) }

    private val MemoryType.displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
