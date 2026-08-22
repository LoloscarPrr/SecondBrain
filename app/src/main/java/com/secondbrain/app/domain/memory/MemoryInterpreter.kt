package com.secondbrain.app.domain.memory

import com.secondbrain.app.core.model.MemoryType
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId

/**
 * Lightweight, deterministic interpreter for the first alpha.
 * It gives SecondBrain useful structure before a cloud/local LLM is connected.
 */
class MemoryInterpreter {

    data class Interpretation(
        val type: MemoryType,
        val summary: String?,
        val importance: Float,
        val confidence: Float
    )

    fun interpret(text: String): Interpretation {
        val normalized = text.trim()
        val lower = normalized.lowercase()

        val type = when {
            containsAny(lower, "tengo que", "debo ", "necesito ", "hay que", "recordar ", "acuérdame", "pendiente") -> MemoryType.TASK
            containsAny(lower, "decidí", "decidimos", "queda decidido", "definitivamente", "vamos a hacer") -> MemoryType.DECISION
            containsAny(lower, "idea:", "se me ocurrió", "podríamos", "podriamos", "quizás podríamos", "quizas podriamos") -> MemoryType.IDEA
            containsAny(lower, "quiero lograr", "mi objetivo", "meta:") -> MemoryType.GOAL
            normalized.endsWith("?") || containsAny(lower, "pregunta:", "me pregunto") -> MemoryType.QUESTION
            containsAny(lower, "prefiero", "me gusta", "no me gusta", "odio ", "me encanta") -> MemoryType.PREFERENCE
            containsExplicitDate(lower) -> MemoryType.EVENT
            else -> MemoryType.OBSERVATION
        }

        val importance = when (type) {
            MemoryType.TASK, MemoryType.DECISION, MemoryType.GOAL -> 0.8f
            MemoryType.EVENT -> 0.75f
            MemoryType.IDEA, MemoryType.QUESTION -> 0.65f
            else -> 0.5f
        }

        val summary = buildSummary(normalized, type)

        return Interpretation(
            type = type,
            summary = summary,
            importance = importance,
            confidence = if (type == MemoryType.OBSERVATION) 0.55f else 0.82f
        )
    }

    private fun buildSummary(text: String, type: MemoryType): String? {
        if (text.length <= 72) return null
        val clipped = text.take(96).trimEnd()
        return "${type.displayName}: $clipped${if (text.length > 96) "…" else ""}"
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }

    private fun containsExplicitDate(text: String): Boolean {
        val months = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "setiembre", "octubre", "noviembre", "diciembre"
        )
        val hasMonth = months.any { text.contains(it) }
        val hasNumericDate = Regex("\\b\\d{1,2}[/-]\\d{1,2}([/-]\\d{2,4})?\\b").containsMatchIn(text)
        return hasMonth || hasNumericDate
    }

    private val MemoryType.displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
