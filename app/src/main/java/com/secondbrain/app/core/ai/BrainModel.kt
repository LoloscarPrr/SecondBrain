package com.secondbrain.app.core.ai

import com.secondbrain.app.core.model.Memory

data class BrainAnswer(
    val text: String,
    val sourceMemoryIds: List<String>
)

interface BrainModel {
    suspend fun summarize(input: String): String
    suspend fun extractMemories(input: String): List<Memory>
    suspend fun answer(question: String, context: List<Memory>): BrainAnswer
    suspend fun embed(text: String): FloatArray
}
