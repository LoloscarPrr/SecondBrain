package com.secondbrain.app.domain.capture

import com.secondbrain.app.core.model.CaptureType
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.core.model.MemoryType
import com.secondbrain.app.core.model.ProcessingState
import com.secondbrain.app.core.model.RawCapture
import com.secondbrain.app.domain.memory.MemoryRepository
import java.time.Instant

class SaveTextCaptureUseCase(
    private val captureRepository: CaptureRepository,
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(text: String) {
        val normalizedText = text.trim()
        require(normalizedText.isNotEmpty()) { "Capture text cannot be empty." }

        val capture = RawCapture(
            type = CaptureType.TEXT,
            rawText = normalizedText,
            processingState = ProcessingState.PENDING
        )
        captureRepository.saveCapture(capture)

        val processingCapture = capture.copy(processingState = ProcessingState.PROCESSING)
        captureRepository.saveCapture(processingCapture)

        val memory = Memory(
            content = normalizedText,
            type = MemoryType.OBSERVATION,
            sourceId = capture.id
        )
        memoryRepository.saveMemory(memory)

        captureRepository.saveCapture(
            processingCapture.copy(
                processedAt = Instant.now(),
                processingState = ProcessingState.PROCESSED
            )
        )
    }
}
