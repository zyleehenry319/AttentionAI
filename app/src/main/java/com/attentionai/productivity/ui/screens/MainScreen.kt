package com.attentionai.productivity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.attentionai.productivity.data.model.AIConfig
import com.attentionai.productivity.data.model.ActivitySession
import com.attentionai.productivity.viewmodel.MainViewModel
import com.attentionai.productivity.viewmodel.PermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onStartScreenCapture: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onNavigateToAIConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    
    var showCustomQuestionDialog by remember { mutableStateOf(false) }
    var customQuestion by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AttentionAI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToAIConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "AI Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Status Card
            item {
                PermissionStatusCard(
                    permissionState = uiState.permissionState,
                    overlayPermissionGranted = uiState.overlayPermissionGranted,
                    onRequestPermissions = onRequestPermissions,
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
            }
            
            // Recording Control Card
            item {
                RecordingControlCard(
                    isRecording = isRecording,
                    currentSession = currentSession,
                    onStartRecording = onStartScreenCapture,
                    onStopRecording = { viewModel.stopScreenRecording() }
                )
            }
            
            // AI Status Card
            item {
                AIStatusCard(
                    aiConfig = aiConfig,
                    onConfigureAI = onNavigateToAIConfig
                )
            }
            
            // Quick Actions
            item {
                QuickActionsCard(
                    onAskQuestion = { question ->
                        viewModel.askQuestion(question)
                    },
                    onGenerateSummary = { viewModel.generateSummary() },
                    onGenerateInsights = { viewModel.generateInsights() },
                    onAskCustomQuestion = { showCustomQuestionDialog = true },
                    isLoading = uiState.isLoading
                )
            }
            
            // AI Insights
            aiInsights?.let { insights ->
                item {
                    AIInsightsCard(insights = insights)
                }
            }
            
            // AI Chat Section
            if (uiState.lastQuestion != null || uiState.lastAnswer != null) {
                item {
                    AIChatCard(
                        question = uiState.lastQuestion,
                        answer = uiState.lastAnswer
                    )
                }
            }
            
            // Summary Section
            uiState.currentSummary?.let { summary ->
                item {
                    SummaryCard(summary = summary)
                }
            }
            
            // Recent Sessions
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Sessions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            items(uiState.recentSessions) { session ->
                SessionCard(session = session)
            }
            
            // Status Message
            uiState.message?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isLoading) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Custom Question Dialog
    if (showCustomQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomQuestionDialog = false },
            title = { Text("Ask AI Assistant") },
            text = {
                Column {
                    Text("Ask any question about your phone activity:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        label = { Text("Your question") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customQuestion.isNotBlank()) {
                            viewModel.askCustomQuestion(customQuestion)
                            customQuestion = ""
                            showCustomQuestionDialog = false
                        }
                    },
                    enabled = customQuestion.isNotBlank()
                ) {
                    Text("Ask")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomQuestionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionStatusCard(
    permissionState: PermissionState,
    overlayPermissionGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (permissionState) {
                PermissionState.GRANTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                PermissionState.DENIED -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (permissionState) {
                        PermissionState.GRANTED -> Icons.Default.CheckCircle
                        PermissionState.DENIED -> Icons.Default.Warning
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = when (permissionState) {
                        PermissionState.GRANTED -> Color(0xFF4CAF50)
                        PermissionState.DENIED -> Color(0xFFF44336)
                        else -> Color(0xFFFF9800)
                    }
                )
                Text(
                    text = "Permissions Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = when (permissionState) {
                    PermissionState.GRANTED -> "All permissions granted ✓"
                    PermissionState.DENIED -> "Permissions required for app functionality"
                    else -> "Checking permissions..."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (permissionState != PermissionState.GRANTED) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
            
            if (!overlayPermissionGranted) {
                OutlinedButton(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Overlay Permission")
                }
            }
        }
    }
}

@Composable
fun RecordingControlCard(
    isRecording: Boolean,
    currentSession: com.attentionai.productivity.data.model.ActivitySession?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) 
                Color(0xFFF44336).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isRecording) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                Text(
                    text = if (isRecording) "Recording Active" else "Ready to Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isRecording && currentSession != null) {
                Text(
                    text = "Session: ${currentSession.formattedDuration}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = if (isRecording) onStopRecording else onStartRecording,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onAskQuestion: (String) -> Unit,
    onGenerateSummary: () -> Unit,
    onGenerateInsights: () -> Unit,
    onAskCustomQuestion: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateSummary,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Summary")
                }
                
                Button(
                    onClick = onGenerateInsights,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Insights")
                }
            }
            
            OutlinedButton(
                onClick = onAskCustomQuestion,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask Custom Question")
            }
        }
    }
}

@Composable
fun AIChatCard(
    question: String?,
    answer: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            question?.let {
                Text(
                    text = "Q: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            answer?.let {
                Text(
                    text = "A: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Session Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SessionCard(session: com.attentionai.productivity.data.model.ActivitySession) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.formattedStartTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = session.formattedDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (session.summary != null) {
                Text(
                    text = session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AIStatusCard(
    aiConfig: AIConfig?,
    onConfigureAI: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (aiConfig?.apiKey?.isNotBlank() == true) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (aiConfig?.apiKey?.isNotBlank() == true) 
                        Color(0xFF4CAF50) 
                    else Color(0xFFFF9800)
                )
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = if (aiConfig?.apiKey?.isNotBlank() == true) 
                    "AI is ready to help analyze your productivity!"
                else "Configure AI to get intelligent insights",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (aiConfig?.apiKey?.isNotBlank() == true) {
                Text(
                    text = "Model: ${aiConfig.model} • Max Tokens: ${aiConfig.maxTokens}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onConfigureAI,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (aiConfig?.apiKey?.isNotBlank() == true) "Configure AI" else "Setup AI")
            }
        }
    }
}

@Composable
fun AIInsightsCard(insights: com.attentionai.productivity.data.model.AIInsightResponse) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (insights.productivityScore != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Productivity Score:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(insights.productivityScore * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (insights.keyFindings.isNotEmpty()) {
                Text(
                    text = "Key Findings:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                insights.keyFindings.take(3).forEach { finding ->
                    Text(
                        text = "• $finding",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (insights.recommendations.isNotEmpty()) {
                Text(
                    text = "Recommendations:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                insights.recommendations.take(3).forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
