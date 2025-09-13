package com.attentionai.productivity.data.model

import com.google.gson.annotations.SerializedName

// OpenAI API Request Models
data class ChatRequest(
    @SerializedName("model") val model: String = "gpt-4",
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("stream") val stream: Boolean = false
)

data class ChatMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
)

data class ChatResponse(
    @SerializedName("id") val id: String,
    @SerializedName("object") val `object`: String,
    @SerializedName("created") val created: Long,
    @SerializedName("model") val model: String,
    @SerializedName("choices") val choices: List<Choice>,
    @SerializedName("usage") val usage: Usage?
)

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// AI Analysis Models
data class AIAnalysisRequest(
    val sessionId: Long,
    val question: String,
    val context: ActivityContext,
    val analysisType: AnalysisType = AnalysisType.GENERAL
)

data class ActivityContext(
    val session: ActivitySession?,
    val events: List<ActivityEvent>,
    val screenContent: List<String> = emptyList(),
    val audioTranscript: String? = null,
    val appUsage: Map<String, Long> = emptyMap(),
    val timeRange: String? = null
)

enum class AnalysisType {
    GENERAL,
    PRODUCTIVITY_ANALYSIS,
    APP_USAGE_ANALYSIS,
    TIME_MANAGEMENT,
    DISTRACTION_ANALYSIS,
    GOAL_TRACKING,
    SUMMARY_GENERATION
}

data class AIInsightResponse(
    val insights: List<AIInsight>,
    val recommendations: List<String>,
    val productivityScore: Float?,
    val keyFindings: List<String>,
    val actionItems: List<String>
)

data class AISummaryResponse(
    val summary: String,
    val keyMetrics: Map<String, Any>,
    val highlights: List<String>,
    val concerns: List<String>,
    val recommendations: List<String>
)

// Configuration Models
data class AIConfig(
    val apiKey: String,
    val model: String = "gemini-flash-2.5",
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
    val enableStreaming: Boolean = false,
    val customInstructions: String? = null
)


data class PromptTemplate(
    val systemPrompt: String,
    val userPromptTemplate: String,
    val contextTemplate: String
)
