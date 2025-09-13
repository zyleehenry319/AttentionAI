package com.attentionai.productivity.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attentionai.productivity.data.model.*
import com.attentionai.productivity.data.repository.ActivityRepository
import com.attentionai.productivity.service.AIProcessingService
import com.attentionai.productivity.service.ScreenRecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = getApplication<Application>()
    private val repository = ActivityRepository(context)
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _currentSession = MutableStateFlow<ActivitySession?>(null)
    val currentSession: StateFlow<ActivitySession?> = _currentSession.asStateFlow()
    
    private val _aiInsights = MutableStateFlow<AIInsightResponse?>(null)
    val aiInsights: StateFlow<AIInsightResponse?> = _aiInsights.asStateFlow()
    
    private val _aiConfig = MutableStateFlow<AIConfig?>(null)
    val aiConfig: StateFlow<AIConfig?> = _aiConfig.asStateFlow()
    
    init {
        // Defer database operations to avoid blocking the main thread
        viewModelScope.launch {
            try {
                loadRecentSessions()
                loadAIConfig()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error in init block", e)
                _uiState.value = _uiState.value.copy(
                    message = "Error initializing app: ${e.message}"
                )
            }
        }
    }
    
    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(
            permissionState = PermissionState.GRANTED,
            message = "Ready to record! Tap the button to start screen recording."
        )
    }
    
    fun onPermissionsDenied() {
        _uiState.value = _uiState.value.copy(
            permissionState = PermissionState.DENIED,
            message = "Please grant permissions to enable screen recording."
        )
    }
    
    fun onOverlayPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            overlayPermissionGranted = true,
            message = "Overlay permission granted!"
        )
    }
    
    fun onRecordingStopped(sessionId: Long, videoPath: String?) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MainViewModel", "onRecordingStopped - sessionId: $sessionId, videoPath: $videoPath")
                android.util.Log.d("MainViewModel", "onRecordingStopped - _currentSession.value: $_currentSession.value")
                
                _currentSession.value?.let { session ->
                    val updatedSession = session.copy(
                        endTime = System.currentTimeMillis(),
                        isActive = false,
                        videoPath = videoPath
                    )
                    android.util.Log.d("MainViewModel", "onRecordingStopped - updatedSession: $updatedSession")
                    repository.updateSession(updatedSession)
                    _currentSession.value = updatedSession
                    android.util.Log.d("MainViewModel", "onRecordingStopped - _currentSession updated")
                    
                    // Upload the recorded file and store the file URI
                    videoPath?.let { path ->
                        val file = java.io.File(path)
                        android.util.Log.d("MainViewModel", "Checking file: $path, exists: ${file.exists()}")
                        if (file.exists()) {
                            android.util.Log.d("MainViewModel", "Calling uploadAndStoreFileUri")
                            uploadAndStoreFileUri(file, updatedSession)
                        } else {
                            android.util.Log.e("MainViewModel", "File does not exist: $path")
                        }
                    } ?: android.util.Log.e("MainViewModel", "videoPath is null")
                }
                
                _isRecording.value = false
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isLoading = true,
                    message = "Processing recording..."
                )
                
                // Set a timeout to clear loading state
                kotlinx.coroutines.delay(5000) // 5 seconds timeout
                if (_uiState.value.isLoading && _uiState.value.message?.contains("Processing") == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Recording processed successfully!"
                    )
                }
                
                // Refresh recent sessions
                loadRecentSessions()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error processing stopped recording: ${e.message}"
                )
            }
        }
    }
    
    fun startScreenRecording(mediaProjectionData: android.content.Intent?, resultCode: Int) {
        if (mediaProjectionData == null) {
            android.util.Log.e("MainViewModel", "MediaProjectionData is null")
            _uiState.value = _uiState.value.copy(
                message = "Error: No screen capture data received",
                isRecording = false
            )
            return
        }
        
        android.util.Log.d("MainViewModel", "Starting screen recording with resultCode: $resultCode")
        viewModelScope.launch {
            try {
                // Step 1: Create session
                val session = ActivitySession(
                    id = System.currentTimeMillis(),
                    startTime = System.currentTimeMillis(),
                    isActive = true
                )
                
                _currentSession.value = session
                repository.insertSession(session)
                android.util.Log.d("MainViewModel", "Session created: ${session.id}")
                
                // Step 2: Create service intent
                val serviceIntent = Intent(context, ScreenRecordingService::class.java).apply {
                    putExtra("mediaProjectionData", mediaProjectionData)
                    putExtra("resultCode", resultCode)
                    putExtra("sessionId", session.id)
                }
                
                android.util.Log.d("MainViewModel", "Starting foreground service...")
                
                // Step 3: Start foreground service
                context.startForegroundService(serviceIntent)
                
                // Step 4: Update UI state
                _isRecording.value = true
                _uiState.value = _uiState.value.copy(
                    message = "Recording started successfully!",
                    isRecording = true
                )
                
                android.util.Log.d("MainViewModel", "Recording state updated successfully")
                
                // Step 5: Refresh recent sessions
                loadRecentSessions()
                
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error starting screen recording", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to start recording: ${e.message}",
                    isRecording = false
                )
            }
        }
    }
    
    fun stopScreenRecording() {
        viewModelScope.launch {
            try {
                val serviceIntent = Intent(context, ScreenRecordingService::class.java)
                context.stopService(serviceIntent)
                
                _currentSession.value?.let { session ->
                    val updatedSession = session.copy(
                        endTime = System.currentTimeMillis(),
                        isActive = false
                    )
                    repository.updateSession(updatedSession)
                    _currentSession.value = updatedSession
                    
                    // Automatically upload the recorded file
                    val videoFile = java.io.File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
                        "AttentionAI/screen_recording_${session.id}.mp4"
                    )
                    
                    if (videoFile.exists()) {
                        uploadScreenRecording(videoFile)
                    }
                }
                
                _isRecording.value = false
                _uiState.value = _uiState.value.copy(
                    message = "Recording stopped and uploaded successfully!",
                    isRecording = false
                )
                
                // Refresh recent sessions
                loadRecentSessions()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }
    
    fun askQuestion(question: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Processing your question..."
                )
                
                val currentSessionId = _currentSession.value?.id
                android.util.Log.d("MainViewModel", "askQuestion - currentSessionId: $currentSessionId")
                android.util.Log.d("MainViewModel", "askQuestion - _currentSession.value: $_currentSession.value")
                
                val response = repository.askQuestion(question, currentSessionId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = response,
                    lastQuestion = question,
                    lastAnswer = response
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error processing question: ${e.message}"
                )
            }
        }
    }
    
    fun generateSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Generating summary..."
                )
                
                val summary = repository.generateSummary(_currentSession.value?.id)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Summary generated!",
                    currentSummary = summary
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error generating summary: ${e.message}"
                )
            }
        }
    }
    
    private fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                val sessions = repository.getRecentSessions(10)
                _uiState.value = _uiState.value.copy(recentSessions = sessions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error loading sessions: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun generateInsights() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Generating AI insights..."
                )
                
                val insights = repository.generateInsights(_currentSession.value?.id)
                _aiInsights.value = insights
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "AI insights generated successfully!"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error generating insights: ${e.message}"
                )
            }
        }
    }
    
    fun loadAIConfig() {
        viewModelScope.launch {
            try {
                val config = repository.getAIConfig()
                _aiConfig.value = config
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error loading AI configuration: ${e.message}"
                )
            }
        }
    }
    
    fun saveAIConfig(config: AIConfig) {
        viewModelScope.launch {
            try {
                repository.saveAIConfig(config)
                _aiConfig.value = config
                _uiState.value = _uiState.value.copy(
                    message = "AI configuration saved successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error saving AI configuration: ${e.message}"
                )
            }
        }
    }
    
    fun askCustomQuestion(question: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "AI is thinking..."
                )
                
                val response = repository.askQuestion(question, _currentSession.value?.id)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "AI response generated!",
                    lastQuestion = question,
                    lastAnswer = response
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error processing question: ${e.message}"
                )
            }
        }
    }
    
    private fun uploadAndStoreFileUri(file: java.io.File, session: ActivitySession) {
        viewModelScope.launch {
            try {
                val fileSizeMB = file.length() / (1024.0 * 1024.0)
                val uploadMessage = "Uploading screen recording to Gemini... (${String.format("%.1f", fileSizeMB)} MB)"
                android.util.Log.d("MainViewModel", "Setting loading state: isLoading=true, message='$uploadMessage'")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = uploadMessage
                )
                
                val fileUri = repository.uploadScreenRecording(file)
                
                if (fileUri != null) {
                    // Update the session with the file URI
                    val updatedSession = session.copy(geminiFileUri = fileUri)
                    repository.updateSession(updatedSession)
                    _currentSession.value = updatedSession
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Screen recording uploaded successfully!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Failed to upload screen recording"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error uploading screen recording: ${e.message}"
                )
            }
        }
    }
    
    fun uploadScreenRecording(file: java.io.File) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Uploading screen recording to Gemini..."
                )
                
                val fileName = repository.uploadScreenRecording(file)
                
                if (fileName != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Screen recording uploaded successfully!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Failed to upload screen recording"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error uploading screen recording: ${e.message}"
                )
            }
        }
    }
    
    fun analyzeScreenRecording(fileName: String, question: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Analyzing screen recording..."
                )
                
                val response = repository.analyzeScreenRecording(fileName, question)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Analysis complete!",
                    lastQuestion = question,
                    lastAnswer = response
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error analyzing screen recording: ${e.message}"
                )
            }
        }
    }
}

data class MainUiState(
    val permissionState: PermissionState = PermissionState.UNKNOWN,
    val overlayPermissionGranted: Boolean = false,
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val lastQuestion: String? = null,
    val lastAnswer: String? = null,
    val currentSummary: String? = null,
    val recentSessions: List<ActivitySession> = emptyList()
)

enum class PermissionState {
    UNKNOWN, GRANTED, DENIED
}
