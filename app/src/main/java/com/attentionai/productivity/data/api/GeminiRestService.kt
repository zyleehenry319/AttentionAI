package com.attentionai.productivity.data.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiRestService(private val context: Context) {
    
    // Tunable defaults
    private val CONNECT_TIMEOUT_SEC = 60L
    private val READ_TIMEOUT_SEC    = 180L  // allow long server processing / downloads
    private val WRITE_TIMEOUT_SEC   = 180L  // allow large uploads
    private val CALL_TIMEOUT_SEC    = 300L  // total max per request (end-to-end)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GeminiRestService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val UPLOAD_URL = "https://generativelanguage.googleapis.com/upload/v1beta/files"
    }
    
    suspend fun uploadFile(file: File, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting file upload to Gemini REST API: ${file.absolutePath}")
            
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext null
            }
            
            val fileSize = file.length()
            Log.d(TAG, "File size: $fileSize bytes")
            if (fileSize == 0L) {
                Log.e(TAG, "File is empty")
                return@withContext null
            }
            
            val mimeType = getMimeType(file)
            val displayName = file.name
            
            Log.d(TAG, "File details:")
            Log.d(TAG, "  - Name: $displayName")
            Log.d(TAG, "  - Size: $fileSize bytes")
            Log.d(TAG, "  - MIME Type: $mimeType")
            
            // Step 1: Initiate resumable upload
            val uploadUrl = initiateResumableUpload(apiKey, fileSize, mimeType, displayName)
            if (uploadUrl == null) {
                Log.e(TAG, "Failed to initiate resumable upload")
                return@withContext null
            }
            
            Log.d(TAG, "Resumable upload initiated: $uploadUrl")
            
            // Step 2: Upload the actual file
            val fileUri = uploadFileData(uploadUrl, file)
            if (fileUri == null) {
                Log.e(TAG, "Failed to upload file data")
                return@withContext null
            }
            
            Log.d(TAG, "File uploaded successfully: $fileUri")
            return@withContext fileUri
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }
    
    private suspend fun initiateResumableUpload(
        apiKey: String,
        fileSize: Long,
        mimeType: String,
        displayName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val metadata = JsonObject().apply {
                add("file", JsonObject().apply {
                    addProperty("display_name", displayName)
                })
            }
            
            val requestBody = metadata.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("X-Goog-Upload-Protocol", "resumable")
                .addHeader("X-Goog-Upload-Command", "start")
                .addHeader("X-Goog-Upload-Header-Content-Length", fileSize.toString())
                .addHeader("X-Goog-Upload-Header-Content-Type", mimeType)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to initiate upload: ${response.code} ${response.message}")
                response.body?.string()?.let { Log.e(TAG, "Response body: $it") }
                return@withContext null
            }
            
            val uploadUrl = response.header("X-Goog-Upload-URL")
            Log.d(TAG, "Upload URL received: $uploadUrl")
            uploadUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating resumable upload", e)
            null
        }
    }
    
    private suspend fun uploadFileData(uploadUrl: String, file: File): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = file.asRequestBody(getMimeType(file).toMediaType())
            
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("Content-Length", file.length().toString())
                .addHeader("X-Goog-Upload-Offset", "0")
                .addHeader("X-Goog-Upload-Command", "upload, finalize")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to upload file data: ${response.code} ${response.message}")
                response.body?.string()?.let { Log.e(TAG, "Response body: $it") }
                return@withContext null
            }
            
            val responseBody = response.body?.string()
            Log.d(TAG, "Upload response: $responseBody")
            
            // Parse the response to get the file URI
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val fileObject = jsonResponse.getAsJsonObject("file")
            val fileUri = fileObject.get("uri")?.asString
            
            Log.d(TAG, "File URI: $fileUri")
            Log.d(TAG, "File uploaded successfully: $fileUri")
            fileUri
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file data", e)
            null
        }
    }
    
    suspend fun analyzeFile(fileUri: String, question: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing file with Gemini REST API: $fileUri")
            Log.d(TAG, "Question: $question")
            
            // First, check if the file is active
            Log.d(TAG, "Checking if file is active...")
            if (!waitForFileActive(fileUri, apiKey)) {
                Log.e(TAG, "File $fileUri is not active or failed to process")
                return@withContext "Error: Video file is not ready for analysis. Please try again in a moment."
            }
            Log.d(TAG, "File is active, proceeding with analysis")
            
            val mimeType = "video/mp4" // Assuming video files for screen recordings
            
            // Use the exact structure from the curl example (no role property)
            val requestBody = JsonObject().apply {
                add("contents", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", com.google.gson.JsonArray().apply {
                            add(JsonObject().apply {
                                add("fileData", JsonObject().apply {
                                    addProperty("mimeType", mimeType)
                                    addProperty("fileUri", fileUri)
                                })
                            })
                            add(JsonObject().apply {
                                addProperty("text", question)
                            })
                        })
                    })
                })
                addProperty("model", "gemini-2.5-flash")
            }
            
            Log.d(TAG, "Request body: ${requestBody.toString()}")
            
            val request = Request.Builder()
                .url("$BASE_URL/models/gemini-2.5-flash:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to analyze file: ${response.code} ${response.message}")
                response.body?.string()?.let { Log.e(TAG, "Response body: $it") }
                return@withContext null
            }
            
            val responseBody = response.body?.string()
            Log.d(TAG, "Analysis response: $responseBody")
            
            // Parse the response to get the generated text
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val candidates = jsonResponse.getAsJsonArray("candidates")
            
            if (candidates.size() > 0) {
                val candidate = candidates.get(0).asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                
                if (parts.size() > 0) {
                    val text = parts.get(0).asJsonObject.get("text")?.asString
                    Log.d(TAG, "Analysis result: $text")
                    return@withContext text
                }
            }
            
            Log.e(TAG, "No analysis result found in response")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing file", e)
            null
        }
    }
    
    suspend fun analyzeMultipleFiles(fileUris: List<String>, question: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing multiple files with Gemini REST API: $fileUris")
            Log.d(TAG, "Question: $question")
            
            // Wait for all files to be active
            for (fileUri in fileUris) {
                if (!waitForFileActive(fileUri, apiKey)) {
                    Log.e(TAG, "File $fileUri is not active or failed to process")
                    return@withContext null
                }
            }
            
            Log.d(TAG, "All files are active, proceeding with analysis")
            
            // Build request body with multiple files
            val requestBody = JsonObject().apply {
                add("contents", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", com.google.gson.JsonArray().apply {
                            // Add all file URIs
                            fileUris.forEach { fileUri ->
                                add(JsonObject().apply {
                                    add("fileData", JsonObject().apply {
                                        addProperty("mimeType", "video/mp4")
                                        addProperty("fileUri", fileUri)
                                    })
                                })
                            }
                            // Add the question
                            add(JsonObject().apply {
                                addProperty("text", question)
                            })
                        })
                    })
                })
                addProperty("model", "gemini-2.5-flash")
            }
            
            Log.d(TAG, "Request body: ${requestBody.toString()}")
            
            val request = Request.Builder()
                .url("$BASE_URL/models/gemini-2.5-flash:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to analyze multiple files: ${response.code} ${response.message}")
                response.body?.string()?.let { Log.e(TAG, "Response body: $it") }
                return@withContext null
            }
            
            val responseBody = response.body?.string()
            Log.d(TAG, "Analysis response: $responseBody")
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val candidates = jsonResponse.getAsJsonArray("candidates")
            
            if (candidates.size() > 0) {
                val candidate = candidates.get(0).asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                
                if (parts.size() > 0) {
                    val text = parts.get(0).asJsonObject.get("text")?.asString
                    Log.d(TAG, "Multiple files analysis result: $text")
                    return@withContext text
                }
            }
            
            Log.e(TAG, "No analysis result found in response")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing multiple files", e)
            null
        }
    }
    
    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mp3"
            "m4a" -> "audio/m4a"
            "wav" -> "audio/wav"
            "avi" -> "video/avi"
            "mov" -> "video/mov"
            "webm" -> "video/webm"
            else -> "video/mp4" // Default for screen recordings
        }
    }
    
    private suspend fun waitForFileActive(fileUri: String, apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileId = fileUri.substringAfterLast("/")
            val maxAttempts = 30 // Wait up to 5 minutes (30 * 10 seconds)
            var attempts = 0
            
            while (attempts < maxAttempts) {
                val request = Request.Builder()
                    .url("$BASE_URL/files/$fileId")
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "File status response: $responseBody")
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val state = jsonResponse.get("state")?.asString
                    
                    Log.d(TAG, "File $fileId state: $state")
                    
                    when (state) {
                        "ACTIVE" -> {
                            Log.d(TAG, "File $fileId is now active")
                            return@withContext true
                        }
                        "FAILED" -> {
                            Log.e(TAG, "File $fileId failed to process")
                            return@withContext false
                        }
                        else -> {
                            Log.d(TAG, "File $fileId is still processing, waiting...")
                            kotlinx.coroutines.delay(10000) // Wait 10 seconds
                            attempts++
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to check file status: ${response.code}")
                    return@withContext false
                }
            }
            
            Log.e(TAG, "File $fileId did not become active within timeout")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file status", e)
            false
        }
    }
}
