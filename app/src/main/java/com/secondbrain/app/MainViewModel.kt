package com.secondbrain.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secondbrain.app.core.model.Memory
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
    private val saveTextCapture: SaveTextCaptureUseCase
) : ViewModel() {
    val memories: StateFlow<List<Memory>> = memoryRepository
        .observeMemories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _captureText = MutableStateFlow("")
    val captureText: StateFlow<String> = _captureText.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun updateCaptureText(value: String) {
        _captureText.value = value
    }

    fun saveCapture() {
        val text = _captureText.value.trim()
        if (text.isEmpty() || _isSaving.value) return

        _isSaving.value = true
        viewModelScope.launch {
            runCatching { saveTextCapture(text) }
                .onSuccess { _captureText.value = "" }
            _isSaving.value = false
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
