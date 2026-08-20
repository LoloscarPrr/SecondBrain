package com.secondbrain.app.core.model

import java.time.Instant
import java.util.UUID

enum class CaptureType {
    TEXT,
    VOICE,
    IMAGE,
    DOCUMENT,
    LINK
}

enum class ProcessingState {
    PENDING,
    PROCESSING,
    PROCESSED,
    PARTIAL,
    FAILED
}

data class RawCapture(
    val id: String = UUID.randomUUID().toString(),
    val type: CaptureType,
    val rawText: String? = null,
    val uri: String? = null,
    val mimeType: String? = null,
    val createdAt: Instant = Instant.now(),
    val processedAt: Instant? = null,
    val processingState: ProcessingState = ProcessingState.PENDING
)
