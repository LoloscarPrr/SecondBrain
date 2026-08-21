package com.secondbrain.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.domain.capture.SaveTextCaptureUseCase
import com.secondbrain.app.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    memoryRepository: MemoryRepository,
    private val saveTextCapture: SaveTextCaptureUseCase
) : ViewModel() {
    val memories: StateFlow<List<Memory>> = memoryRepository
        .observeMemories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    var captureText: String = ""
        private set

    var isSaving: Boolean = false
        private set

    fun updateCaptureText(value: String) {
        captureText = value
    }

    fun saveCapture(onStateChanged: () -> Unit) {
        val text = captureText.trim()
        if (text.isEmpty() || isSaving) return

        isSaving = true
        onStateChanged()
        viewModelScope.launch {
            runCatching { saveTextCapture(text) }
                .onSuccess { captureText = "" }
            isSaving = false
            onStateChanged()
        }
    }

    class Factory(
        private val memoryRepository: MemoryRepository,
        private val saveTextCapture: SaveTextCaptureUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(memoryRepository, saveTextCapture) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
