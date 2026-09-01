package com.secondbrain.app.data.repository

import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.data.local.MemoryDao
import com.secondbrain.app.data.local.toDomain
import com.secondbrain.app.data.local.toEntity
import com.secondbrain.app.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalMemoryRepository(
    private val memoryDao: MemoryDao
) : MemoryRepository {
    override fun observeMemories(): Flow<List<Memory>> =
        memoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMemory(id: String): Memory? =
        memoryDao.getById(id)?.toDomain()

    override suspend fun saveMemory(memory: Memory) {
        memoryDao.upsert(memory.toEntity())
    }

    override suspend fun deleteMemory(id: String) {
        memoryDao.deleteById(id)
    }

    override suspend fun searchMemories(query: String): List<Memory> =
        memoryDao.search(query.trim()).map { it.toDomain() }
}
