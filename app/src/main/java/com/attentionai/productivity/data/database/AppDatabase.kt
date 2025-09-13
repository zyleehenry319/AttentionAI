package com.attentionai.productivity.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.attentionai.productivity.data.model.ActivityEvent
import com.attentionai.productivity.data.model.ActivitySession
import com.attentionai.productivity.data.model.AIInsight
import com.attentionai.productivity.data.model.UploadedFile
import com.attentionai.productivity.data.dao.ActivityDao
import com.attentionai.productivity.data.dao.InsightDao
import com.attentionai.productivity.data.dao.UploadedFileDao
import com.attentionai.productivity.data.converter.Converters

@Database(
    entities = [ActivitySession::class, ActivityEvent::class, AIInsight::class, UploadedFile::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun activityDao(): ActivityDao
    abstract fun insightDao(): InsightDao
    abstract fun uploadedFileDao(): UploadedFileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attention_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

