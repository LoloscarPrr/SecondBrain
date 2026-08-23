package com.secondbrain.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.domain.capture.SaveImageCaptureUseCase
import com.secondbrain.app.domain.capture.SaveTextCaptureUseCase
import com.secondbrain.app.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    memoryRepository: MemoryRepository,
    private val saveTextCapture: SaveTextCaptureUseCase,
    private val saveImageCapture: SaveImageCaptureUseCase
) : ViewModel() {
    val memories: StateFlow<List<Memory>> = memoryRepository.observeMemories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _captureText = MutableStateFlow("")
    val captureText: StateFlow<String> = _captureText.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateCaptureText(value: String) { _captureText.value = value }

    fun saveCapture() {
        val text = _captureText.value.trim()
        if (text.isEmpty() || _isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            runCatching { saveTextCapture(text) }
                .onSuccess { _captureText.value = "" }
                .onFailure { _statusMessage.value = "No pude guardar la memoria." }
            _isSaving.value = false
        }
    }

    fun saveImage(uri: Uri) {
        if (_isSaving.value) return
        _isSaving.value = true
        _statusMessage.value = "Analizando imagen…"
        viewModelScope.launch {
            runCatching { saveImageCapture(uri) }
                .onSuccess { _statusMessage.value = "Imagen analizada y memorias creadas." }
                .onFailure { _statusMessage.value = "No pude analizar esa imagen." }
            _isSaving.value = false
        }
    }

    class Factory(
        private val memoryRepository: MemoryRepository,
        private val saveTextCapture: SaveTextCaptureUseCase,
        private val saveImageCapture: SaveImageCaptureUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(memoryRepository, saveTextCapture, saveImageCapture) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
