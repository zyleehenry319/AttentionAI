package com.attentionai.productivity.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.attentionai.productivity.data.model.AIConfig
import com.attentionai.productivity.data.model.ActivitySession
import com.attentionai.productivity.viewmodel.MainViewModel
import com.attentionai.productivity.viewmodel.PermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalistMainScreen(
    viewModel: MainViewModel,
    onStartScreenCapture: () -> Unit,
    onNavigateToAIConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    
    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Full-screen loading overlay for video processing/upload
        android.util.Log.d("MinimalistMainScreen", "UI State - isLoading: ${uiState.isLoading}, message: '${uiState.message}'")
        val shouldShowLoadingOverlay = uiState.isLoading && (
            uiState.message?.contains("Uploading", ignoreCase = true) == true ||
            uiState.message?.contains("Processing", ignoreCase = true) == true
        )
        android.util.Log.d("MinimalistMainScreen", "Should show loading overlay: $shouldShowLoadingOverlay")
        if (shouldShowLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = uiState.message ?: "Uploading...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Please wait while your screen recording is being uploaded to Gemini",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "AttentionAI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Recording Status
            RecordingStatusCard(
                isRecording = isRecording,
                currentSession = currentSession,
                onStartRecording = onStartScreenCapture,
                onStopRecording = { viewModel.stopScreenRecording() }
            )
            
            // AI Chat Interface
            if (aiConfig?.apiKey?.isNotBlank() == true) {
                AIChatInterface(
                    onAskQuestion = { question ->
                        viewModel.askQuestion(question)
                    },
                    onAskCustomQuestion = { showQuestionDialog = true },
                    isLoading = uiState.isLoading,
                    lastQuestion = uiState.lastQuestion,
                    lastAnswer = uiState.lastAnswer,
                    loadingMessage = uiState.message
                )
            } else {
                AISetupCard(
                    onConfigureAI = onNavigateToAIConfig
                )
            }
            
            // Recent Activity
            if (uiState.recentSessions.isNotEmpty()) {
                RecentActivityCard(
                    sessions = uiState.recentSessions,
                    onSessionClick = { session ->
                        // Handle session click
                    }
                )
            }
        }
        
        // Settings Button
        FloatingActionButton(
            onClick = onNavigateToAIConfig,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
    
    // Question Dialog
    if (showQuestionDialog) {
        QuestionDialog(
            question = questionText,
            onQuestionChange = { questionText = it },
            onAsk = {
                if (questionText.isNotBlank()) {
                    viewModel.askCustomQuestion(questionText)
                    questionText = ""
                    showQuestionDialog = false
                }
            },
            onDismiss = { showQuestionDialog = false }
        )
    }
}

@Composable
fun RecordingStatusCard(
    isRecording: Boolean,
    currentSession: ActivitySession?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = if (isRecording) "Recording Active" else "Ready to Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (isRecording && currentSession != null) {
                Text(
                    text = currentSession.formattedDuration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Button(
                onClick = if (isRecording) onStopRecording else onStartRecording,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }
        }
    }
}

@Composable
fun AIChatInterface(
    onAskQuestion: (String) -> Unit,
    onAskCustomQuestion: () -> Unit,
    isLoading: Boolean,
    lastQuestion: String?,
    lastAnswer: String?,
    loadingMessage: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ask About Your Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Quick Questions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAskQuestion("What apps did I use today?") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Apps Used")
                }
                
                OutlinedButton(
                    onClick = { onAskQuestion("How productive was I?") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Productivity")
                }
            }
            
            Button(
                onClick = onAskCustomQuestion,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask Custom Question")
            }
            
            // AI Response
            if (lastQuestion != null || lastAnswer != null) {
                Divider()
                
                lastQuestion?.let { question ->
                    Text(
                        text = "Q: $question",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                lastAnswer?.let { answer ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp) // Limit height to 200dp
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = "A: $answer",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loadingMessage ?: "AI is thinking...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AISetupCard(
    onConfigureAI: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Setup AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Configure Gemini AI to get intelligent insights about your activity",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onConfigureAI,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Setup AI")
            }
        }
    }
}

@Composable
fun RecentActivityCard(
    sessions: List<ActivitySession>,
    onSessionClick: (ActivitySession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions.take(3)) { session ->
                    SessionItem(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    session: ActivitySession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.formattedStartTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (session.summary != null) {
                    Text(
                        text = session.summary.take(50) + if (session.summary.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = session.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QuestionDialog(
    question: String,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask AI Assistant") },
        text = {
            Column {
                Text("Ask any question about your phone activity:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    label = { Text("Your question") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAsk,
                enabled = question.isNotBlank()
            ) {
                Text("Ask")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
