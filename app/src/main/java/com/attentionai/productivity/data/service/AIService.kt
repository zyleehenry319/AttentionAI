package com.attentionai.productivity.data.service

import android.content.Context
import android.util.Log
import com.attentionai.productivity.data.api.GeminiService
import com.attentionai.productivity.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIService(private val context: Context) {
    
    private val geminiService = GeminiService(context)
    
    companion object {
        private const val TAG = "AIService"
    }
    
    suspend fun askQuestion(
        question: String,
        sessionId: Long?,
        context: ActivityContext
    ): String = withContext(Dispatchers.IO) {
        geminiService.askQuestion(question, sessionId, context)
    }
    
    suspend fun generateSummary(
        sessionId: Long?,
        context: ActivityContext
    ): AISummaryResponse = withContext(Dispatchers.IO) {
        geminiService.generateSummary(sessionId, context)
    }
    
    suspend fun generateInsights(
        sessionId: Long?,
        context: ActivityContext
    ): AIInsightResponse = withContext(Dispatchers.IO) {
        geminiService.generateInsights(sessionId, context)
    }
    
    suspend fun uploadScreenRecording(file: java.io.File): String? = withContext(Dispatchers.IO) {
        geminiService.uploadScreenRecording(file)
    }
    
    suspend fun analyzeScreenRecording(fileName: String, question: String): String = withContext(Dispatchers.IO) {
        geminiService.analyzeScreenRecording(fileName, question)
    }
    
    suspend fun analyzeMultipleFiles(fileUris: List<String>, question: String): String = withContext(Dispatchers.IO) {
        geminiService.analyzeMultipleFiles(fileUris, question)
    }
    
    fun getAIConfig(): AIConfig {
        return geminiService.getAIConfig()
    }
    
    fun saveAIConfig(config: AIConfig) {
        geminiService.saveAIConfig(config)
    }
}
