package com.secondbrain.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class MemoryType {
    OBSERVATION,
    FACT,
    PREFERENCE,
    IDEA,
    DECISION,
    TASK,
    EVENT,
    GOAL,
    QUESTION,
    INSIGHT
}

enum class DayPart {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

data class TemporalContext(
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val dayPart: DayPart? = null,
    val sourceExpression: String
)

data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val summary: String? = null,
    val type: MemoryType,
    val importance: Float = 0.5f,
    val confidence: Float = 1.0f,
    val sourceId: String? = null,
    val temporalContext: TemporalContext? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt
)
