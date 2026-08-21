package com.secondbrain.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.core.model.MemoryType
import java.time.Instant

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val summary: String?,
    val type: String,
    val importance: Float,
    val confidence: Float,
    val sourceId: String?,
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
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)
