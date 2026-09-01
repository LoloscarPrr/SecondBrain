package com.secondbrain.app.domain.memory

import com.secondbrain.app.core.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeMemories(): Flow<List<Memory>>
    suspend fun getMemory(id: String): Memory?
    suspend fun saveMemory(memory: Memory)
    suspend fun deleteMemory(id: String)
    suspend fun searchMemories(query: String): List<Memory>
}
