package com.attentionai.productivity.data.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.attentionai.productivity.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GeminiService(private val context: Context) {
    
    private var generativeModel: GenerativeModel? = null
    private val gson = com.google.gson.Gson()
    private val restService = GeminiRestService(context)
    
    companion object {
        private const val TAG = "GeminiService"
        private const val DEFAULT_API_KEY = "your-gemini-api-key-here"
    }
    
    private fun initializeModel(apiKey: String) {
        if (generativeModel == null || apiKey != getAIConfig().apiKey) {
            val config = getAIConfig()
            generativeModel = GenerativeModel(
                modelName = config.model,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = config.temperature.toFloat()
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = config.maxTokens
                }
            )
        }
    }
    
    suspend fun askQuestion(
        question: String,
        sessionId: Long?,
        context: ActivityContext
    ): String = withContext(Dispatchers.IO) {
        try {
            val config = getAIConfig()
            initializeModel(config.apiKey)
            
            val prompt = buildPrompt(question, context, AnalysisType.GENERAL)
            val model = generativeModel ?: throw IllegalStateException("Model not initialized")
            
            val response = model.generateContent(
                content {
                    text(prompt.systemPrompt + "\n\n" + prompt.userPromptTemplate)
                }
            )
            
            response.text ?: "I couldn't generate a response. Please try again."
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in askQuestion", e)
            "I encountered an error processing your question: ${e.message}"
        }
    }
    
    suspend fun generateSummary(
        sessionId: Long?,
        context: ActivityContext
    ): AISummaryResponse = withContext(Dispatchers.IO) {
        try {
            val config = getAIConfig()
            initializeModel(config.apiKey)
            
            val prompt = buildPrompt("Generate a comprehensive summary", context, AnalysisType.SUMMARY_GENERATION)
            val model = generativeModel ?: throw IllegalStateException("Model not initialized")
            
            val response = model.generateContent(
                content {
                    text(prompt.systemPrompt + "\n\n" + prompt.userPromptTemplate)
                }
            )
            
            val content = response.text ?: ""
            parseSummaryResponse(content)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateSummary", e)
            AISummaryResponse(
                summary = "Error generating summary: ${e.message}",
                keyMetrics = emptyMap(),
                highlights = emptyList(),
                concerns = emptyList(),
                recommendations = emptyList()
            )
        }
    }
    
    suspend fun generateInsights(
        sessionId: Long?,
        context: ActivityContext
    ): AIInsightResponse = withContext(Dispatchers.IO) {
        try {
            val config = getAIConfig()
            initializeModel(config.apiKey)
            
            val prompt = buildPrompt("Analyze this session for productivity insights", context, AnalysisType.PRODUCTIVITY_ANALYSIS)
            val model = generativeModel ?: throw IllegalStateException("Model not initialized")
            
            val response = model.generateContent(
                content {
                    text(prompt.systemPrompt + "\n\n" + prompt.userPromptTemplate)
                }
            )
            
            val content = response.text ?: ""
            parseInsightResponse(content, sessionId ?: 0)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateInsights", e)
            AIInsightResponse(
                insights = emptyList(),
                recommendations = emptyList(),
                productivityScore = null,
                keyFindings = emptyList(),
                actionItems = emptyList()
            )
        }
    }
    
    suspend fun uploadScreenRecording(file: File): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Uploading screen recording file to Gemini REST API: ${file.absolutePath}")
            
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext null
            }
            
            val fileSize = file.length()
            Log.d(TAG, "File size: $fileSize bytes")
            
            if (fileSize == 0L) {
                Log.e(TAG, "File is empty, cannot process")
                return@withContext null
            }
            
            val config = getAIConfig()
            if (config.apiKey == DEFAULT_API_KEY) {
                Log.e(TAG, "Please configure your Gemini API key in settings")
                return@withContext null
            }
            
            // Upload file using REST API
            val fileUri = restService.uploadFile(file, config.apiKey)
            if (fileUri == null) {
                Log.e(TAG, "Failed to upload file to Gemini")
                return@withContext null
            }
            
            Log.d(TAG, "File uploaded successfully to Gemini: $fileUri")
            return@withContext fileUri
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file to Gemini", e)
            null
        }
    }
    
    suspend fun analyzeScreenRecording(fileName: String, question: String): String = withContext(Dispatchers.IO) {
        try {
            val config = getAIConfig()
            if (config.apiKey == DEFAULT_API_KEY) {
                return@withContext "Please configure your Gemini API key in settings to analyze screen recordings."
            }
            
            Log.d(TAG, "Analyzing uploaded video file with Gemini REST API: $fileName")
            
            // Use REST API to analyze the uploaded file
            val result = restService.analyzeFile(fileName, question, config.apiKey)
            if (result == null) {
                Log.e(TAG, "Failed to analyze file with Gemini REST API")
                return@withContext "I encountered an error analyzing the screen recording. Please try again."
            }
            
            Log.d(TAG, "Analysis completed successfully using REST API")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing screen recording with REST API", e)
            "I encountered an error analyzing the screen recording: ${e.message}"
        }
    }
    
    suspend fun analyzeMultipleFiles(fileUris: List<String>, question: String): String = withContext(Dispatchers.IO) {
        try {
            val config = getAIConfig()
            if (config.apiKey == DEFAULT_API_KEY) {
                return@withContext "Please configure your Gemini API key in settings to analyze screen recordings."
            }
            
            Log.d(TAG, "Analyzing multiple files with Gemini REST API: $fileUris")
            
            // Use REST API to analyze multiple files
            val result = restService.analyzeMultipleFiles(fileUris, question, config.apiKey)
            if (result == null) {
                Log.e(TAG, "Failed to analyze multiple files with Gemini REST API")
                return@withContext "I encountered an error analyzing the screen recordings. Please try again."
            }
            
            Log.d(TAG, "Multiple files analysis completed successfully using REST API")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing multiple files with REST API", e)
            "I encountered an error analyzing the screen recordings: ${e.message}"
        }
    }
    
    private data class FileInfo(
        val size: Long,
        val timestamp: Long
    )
    
    private fun parseFileReference(fileReference: String): FileInfo {
        return try {
            // Parse "video_timestamp_size" format
            val parts = fileReference.split("_")
            if (parts.size >= 3) {
                val timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                val size = parts[2].toLongOrNull() ?: 0L
                FileInfo(size, timestamp)
            } else {
                FileInfo(0L, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing file reference: $fileReference", e)
            FileInfo(0L, System.currentTimeMillis())
        }
    }
    
    private fun buildPrompt(question: String, context: ActivityContext, analysisType: AnalysisType): PromptTemplate {
        val systemPrompt = buildSystemPrompt(analysisType)
        val contextData = buildContextData(context)
        val userPrompt = buildUserPrompt(question, contextData, analysisType)
        
        return PromptTemplate(
            systemPrompt = systemPrompt,
            userPromptTemplate = userPrompt,
            contextTemplate = contextData
        )
    }
    
    private fun buildSystemPrompt(analysisType: AnalysisType): String {
        return when (analysisType) {
            AnalysisType.PRODUCTIVITY_ANALYSIS -> """
                You are an expert productivity analyst and personal assistant. Your role is to analyze phone usage data and provide actionable insights to help users improve their productivity and digital well-being.
                
                Key areas to focus on:
                - App usage patterns and time distribution
                - Focus and distraction analysis
                - Time management effectiveness
                - Productivity trends and recommendations
                - Digital wellness insights
                
                Always provide specific, actionable advice based on the data provided.
                Be encouraging but honest about areas for improvement.
                Use emojis and clear formatting to make responses engaging.
            """.trimIndent()
            
            AnalysisType.SUMMARY_GENERATION -> """
                You are a professional productivity coach creating comprehensive session summaries. 
                Analyze the provided phone usage data and create detailed, insightful summaries that help users understand their digital behavior patterns.
                
                Include:
                - Key metrics and statistics
                - Notable patterns and trends
                - Productivity highlights and concerns
                - Specific recommendations for improvement
                - Overall assessment with actionable next steps
                
                Format your response as a structured summary with clear sections and bullet points.
            """.trimIndent()
            
            else -> """
                You are an intelligent personal assistant that analyzes phone usage data to help users understand and improve their digital habits.
                
                Provide helpful, accurate, and actionable responses based on the data provided.
                Be conversational but informative.
                Focus on productivity, digital wellness, and time management insights.
            """.trimIndent()
        }
    }
    
    private fun buildContextData(context: ActivityContext): String {
        val session = context.session
        val events = context.events
        
        return buildString {
            appendLine("=== SESSION DATA ===")
            session?.let {
                appendLine("Session Duration: ${it.formattedDuration}")
                appendLine("Start Time: ${it.formattedStartTime}")
                appendLine("Productivity Score: ${it.productivityScore ?: "Not calculated"}")
            }
            
            appendLine("\n=== APP USAGE ===")
            val appUsage = events.groupBy { it.appName }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
            
            appUsage.forEach { (app, count) ->
                if (app != null) {
                    appendLine("• $app: $count interactions")
                }
            }
            
            appendLine("\n=== ACTIVITY EVENTS ===")
            events.take(20).forEach { event ->
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
                appendLine("• $time: ${event.eventType} - ${event.appName ?: "Unknown"}")
            }
            
            if (context.screenContent.isNotEmpty()) {
                appendLine("\n=== SCREEN CONTENT SAMPLE ===")
                context.screenContent.take(5).forEach { content ->
                    appendLine("• $content")
                }
            }
            
            if (!context.audioTranscript.isNullOrBlank()) {
                appendLine("\n=== AUDIO TRANSCRIPT ===")
                appendLine(context.audioTranscript.take(500))
            }
        }
    }
    
    private fun buildUserPrompt(question: String, contextData: String, analysisType: AnalysisType): String {
        return when (analysisType) {
            AnalysisType.SUMMARY_GENERATION -> """
                Please analyze the following phone usage session data and provide a comprehensive summary:
                
                $contextData
                
                Generate a detailed summary including key metrics, patterns, insights, and recommendations.
            """.trimIndent()
            
            else -> """
                User Question: $question
                
                Context Data:
                $contextData
                
                Please provide a helpful, specific response based on this data.
            """.trimIndent()
        }
    }
    
    private fun parseSummaryResponse(content: String): AISummaryResponse {
        val lines = content.split("\n")
        
        val summary = lines.firstOrNull { it.contains("Summary:") || it.contains("## Summary") } 
            ?: content.take(500)
        
        val highlights = lines.filter { it.contains("•") || it.contains("-") }
            .map { it.replace(Regex("[•-]\\s*"), "").trim() }
            .filter { it.isNotEmpty() }
        
        val recommendations = lines.filter { 
            it.contains("recommendation", ignoreCase = true) || 
            it.contains("suggest", ignoreCase = true) ||
            it.contains("should", ignoreCase = true)
        }
        
        return AISummaryResponse(
            summary = summary,
            keyMetrics = emptyMap(),
            highlights = highlights.take(5),
            concerns = emptyList(),
            recommendations = recommendations.take(3)
        )
    }
    
    private fun parseInsightResponse(content: String, sessionId: Long): AIInsightResponse {
        val insights = mutableListOf<AIInsight>()
        
        return AIInsightResponse(
            insights = insights,
            recommendations = emptyList(),
            productivityScore = null,
            keyFindings = emptyList(),
            actionItems = emptyList()
        )
    }
    
    fun getAIConfig(): AIConfig {
        val sharedPrefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("gemini_api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        
        return AIConfig(
            apiKey = apiKey,
            model = sharedPrefs.getString("model", "gemini-flash-2.5") ?: "gemini-flash-2.5",
            maxTokens = sharedPrefs.getInt("max_tokens", 2048),
            temperature = sharedPrefs.getFloat("temperature", 0.7f).toDouble(),
            customInstructions = sharedPrefs.getString("custom_instructions", null)
        )
    }
    
    fun saveAIConfig(config: AIConfig) {
        val sharedPrefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("gemini_api_key", config.apiKey)
            .putString("model", config.model)
            .putInt("max_tokens", config.maxTokens)
            .putFloat("temperature", config.temperature.toFloat())
            .putString("custom_instructions", config.customInstructions)
            .apply()
    }
}
