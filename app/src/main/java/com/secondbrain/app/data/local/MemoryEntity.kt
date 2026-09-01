package com.secondbrain.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secondbrain.app.core.model.DayPart
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.core.model.MemoryType
import com.secondbrain.app.core.model.TemporalContext
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val summary: String?,
    val type: String,
    val importance: Float,
    val confidence: Float,
    val sourceId: String?,
    val temporalStartDate: String?,
    val temporalEndDate: String?,
    val temporalDayPart: String?,
    val temporalExpression: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

fun MemoryEntity.toDomain(): Memory = Memory(
    id = id,
    content = content,
    summary = summary,
    type = MemoryType.valueOf(type),
    importance = importance,
    confidence = confidence,
    sourceId = sourceId,
    temporalContext = temporalStartDate?.let { start ->
        TemporalContext(
            startDate = LocalDate.parse(start),
            endDate = temporalEndDate?.let(LocalDate::parse),
            dayPart = temporalDayPart?.let(DayPart::valueOf),
            sourceExpression = temporalExpression.orEmpty()
        )
    },
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun Memory.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    content = content,
    summary = summary,
    type = type.name,
    importance = importance,
    confidence = confidence,
    sourceId = sourceId,
    temporalStartDate = temporalContext?.startDate?.toString(),
    temporalEndDate = temporalContext?.endDate?.toString(),
    temporalDayPart = temporalContext?.dayPart?.name,
    temporalExpression = temporalContext?.sourceExpression,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)
