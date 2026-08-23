package com.secondbrain.app.domain.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.secondbrain.app.core.model.CaptureType
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.core.model.MemoryType
import com.secondbrain.app.core.model.ProcessingState
import com.secondbrain.app.core.model.RawCapture
import com.secondbrain.app.core.model.TemporalContext
import com.secondbrain.app.domain.memory.MemoryRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SaveImageCaptureUseCase(
    private val context: Context,
    private val captureRepository: CaptureRepository,
    private val memoryRepository: MemoryRepository,
    private val billInterpreter: ImageBillInterpreter = ImageBillInterpreter()
) {
    suspend operator fun invoke(uri: Uri) {
        val capture = RawCapture(
            type = CaptureType.IMAGE,
            uri = uri.toString(),
            mimeType = context.contentResolver.getType(uri),
            processingState = ProcessingState.PROCESSING
        )
        captureRepository.saveCapture(capture)

        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
            }
            recognizer.close()

            val ocrText = result.text.trim()
            val items = billInterpreter.extract(ocrText)
            if (items.isNotEmpty()) {
                items.forEach { item ->
                    val amount = "$" + "%,d".format(item.amount).replace(',', '.')
                    memoryRepository.saveMemory(
                        Memory(
                            content = "Pagar ${item.label}: $amount",
                            type = MemoryType.TASK,
                            importance = 0.85f,
                            confidence = 0.86f,
                            sourceId = capture.id,
                            temporalContext = item.dueDate?.let { TemporalContext(startDate = it, sourceExpression = "vencimiento detectado en imagen") }
                        )
                    )
                }
            } else {
                memoryRepository.saveMemory(
                    Memory(
                        content = if (ocrText.isBlank()) "Imagen guardada sin texto reconocible" else ocrText,
                        summary = "Texto extraído de imagen",
                        type = MemoryType.OBSERVATION,
                        importance = 0.5f,
                        confidence = 0.65f,
                        sourceId = capture.id
                    )
                )
            }
            captureRepository.saveCapture(capture.copy(rawText = ocrText, processedAt = java.time.Instant.now(), processingState = ProcessingState.PROCESSED))
        } catch (error: Throwable) {
            captureRepository.saveCapture(capture.copy(processingState = ProcessingState.FAILED))
            throw error
        }
    }
}
