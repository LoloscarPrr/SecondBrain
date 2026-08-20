package com.secondbrain.app.core.model

import java.time.Instant
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

data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val summary: String? = null,
    val type: MemoryType,
    val importance: Float = 0.5f,
    val confidence: Float = 1.0f,
    val sourceId: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt
)
