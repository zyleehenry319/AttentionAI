package com.attentionai.productivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attentionai.productivity.ui.screens.MinimalistMainScreen
import com.attentionai.productivity.ui.screens.AIConfigScreen
import com.attentionai.productivity.ui.theme.AttentionAITheme
import com.attentionai.productivity.viewmodel.MainViewModel
import com.attentionai.productivity.data.model.AIConfig

class MainActivity : ComponentActivity() {
    
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var viewModel: MainViewModel
    
    private val recordingStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("MainActivity", "Broadcast received: ${intent?.action}")
            if (intent?.action == "com.attentionai.productivity.RECORDING_STOPPED") {
                val sessionId = intent.getLongExtra("sessionId", 0)
                val videoPath = intent.getStringExtra("videoPath")
                android.util.Log.d("MainActivity", "Recording stopped for session $sessionId, video: $videoPath")
                viewModel.onRecordingStopped(sessionId, videoPath)
            } else {
                android.util.Log.d("MainActivity", "Ignoring broadcast with action: ${intent?.action}")
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("MainActivity", "All permissions granted, starting screen capture")
            viewModel.onPermissionsGranted()
            // Start screen capture after permissions are granted
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        } else {
            android.util.Log.w("MainActivity", "Some permissions denied: $permissions")
            viewModel.onPermissionsDenied()
        }
    }
    
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            viewModel.startScreenRecording(data, result.resultCode)
        } else {
            viewModel.onPermissionsDenied()
        }
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            viewModel.onOverlayPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            viewModel = MainViewModel(application)
            
            // Register broadcast receiver
            try {
                val filter = IntentFilter("com.attentionai.productivity.RECORDING_STOPPED")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(recordingStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(recordingStoppedReceiver, filter)
                }
                android.util.Log.d("MainActivity", "Broadcast receiver registered successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error registering broadcast receiver", e)
            }
            
            setContent {
                AttentionAITheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var showAIConfig by remember { mutableStateOf(false) }
                        val aiConfig by viewModel.aiConfig.collectAsState()
                        
                        if (showAIConfig) {
                            AIConfigScreen(
                                currentConfig = aiConfig ?: AIConfig(apiKey = ""),
                                onConfigSaved = { config ->
                                    viewModel.saveAIConfig(config)
                                    showAIConfig = false
                                },
                                onBackPressed = { showAIConfig = false }
                            )
                        } else {
                            MinimalistMainScreen(
                                viewModel = viewModel,
                                onStartScreenCapture = ::startScreenCapture,
                                onNavigateToAIConfig = { showAIConfig = true }
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            // Show a simple error screen
            setContent {
                AttentionAITheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = "App initialization error: ${e.message}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(recordingStoppedReceiver)
    }
    
    private fun requestAllPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE
        )
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun requestPermissions() {
        requestAllPermissions()
    }
    
    private fun startScreenCapture() {
        // Check if we have the required permissions first
        val requiredPermissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        
        // Only request WRITE_EXTERNAL_STORAGE for older Android versions
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        val missingPermissions = requiredPermissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missingPermissions.isNotEmpty()) {
            android.util.Log.d("MainActivity", "Requesting missing permissions: $missingPermissions")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            android.util.Log.d("MainActivity", "All permissions granted, starting screen capture")
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        }
    }
    
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            overlayPermissionLauncher.launch(intent)
        } else {
            viewModel.onOverlayPermissionGranted()
        }
    }
}
