package com.attentionai.productivity.data.repository

import android.content.Context
import com.attentionai.productivity.data.database.AppDatabase
import com.attentionai.productivity.data.model.*
import com.attentionai.productivity.data.service.AIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityRepository(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val activityDao = database.activityDao()
    private val insightDao = database.insightDao()
    private val uploadedFileDao = database.uploadedFileDao()
    private val aiService = AIService(context)
    
    suspend fun insertSession(session: ActivitySession) = withContext(Dispatchers.IO) {
        activityDao.insertSession(session)
    }
    
    suspend fun updateSession(session: ActivitySession) = withContext(Dispatchers.IO) {
        activityDao.updateSession(session)
    }
    
    suspend fun getSessionById(sessionId: Long): ActivitySession? = withContext(Dispatchers.IO) {
        activityDao.getSessionById(sessionId)
    }
    
    suspend fun getRecentSessions(limit: Int): List<ActivitySession> = withContext(Dispatchers.IO) {
        activityDao.getRecentSessions(limit)
    }
    
    suspend fun getActiveSession(): ActivitySession? = withContext(Dispatchers.IO) {
        activityDao.getActiveSession()
    }
    
    suspend fun insertEvent(event: ActivityEvent) = withContext(Dispatchers.IO) {
        activityDao.insertEvent(event)
    }
    
    suspend fun getEventsForSession(sessionId: Long): List<ActivityEvent> = withContext(Dispatchers.IO) {
        activityDao.getEventsForSession(sessionId)
    }
    
    suspend fun insertInsight(insight: AIInsight) = withContext(Dispatchers.IO) {
        insightDao.insertInsight(insight)
    }
    
    suspend fun getInsightsForSession(sessionId: Long): List<AIInsight> = withContext(Dispatchers.IO) {
        insightDao.getInsightsForSession(sessionId)
    }
    
    suspend fun askQuestion(question: String, sessionId: Long?): String = withContext(Dispatchers.IO) {
        try {
            val session = sessionId?.let { activityDao.getSessionById(it) }
            val events = sessionId?.let { activityDao.getEventsForSession(it) } ?: emptyList()
            
            android.util.Log.d("ActivityRepository", "askQuestion - sessionId: $sessionId")
            android.util.Log.d("ActivityRepository", "askQuestion - session: $session")
            
            // Get up to 5 most recent uploaded files
            val recentFiles = uploadedFileDao.getRecentFiles(5)
            android.util.Log.d("ActivityRepository", "askQuestion - Found ${recentFiles.size} recent uploaded files")
            
            if (recentFiles.isNotEmpty()) {
                // Use file-based analysis with multiple recent files
                val fileUris = recentFiles.map { it.fileUri }
                android.util.Log.d("ActivityRepository", "askQuestion - Using ${fileUris.size} file URIs for analysis: $fileUris")
                
                return@withContext aiService.analyzeMultipleFiles(fileUris, question)
            } else {
                // Fallback to context-based analysis if no file URIs are available
                android.util.Log.d("ActivityRepository", "askQuestion - No uploaded files available, using context-based analysis")
                val context = ActivityContext(
                    session = session,
                    events = events,
                    screenContent = extractScreenContent(events),
                    audioTranscript = session?.transcript,
                    appUsage = session?.appUsage ?: emptyMap(),
                    timeRange = session?.let { "${it.formattedStartTime} - ${it.formattedDuration}" }
                )
                
                return@withContext aiService.askQuestion(question, sessionId, context)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error in askQuestion", e)
            "I encountered an error processing your question: ${e.message}"
        }
    }
    
    suspend fun generateSummary(sessionId: Long?): String = withContext(Dispatchers.IO) {
        try {
            val session = sessionId?.let { activityDao.getSessionById(it) }
            val events = sessionId?.let { activityDao.getEventsForSession(it) } ?: emptyList()
            
            if (session == null) {
                return@withContext "No active session found to summarize."
            }
            
            // Check if we have a video file for this session and use file analysis
            if (session.videoPath != null) {
                val videoFile = java.io.File(session.videoPath)
                if (videoFile.exists()) {
                    // First upload the file to get the file URI
                    val fileUri = aiService.uploadScreenRecording(videoFile)
                    if (fileUri != null) {
                        // Use the uploaded file URI for analysis
                        val summary = aiService.analyzeScreenRecording(fileUri, "Please provide a comprehensive summary of what happened in this screen recording session. Include key activities, apps used, and productivity insights.")
                        
                        // Save the AI-generated summary to the database
                        activityDao.updateSessionSummary(sessionId, summary)
                        
                        return@withContext summary
                    }
                }
            }
            
            // Fallback to regular context-based analysis
            val context = ActivityContext(
                session = session,
                events = events,
                screenContent = extractScreenContent(events),
                audioTranscript = session.transcript,
                appUsage = session.appUsage,
                timeRange = "${session.formattedStartTime} - ${session.formattedDuration}"
            )
            
            val aiSummary = aiService.generateSummary(sessionId, context)
            
            // Save the AI-generated summary to the database
            activityDao.updateSessionSummary(sessionId, aiSummary.summary)
            
            // Format the response with highlights and recommendations
            buildString {
                appendLine(aiSummary.summary)
                if (aiSummary.highlights.isNotEmpty()) {
                    appendLine("\n🎯 Key Highlights:")
                    aiSummary.highlights.forEach { highlight ->
                        appendLine("• $highlight")
                    }
                }
                if (aiSummary.recommendations.isNotEmpty()) {
                    appendLine("\n💡 Recommendations:")
                    aiSummary.recommendations.forEach { recommendation ->
                        appendLine("• $recommendation")
                    }
                }
            }
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
        }
    }
    
    suspend fun generateInsights(sessionId: Long?): AIInsightResponse = withContext(Dispatchers.IO) {
        try {
            val session = sessionId?.let { activityDao.getSessionById(it) }
            val events = sessionId?.let { activityDao.getEventsForSession(it) } ?: emptyList()
            
            if (session == null) {
                return@withContext AIInsightResponse(
                    insights = emptyList(),
                    recommendations = emptyList(),
                    productivityScore = null,
                    keyFindings = emptyList(),
                    actionItems = emptyList()
                )
            }
            
            val context = ActivityContext(
                session = session,
                events = events,
                screenContent = extractScreenContent(events),
                audioTranscript = session.transcript,
                appUsage = session.appUsage,
                timeRange = "${session.formattedStartTime} - ${session.formattedDuration}"
            )
            
            val aiInsights = aiService.generateInsights(sessionId, context)
            
            // Save insights to database
            aiInsights.insights.forEach { insight ->
                insightDao.insertInsight(insight)
            }
            
            aiInsights
        } catch (e: Exception) {
            AIInsightResponse(
                insights = emptyList(),
                recommendations = emptyList(),
                productivityScore = null,
                keyFindings = emptyList(),
                actionItems = emptyList()
            )
        }
    }
    
    private fun extractScreenContent(events: List<ActivityEvent>): List<String> {
        return events
            .filter { it.screenContent != null }
            .mapNotNull { it.screenContent }
            .distinct()
    }
    
    fun saveAIConfig(config: AIConfig) {
        aiService.saveAIConfig(config)
    }
    
    fun getAIConfig(): AIConfig {
        return aiService.getAIConfig()
    }
    
    suspend fun uploadScreenRecording(file: java.io.File): String? = withContext(Dispatchers.IO) {
        try {
            val fileUri = aiService.uploadScreenRecording(file)
            if (fileUri != null) {
                // Store the uploaded file info in the database
                val uploadedFile = UploadedFile(
                    fileUri = fileUri,
                    localPath = file.absolutePath,
                    fileSize = file.length(),
                    mimeType = "video/mp4"
                )
                uploadedFileDao.insertFile(uploadedFile)
                android.util.Log.d("ActivityRepository", "Stored uploaded file: $fileUri")
            }
            fileUri
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun analyzeScreenRecording(fileName: String, question: String): String = withContext(Dispatchers.IO) {
        try {
            aiService.analyzeScreenRecording(fileName, question)
        } catch (e: Exception) {
            "Error analyzing screen recording: ${e.message}"
        }
    }
}
