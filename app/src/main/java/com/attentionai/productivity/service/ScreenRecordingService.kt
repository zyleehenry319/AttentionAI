package com.attentionai.productivity.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.attentionai.productivity.MainActivity
import com.attentionai.productivity.R
import com.attentionai.productivity.data.api.GeminiRestService
import com.attentionai.productivity.data.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecordingService : Service() {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var sessionId: Long = 0
    private lateinit var repository: ActivityRepository
    private lateinit var geminiRestService: GeminiRestService
    private var audioCheckJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_recording_channel"
        private const val CHANNEL_NAME = "Screen Recording"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        repository = ActivityRepository(this)
        geminiRestService = GeminiRestService(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP_RECORDING" -> {
                android.util.Log.d("ScreenRecordingService", "Stop recording requested from notification")
                stopRecording()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // ALWAYS start foreground service first
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                    } else {
                        startForeground(NOTIFICATION_ID, createNotification())
                    }
                    android.util.Log.d("ScreenRecordingService", "Foreground service started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Error starting foreground service", e)
                    stopSelf()
                    return START_NOT_STICKY
                }
                
        val mediaProjectionData = intent?.getParcelableExtra<Intent>("mediaProjectionData")
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        sessionId = intent?.getLongExtra("sessionId", 0) ?: 0
        
        if (mediaProjectionData != null && resultCode != 0) {
                    // Now start recording after foreground service is running
            startRecording(mediaProjectionData, resultCode)
                } else {
                    android.util.Log.d("ScreenRecordingService", "No recording data provided, service running without recording")
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startRecording(mediaProjectionData: Intent, resultCode: Int) {
        try {
            android.util.Log.d("ScreenRecordingService", "Starting recording with resultCode: $resultCode")
            
            // Step 1: Get MediaProjection
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, mediaProjectionData)
            
            if (mediaProjection == null) {
                android.util.Log.e("ScreenRecordingService", "Failed to get MediaProjection")
                stopSelf()
                return
            }
            
            // Register MediaProjection callback
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    android.util.Log.d("ScreenRecordingService", "MediaProjection stopped")
                    stopSelf()
                }
            }, null)
            
            android.util.Log.d("ScreenRecordingService", "MediaProjection obtained and callback registered successfully")
            
            // Step 2: Setup MediaRecorder
            try {
            setupMediaRecorder()
                android.util.Log.d("ScreenRecordingService", "MediaRecorder setup completed")
            } catch (e: Exception) {
                android.util.Log.e("ScreenRecordingService", "Error setting up MediaRecorder", e)
                stopSelf()
                return
            }
            
            // Step 3: Audio is now handled in setupMediaRecorder()
            
            // Step 4: Create Virtual Display
            val displayMetrics = resources.displayMetrics
            val density = displayMetrics.densityDpi
            
            // Use medium resolution for recording
            val recordingWidth = 360
            val recordingHeight = 640
            
            try {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecord",
                    recordingWidth,
                    recordingHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )
            
                if (virtualDisplay == null) {
                    android.util.Log.e("ScreenRecordingService", "Failed to create VirtualDisplay")
                    stopSelf()
                    return
                }
                
                android.util.Log.d("ScreenRecordingService", "VirtualDisplay created successfully")
            } catch (e: Exception) {
                android.util.Log.e("ScreenRecordingService", "Error creating VirtualDisplay", e)
                stopSelf()
                return
            }
            
            // Step 5: Start Recording
            try {
            mediaRecorder?.start()
                android.util.Log.d("ScreenRecordingService", "MediaRecorder started successfully")
            } catch (e: Exception) {
                android.util.Log.e("ScreenRecordingService", "Error starting MediaRecorder", e)
                stopSelf()
                return
            }
            
            // Step 6: Foreground service already started in onStartCommand
            android.util.Log.d("ScreenRecordingService", "Foreground service already running")
            
            android.util.Log.d("ScreenRecordingService", "Recording started successfully")
            
            // Start audio monitoring for long recordings
            startAudioMonitoring()
            
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecordingService", "Unexpected error in startRecording", e)
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun setupMediaRecorder() {
        try {
            android.util.Log.d("ScreenRecordingService", "Setting up MediaRecorder...")
            
            // Check system audio support first
            checkSystemAudioSupport()
            
            // Release any existing MediaRecorder
            mediaRecorder?.release()
            mediaRecorder = MediaRecorder()
            
            // Step 1: Set audio source first (required order) - SYSTEM AUDIO ONLY
            var audioSourceSet = false
            val audioSources = mutableListOf<Pair<Int, String>>()
            
            // Add system audio sources based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioSources.add(MediaRecorder.AudioSource.REMOTE_SUBMIX to "REMOTE_SUBMIX")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                audioSources.add(MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED")
            }
            // Add VOICE_COMMUNICATION as fallback for system audio
            audioSources.add(MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION")
            
            for ((source, name) in audioSources) {
                try {
                    mediaRecorder?.setAudioSource(source)
                    android.util.Log.d("ScreenRecordingService", "Audio source set successfully: $name")
                    audioSourceSet = true
                    break
                } catch (e: Exception) {
                    android.util.Log.w("ScreenRecordingService", "Failed to set audio source $name: ${e.message}")
                    // Reset MediaRecorder and try next source
                    try {
                        mediaRecorder?.release()
                        mediaRecorder = MediaRecorder()
                    } catch (resetError: Exception) {
                        android.util.Log.w("ScreenRecordingService", "Error resetting MediaRecorder", resetError)
                    }
                }
            }
            
            if (!audioSourceSet) {
                android.util.Log.e("ScreenRecordingService", "CRITICAL: Could not set any system audio source!")
                android.util.Log.e("ScreenRecordingService", "System audio capture failed - this may be due to:")
                android.util.Log.e("ScreenRecordingService", "1. Missing system audio permissions")
                android.util.Log.e("ScreenRecordingService", "2. Device doesn't support system audio capture")
                android.util.Log.e("ScreenRecordingService", "3. App not running with proper privileges")
                android.util.Log.w("ScreenRecordingService", "Continuing with video-only recording")
                
                // Create a new MediaRecorder instance for video-only recording
                try {
                    mediaRecorder?.release()
                    mediaRecorder = MediaRecorder()
                    android.util.Log.d("ScreenRecordingService", "Created new MediaRecorder for video-only recording")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Failed to create new MediaRecorder", e)
                    throw e
                }
            }
            
            // Step 2: Set video source (only if MediaRecorder is valid)
            if (mediaRecorder != null) {
                try {
                    mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    android.util.Log.d("ScreenRecordingService", "Video source set")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Failed to set video source", e)
                    throw e
                }
            } else {
                android.util.Log.e("ScreenRecordingService", "MediaRecorder is null, cannot set video source")
                throw IllegalStateException("MediaRecorder is null")
            }
            
            // Step 3: Set output format
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            android.util.Log.d("ScreenRecordingService", "Output format set")
            
            // Step 4: Set audio encoder
            try {
                mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                android.util.Log.d("ScreenRecordingService", "Audio encoder set (AAC)")
            } catch (e: Exception) {
                android.util.Log.w("ScreenRecordingService", "Could not set audio encoder", e)
            }
            
            // Step 5: Set video encoder
            // Try HEVC to shrink files, fall back to H.264
            try {
                mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
            } catch (_: Exception) {
                mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            }
            android.util.Log.d("ScreenRecordingService", "Video encoder set")
            
            // Step 6: Set output file
            outputFile = createOutputFile("screen_recording_${sessionId}.mp4")
            mediaRecorder?.setOutputFile(outputFile!!.absolutePath)
            android.util.Log.d("ScreenRecordingService", "Output file set: ${outputFile!!.absolutePath}")
            
            // Step 7: Set video properties
            mediaRecorder?.setVideoSize(360, 640)
            mediaRecorder?.setVideoEncodingBitRate(800000)
            mediaRecorder?.setVideoFrameRate(15)
            android.util.Log.d("ScreenRecordingService", "Video properties set")
            
            // Step 8: Set audio properties only if audio source was set
            if (audioSourceSet) {
                try {
                    // Use lower bitrate and sampling rate for better stability in long recordings
                    mediaRecorder?.setAudioSamplingRate(22050) // Lower sampling rate for stability
                    mediaRecorder?.setAudioEncodingBitRate(64000) // Lower bitrate for stability
                    android.util.Log.d("ScreenRecordingService", "Audio properties set (22050Hz, 64kbps)")
                } catch (e: Exception) {
                    android.util.Log.w("ScreenRecordingService", "Could not set audio properties", e)
                }
            } else {
                android.util.Log.d("ScreenRecordingService", "Skipping audio properties - no audio source set")
            }
            
            // Step 9: Prepare MediaRecorder
            mediaRecorder?.prepare()
            android.util.Log.d("ScreenRecordingService", "MediaRecorder prepared successfully")
            
            // Add error callback to track audio issues
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder?.setOnErrorListener { mr, what, extra ->
                    android.util.Log.e("ScreenRecordingService", "MediaRecorder error: what=$what, extra=$extra")
                    if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                        android.util.Log.e("ScreenRecordingService", "Unknown MediaRecorder error - audio may have stopped")
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecordingService", "Error setting up MediaRecorder", e)
            throw e
        }
    }
    
    
    private fun createOutputFile(fileName: String): File {
        val mediaDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+), use app-specific external storage
            File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "AttentionAI")
        } else {
            // For older versions, use public Movies directory
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AttentionAI")
        }
        
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        val file = File(mediaDir, fileName)
        android.util.Log.d("ScreenRecordingService", "Creating output file: ${file.absolutePath}")
        return file
    }
    
    private fun createNotificationChannel() {
        try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen recording in progress"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
                android.util.Log.d("ScreenRecordingService", "Notification channel created successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecordingService", "Error creating notification channel", e)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create stop recording action
        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = "STOP_RECORDING"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recording Active")
            .setContentText("Tap to open app or use action to stop recording")
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Recording",
                stopPendingIntent
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
    
    private fun checkSystemAudioSupport() {
        android.util.Log.d("ScreenRecordingService", "Checking system audio support...")
        android.util.Log.d("ScreenRecordingService", "Android version: ${Build.VERSION.SDK_INT}")
        android.util.Log.d("ScreenRecordingService", "REMOTE_SUBMIX available: ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP}")
        android.util.Log.d("ScreenRecordingService", "UNPROCESSED available: ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q}")
        
        // Check if we have the required permissions
        val hasRecordAudio = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCaptureAudioOutput = checkSelfPermission(android.Manifest.permission.CAPTURE_AUDIO_OUTPUT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("ScreenRecordingService", "RECORD_AUDIO permission: $hasRecordAudio")
        android.util.Log.d("ScreenRecordingService", "CAPTURE_AUDIO_OUTPUT permission: $hasCaptureAudioOutput")
    }
    
    private fun startAudioMonitoring() {
        audioCheckJob = serviceScope.launch {
            while (mediaRecorder != null) {
                kotlinx.coroutines.delay(30000) // Check every 30 seconds
                try {
                    // Log audio status
                    android.util.Log.d("ScreenRecordingService", "Audio monitoring check - MediaRecorder active: ${mediaRecorder != null}")
                } catch (e: Exception) {
                    android.util.Log.w("ScreenRecordingService", "Audio monitoring error", e)
                }
            }
        }
    }
    
    private fun stopAudioMonitoring() {
        audioCheckJob?.cancel()
        audioCheckJob = null
    }
    
    private fun stopRecording() {
        try {
            // Stop audio monitoring
            stopAudioMonitoring()
            
            // Stop MediaRecorder safely
            mediaRecorder?.let { recorder ->
                try {
                    recorder.stop()
                    android.util.Log.d("ScreenRecordingService", "MediaRecorder stopped successfully")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Error stopping MediaRecorder", e)
                } finally {
                    try {
                        recorder.release()
                        android.util.Log.d("ScreenRecordingService", "MediaRecorder released")
                    } catch (e: Exception) {
                        android.util.Log.e("ScreenRecordingService", "Error releasing MediaRecorder", e)
                    }
                }
            }
            
            // Release VirtualDisplay
            virtualDisplay?.let { display ->
                try {
                    display.release()
                    android.util.Log.d("ScreenRecordingService", "VirtualDisplay released")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Error releasing VirtualDisplay", e)
                }
            }
            
            // Stop MediaProjection
            mediaProjection?.let { projection ->
                try {
                    projection.stop()
                    android.util.Log.d("ScreenRecordingService", "MediaProjection stopped")
                } catch (e: Exception) {
                    android.util.Log.e("ScreenRecordingService", "Error stopping MediaProjection", e)
                }
            }
            
            // Use the actual output file that was created during recording
            val videoFile = outputFile
            if (videoFile != null) {
                // Log the created files for debugging
                android.util.Log.d("ScreenRecordingService", "Video file created: ${videoFile.exists()}")
                android.util.Log.d("ScreenRecordingService", "Video file size: ${videoFile.length()} bytes")
                
                // Directly upload the file to Gemini
                serviceScope.launch {
                    try {
                        android.util.Log.d("ScreenRecordingService", "Starting direct upload to Gemini...")
                        val fileUri = repository.uploadScreenRecording(videoFile)
                        if (fileUri != null) {
                            android.util.Log.d("ScreenRecordingService", "File uploaded successfully to Gemini: $fileUri")
                        } else {
                            android.util.Log.e("ScreenRecordingService", "Failed to upload file to Gemini")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ScreenRecordingService", "Error uploading file to Gemini", e)
                    }
                }
                
                // Also send broadcast for UI updates
                val intent = Intent("com.attentionai.productivity.RECORDING_STOPPED")
                intent.putExtra("sessionId", sessionId)
                intent.putExtra("videoPath", videoFile.absolutePath)
                
                android.util.Log.d("ScreenRecordingService", "Sending broadcast with sessionId: $sessionId")
                android.util.Log.d("ScreenRecordingService", "Sending broadcast with videoPath: ${videoFile.absolutePath}")
                
                sendBroadcast(intent)
                
                android.util.Log.d("ScreenRecordingService", "Broadcast sent successfully")
            } else {
                android.util.Log.e("ScreenRecordingService", "Output file is null, cannot upload or notify MainActivity")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecordingService", "Error stopping recording", e)
            e.printStackTrace()
        }
    }
}
