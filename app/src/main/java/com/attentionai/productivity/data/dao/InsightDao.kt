package com.attentionai.productivity.data.dao

import androidx.room.*
import com.attentionai.productivity.data.model.AIInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    
    @Query("SELECT * FROM ai_insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<AIInsight>>
    
    @Query("SELECT * FROM ai_insights WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getInsightsForSession(sessionId: Long): List<AIInsight>
    
    @Query("SELECT * FROM ai_insights WHERE insightType = :type ORDER BY timestamp DESC")
    suspend fun getInsightsByType(type: String): List<AIInsight>
    
    @Query("SELECT * FROM ai_insights WHERE isPositive = 1 ORDER BY timestamp DESC")
    suspend fun getPositiveInsights(): List<AIInsight>
    
    @Query("SELECT * FROM ai_insights WHERE actionable = 1 ORDER BY timestamp DESC")
    suspend fun getActionableInsights(): List<AIInsight>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AIInsight)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<AIInsight>)
    
    @Update
    suspend fun updateInsight(insight: AIInsight)
    
    @Delete
    suspend fun deleteInsight(insight: AIInsight)
    
    @Query("DELETE FROM ai_insights WHERE id = :insightId")
    suspend fun deleteInsightById(insightId: Long)
    
    @Query("DELETE FROM ai_insights WHERE sessionId = :sessionId")
    suspend fun deleteInsightsForSession(sessionId: Long)
    
    @Query("SELECT * FROM ai_insights WHERE confidence >= :minConfidence ORDER BY confidence DESC, timestamp DESC")
    suspend fun getHighConfidenceInsights(minConfidence: Float = 0.7f): List<AIInsight>
    
    @Query("SELECT COUNT(*) FROM ai_insights WHERE sessionId = :sessionId")
    suspend fun getInsightCountForSession(sessionId: Long): Int
}

