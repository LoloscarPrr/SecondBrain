package com.secondbrain.app

import android.app.Application
import com.secondbrain.app.core.diagnostics.CrashReporter
import com.secondbrain.app.data.local.SecondBrainDatabase
import com.secondbrain.app.data.repository.LocalCaptureRepository
import com.secondbrain.app.data.repository.LocalMemoryRepository
import com.secondbrain.app.domain.capture.SaveImageCaptureUseCase
import com.secondbrain.app.domain.capture.SaveTextCaptureUseCase
import com.secondbrain.app.domain.memory.MemoryRepository

class SecondBrainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize(this)
    }

    private val database by lazy { SecondBrainDatabase.getInstance(this) }

    val memoryRepository: MemoryRepository by lazy {
        LocalMemoryRepository(database.memoryDao())
    }

    private val captureRepository by lazy {
        LocalCaptureRepository(database.rawCaptureDao())
    }

    val saveTextCapture by lazy {
        SaveTextCaptureUseCase(captureRepository = captureRepository, memoryRepository = memoryRepository)
    }

    val saveImageCapture by lazy {
        SaveImageCaptureUseCase(
            context = this,
            captureRepository = captureRepository,
            memoryRepository = memoryRepository
        )
    }
}
