package com.secondbrain.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secondbrain.app.core.model.CaptureType
import com.secondbrain.app.core.model.ProcessingState
import com.secondbrain.app.core.model.RawCapture
import java.time.Instant

@Entity(tableName = "raw_captures")
data class RawCaptureEntity(
    @PrimaryKey val id: String,
    val type: String,
    val rawText: String?,
    val uri: String?,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
    val processedAtEpochMillis: Long?,
    val processingState: String
)

fun RawCaptureEntity.toDomain(): RawCapture = RawCapture(
    id = id,
    type = CaptureType.valueOf(type),
    rawText = rawText,
    uri = uri,
    mimeType = mimeType,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    processedAt = processedAtEpochMillis?.let(Instant::ofEpochMilli),
    processingState = ProcessingState.valueOf(processingState)
)

fun RawCapture.toEntity(): RawCaptureEntity = RawCaptureEntity(
    id = id,
    type = type.name,
    rawText = rawText,
    uri = uri,
    mimeType = mimeType,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    processedAtEpochMillis = processedAt?.toEpochMilli(),
    processingState = processingState.name
)
