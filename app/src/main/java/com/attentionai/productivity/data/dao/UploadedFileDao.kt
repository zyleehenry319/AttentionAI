package com.attentionai.productivity.data.dao

import androidx.room.*
import com.attentionai.productivity.data.model.UploadedFile
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadedFileDao {
    
    @Query("SELECT * FROM uploaded_files ORDER BY uploadTime DESC")
    fun getAllFiles(): Flow<List<UploadedFile>>
    
    @Query("SELECT * FROM uploaded_files ORDER BY uploadTime DESC LIMIT :limit")
    suspend fun getRecentFiles(limit: Int): List<UploadedFile>
    
    @Query("SELECT * FROM uploaded_files WHERE fileUri = :fileUri")
    suspend fun getFileByUri(fileUri: String): UploadedFile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: UploadedFile)
    
    @Delete
    suspend fun deleteFile(file: UploadedFile)
    
    @Query("DELETE FROM uploaded_files WHERE fileUri = :fileUri")
    suspend fun deleteFileByUri(fileUri: String)
    
    @Query("DELETE FROM uploaded_files WHERE uploadTime < :cutoffTime")
    suspend fun deleteOldFiles(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM uploaded_files")
    suspend fun getFileCount(): Int
}


