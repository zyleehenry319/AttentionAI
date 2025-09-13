package com.attentionai.productivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "activity_sessions")
data class ActivitySession(
    @PrimaryKey
    val id: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val isActive: Boolean = false,
    val summary: String? = null,
    val videoPath: String? = null,
    val audioPath: String? = null,
    val transcript: String? = null,
    val keywords: List<String> = emptyList(),
    val appUsage: Map<String, Long> = emptyMap(), // app package name -> time spent in ms
    val productivityScore: Float? = null,
    val geminiFileUri: String? = null, // Store the Gemini file URI for uploaded videos
    val createdAt: Long = System.currentTimeMillis()
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
    
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    
    val formattedStartTime: String
        get() {
            val date = Date(startTime)
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            return formatter.format(date)
        }
}

@Entity(tableName = "activity_events")
data class ActivityEvent(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val sessionId: Long,
    val timestamp: Long,
    val eventType: String, // Changed from enum to String for Room compatibility
    val appPackage: String? = null,
    val appName: String? = null,
    val screenContent: String? = null,
    val audioTranscript: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class EventType {
    APP_OPENED,
    APP_CLOSED,
    SCREEN_CONTENT_CHANGED,
    AUDIO_DETECTED,
    USER_INTERACTION,
    NOTIFICATION_RECEIVED,
    CALL_RECEIVED,
    CALL_ENDED
}

@Entity(tableName = "ai_insights")
data class AIInsight(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val sessionId: Long,
    val insightType: String, // Changed from enum to String for Room compatibility
    val title: String,
    val description: String,
    val confidence: Float, // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis(),
    val isPositive: Boolean = true,
    val actionable: Boolean = false,
    val suggestion: String? = null
)

enum class InsightType {
    PRODUCTIVITY_TIP,
    DISTRACTION_ALERT,
    FOCUS_ACHIEVEMENT,
    TIME_MANAGEMENT,
    APP_USAGE_PATTERN,
    BREAK_REMINDER,
    GOAL_PROGRESS
}
