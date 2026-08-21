package com.secondbrain.app.data.repository

import com.secondbrain.app.core.model.RawCapture
import com.secondbrain.app.data.local.RawCaptureDao
import com.secondbrain.app.data.local.toDomain
import com.secondbrain.app.data.local.toEntity
import com.secondbrain.app.domain.capture.CaptureRepository

class LocalCaptureRepository(
    private val rawCaptureDao: RawCaptureDao
) : CaptureRepository {
    override suspend fun saveCapture(capture: RawCapture) {
        rawCaptureDao.upsert(capture.toEntity())
    }

    override suspend fun getCapture(id: String): RawCapture? =
        rawCaptureDao.getById(id)?.toDomain()
}
