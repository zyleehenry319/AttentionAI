package com.attentionai.productivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_files")
data class UploadedFile(
    @PrimaryKey
    val fileUri: String, // The full Gemini file URI
    val localPath: String, // Local file path
    val uploadTime: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val mimeType: String = "video/mp4"
)


