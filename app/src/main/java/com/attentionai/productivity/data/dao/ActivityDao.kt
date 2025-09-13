package com.attentionai.productivity.data.dao

import androidx.room.*
import com.attentionai.productivity.data.model.ActivitySession
import com.attentionai.productivity.data.model.ActivityEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    
    @Query("SELECT * FROM activity_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ActivitySession>>
    
    @Query("SELECT * FROM activity_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ActivitySession?
    
    @Query("SELECT * FROM activity_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<ActivitySession>
    
    @Query("SELECT * FROM activity_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ActivitySession?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ActivitySession)
    
    @Update
    suspend fun updateSession(session: ActivitySession)
    
    @Delete
    suspend fun deleteSession(session: ActivitySession)
    
    @Query("DELETE FROM activity_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    @Query("SELECT * FROM activity_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: Long): List<ActivityEvent>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEvent)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ActivityEvent>)
    
    @Query("SELECT * FROM activity_events WHERE sessionId = :sessionId AND eventType = :eventType ORDER BY timestamp ASC")
    suspend fun getEventsByType(sessionId: Long, eventType: String): List<ActivityEvent>
    
    @Query("SELECT DISTINCT appPackage FROM activity_events WHERE sessionId = :sessionId AND appPackage IS NOT NULL")
    suspend fun getAppsUsedInSession(sessionId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM activity_events WHERE sessionId = :sessionId AND eventType = 'APP_OPENED'")
    suspend fun getAppSwitchCount(sessionId: Long): Int
    
    @Query("SELECT * FROM activity_sessions WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    suspend fun getSessionsInTimeRange(startTime: Long, endTime: Long): List<ActivitySession>
    
    @Query("SELECT * FROM activity_sessions WHERE summary IS NOT NULL ORDER BY startTime DESC")
    suspend fun getSummarizedSessions(): List<ActivitySession>
    
    @Query("UPDATE activity_sessions SET summary = :summary WHERE id = :sessionId")
    suspend fun updateSessionSummary(sessionId: Long, summary: String)
    
    @Query("UPDATE activity_sessions SET productivityScore = :score WHERE id = :sessionId")
    suspend fun updateProductivityScore(sessionId: Long, score: Float)
    
    @Query("SELECT * FROM activity_sessions WHERE geminiFileUri IS NOT NULL ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessionsWithFileUris(limit: Int): List<ActivitySession>
}

