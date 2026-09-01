package com.secondbrain.app.domain.capture

import com.secondbrain.app.core.model.RawCapture

interface CaptureRepository {
    suspend fun saveCapture(capture: RawCapture)
    suspend fun getCapture(id: String): RawCapture?
}
