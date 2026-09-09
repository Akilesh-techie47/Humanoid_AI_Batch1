package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humanoidai.ai.AIManager
import com.humanoidai.ai.AIRouter
import com.humanoidai.ai.OllamaClient
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.customization.AIMode
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.voice.VoiceEngine

enum class SecurityMethod { PASSWORD, FACE_SCAN, UPDATE_PASSWORD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, 
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    voiceEngine: VoiceEngine,
    appearanceViewModel: AppearanceViewModel,
    microphoneManager: SpeechRecognizerManager,
    aiManager: AIManager,
    authViewModel: AuthViewModel = viewModel()
) {
    val settings by appearanceViewModel.settings.collectAsState()
    
    // UI State
    var aiName by remember { mutableStateOf(ownerManager.getAiName()) }
    
    var showSecurityPrompt by remember { mutableStateOf(false) }
    var securityMethod by remember { mutableStateOf<SecurityMethod?>(null) }
    var showReAuthPrompt by remember { mutableStateOf(false) }
    var pendingPassword by remember { mutableStateOf("") }
    var showDebugAi by remember { mutableStateOf(false) }

    val ownerName = ownerManager.getOwnerName()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSecurityPrompt) {
        if (securityMethod == SecurityMethod.UPDATE_PASSWORD) {
            UpdatePasswordDialog(
                authViewModel = authViewModel,
                onDismiss = { showSecurityPrompt = false },
                onReAuthRequired = { password ->
                    pendingPassword = password
                    showSecurityPrompt = false
                    showReAuthPrompt = true
                }
            )
        }
    }

    if (showReAuthPrompt) {
        ReAuthDialog(
            authViewModel = authViewModel,
            onDismiss = { showReAuthPrompt = false },
            onSuccess = {
                showReAuthPrompt = false
                authViewModel.updateAccountPassword(pendingPassword) {
                    pendingPassword = ""
                }
            }
        )
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(CornerRadiusMedium))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderWidthThin, MaterialTheme.colorScheme.outline, RoundedCornerShape(CornerRadiusMedium))
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    "SETTINGS", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SectionHeader("AI Persona")
                    AuraTextField(
                        value = aiName,
                        onValueChange = { 
                            aiName = it
                            ownerManager.setAiName(it)
                        },
                        label = "AI Name / Wake Word"
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("Appearance")
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Advanced Appearance",
                        subtitle = "Themes, colors and global scale",
                        onClick = { navController.navigate(NavRoutes.APPEARANCE) }
                    )

                    SettingsItem(
                        icon = Icons.Default.DashboardCustomize,
                        title = "System HUD Structure",
                        subtitle = settings.hudStructure.name.replace("_", " "),
                        onClick = { 
                            val structures = HUDStructure.entries
                            val nextIndex = (settings.hudStructure.ordinal + 1) % structures.size
                            appearanceViewModel.setStructure(structures[nextIndex])
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("HUD")
                    ToggleRow(Icons.Default.Analytics, "Sensor Status", settings.showSensorStatus) { 
                        appearanceViewModel.toggleComponent("sensors", it)
                    }
                    ToggleRow(Icons.Default.Label, "ROI Labels", settings.showConfidence) { 
                        appearanceViewModel.toggleComponent("confidence", it)
                    }
                    ToggleRow(Icons.Default.AutoFixHigh, "Glow Effects", settings.glowEnabled) { 
                        appearanceViewModel.toggleComponent("glow", it)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("Account")
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Identity",
                        subtitle = "Owner: $ownerName",
                        onClick = { /* No-op */ }
                    )

                    SettingsItem(
                        icon = Icons.Default.Password,
                        title = "Update Password",
                        subtitle = "Modify account security key",
                        onClick = {
                            securityMethod = SecurityMethod.UPDATE_PASSWORD
                            showSecurityPrompt = true
                        }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Sign Out",
                        subtitle = "Securely end this session",
                        onClick = {
                            ownerManager.clearOwner()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )

                    // Hidden Debug section for CES
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "v2.0.1C-CES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDebugAi = !showDebugAi },
                        textAlign = TextAlign.Center
                    )

                    if (showDebugAi) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("Diagnostic: Neural Link")
                        AiEngineSettings(appearanceViewModel, settings, aiManager)
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
                
                ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun AiEngineSettings(
    viewModel: AppearanceViewModel, 
    settings: AppearanceSettings,
    aiManager: AIManager
) {
    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val health by remember(aiManager) { derivedStateOf { aiManager.getProviderHealth() } }

    AuraPanel(title = "Neural Link Stability") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Diagnostic mode active. Dynamic routing is observing provider health.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val providers = listOf(
                "ollama-local" to "Ollama Local",
                "groq-cloud" to "Groq LPU",
                "gemini-1.5-flash" to "Google Gemini",
                "openrouter-cloud" to "OpenRouter"
            )

            providers.forEach { (id, name) ->
                val status = health[id] ?: AIRouter.ProviderStatus.AVAILABLE
                val testStatus = testResults[id]

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (status) {
                                AIRouter.ProviderStatus.AVAILABLE -> SuccessGreen
                                AIRouter.ProviderStatus.RATE_LIMITED -> WarningOrange
                                else -> ErrorRed
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (testStatus != null) {
                            Text(
                                testStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (testStatus == "PASS") SuccessGreen else ErrorRed,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                scope.launch {
                                    testResults = testResults + (id to "...")
                                    val success = aiManager.testProvider(id)
                                    testResults = testResults + (id to (if (success) "PASS" else "FAIL"))
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Test", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    AuraPanel(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(CornerRadiusSmall))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    AuraPanel(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun UpdatePasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onReAuthRequired: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.RequiresReAuth) {
            onReAuthRequired(newPassword)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AuraPanel(title = "Update Password") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Set a new access key for your account.", color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(20.dp))

                AuraTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(Modifier.height(12.dp))

                AuraTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm New Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                    Text("Passwords do not match", color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }

                AuthStatusText(authState)

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AuraOutlinedButton(text = "Cancel", modifier = Modifier.weight(1f)) { onDismiss() }
                    AuraButton(text = "Update", modifier = Modifier.weight(1f), isLoading = authState is AuthState.Loading) {
                        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                            authViewModel.updateAccountPassword(newPassword) { onDismiss() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReAuthDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        AuraPanel(title = "Recent Sign-In Required") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "For your security, please verify your current password before continuing.",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(20.dp))

                AuraTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Current Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                AuthStatusText(authState)

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AuraOutlinedButton(text = "Cancel", modifier = Modifier.weight(1f)) { onDismiss() }
                    AuraButton(text = "Verify", modifier = Modifier.weight(1f), isLoading = authState is AuthState.Loading) {
                        authViewModel.reauthenticate(password, onSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthStatusText(state: AuthState) {
    when (state) {
        is AuthState.Error -> {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.message,
                color = ErrorRed,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
        else -> {}
    }
}
