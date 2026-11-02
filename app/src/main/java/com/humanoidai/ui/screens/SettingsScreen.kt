package com.humanoidai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humanoidai.ai.AIManager
import com.humanoidai.ai.AIRouter
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.launch

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
    var aiName by remember { mutableStateOf(ownerManager.getAiName()) }

    var showSecurityPrompt by remember { mutableStateOf(false) }
    var securityMethod by remember { mutableStateOf<SecurityMethod?>(null) }
    var showReAuthPrompt by remember { mutableStateOf(false) }
    var pendingPassword by remember { mutableStateOf("") }
    var showDebugAi by remember { mutableStateOf(false) }

    var ownerName by remember { mutableStateOf("Owner") }
    LaunchedEffect(Unit) {
        ownerName = ownerManager.getOwnerName()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    if (showSecurityPrompt && securityMethod == SecurityMethod.UPDATE_PASSWORD) {
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
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Section 1: AI Persona & Identity
                SettingsSectionHeader("AI PERSONA & IDENTITY")
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ASSISTANT NAME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        AuraTextField(
                            value = aiName,
                            onValueChange = {
                                aiName = it
                                ownerManager.setAiName(it)
                            },
                            label = "",
                            placeholder = "Aura 360",
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.SmartToy, null, tint = AccentCyan)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Appearance & Theme
                SettingsSectionHeader("APPEARANCE & DISPLAY")
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ModernToggleRow(
                            icon = Icons.Outlined.DarkMode,
                            label = "Dark Theme",
                            checked = settings.isDarkMode,
                            onCheckedChange = { appearanceViewModel.setDarkMode(it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernSettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = "Theme Calibration",
                            subtitle = "Colors, fonts & global UI scale",
                            onClick = { navController.navigate(NavRoutes.APPEARANCE) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernSettingsItem(
                            icon = Icons.Outlined.DashboardCustomize,
                            title = "HUD Structure",
                            subtitle = settings.hudStructure.name.replace("_", " "),
                            onClick = {
                                val structures = HUDStructure.entries
                                val nextIndex = (settings.hudStructure.ordinal + 1) % structures.size
                                appearanceViewModel.setStructure(structures[nextIndex])
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Perception & Vision HUD Controls
                SettingsSectionHeader("VISION HUD & SENSORS")
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ModernToggleRow(
                            icon = Icons.Outlined.Sensors,
                            label = "Sensor Telemetry",
                            checked = settings.showSensorStatus,
                            onCheckedChange = { appearanceViewModel.toggleComponent("sensors", it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernToggleRow(
                            icon = Icons.Outlined.CropFree,
                            label = "ROI Detection Boxes",
                            checked = settings.showConfidence,
                            onCheckedChange = { appearanceViewModel.toggleComponent("confidence", it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernToggleRow(
                            icon = Icons.Outlined.AutoFixHigh,
                            label = "Glow FX",
                            checked = settings.glowEnabled,
                            onCheckedChange = { appearanceViewModel.toggleComponent("glow", it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Biometrics & Security
                SettingsSectionHeader("BIOMETRICS & SECURITY")
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ModernSettingsItem(
                            icon = Icons.Outlined.Face,
                            title = "Re-scan Face Biometrics",
                            subtitle = "Update 60-sample facial model",
                            onClick = {
                                navController.navigate("${NavRoutes.OWNER_ENROLLMENT}?isUpdateMode=true&updateType=face")
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernSettingsItem(
                            icon = Icons.Outlined.Mic,
                            title = "Re-record Voice Trigger",
                            subtitle = "Verify 'Aura' command phrase",
                            onClick = {
                                navController.navigate("${NavRoutes.OWNER_ENROLLMENT}?isUpdateMode=true&updateType=voice")
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        ModernSettingsItem(
                            icon = Icons.Outlined.Password,
                            title = "Update Master Password",
                            subtitle = "Modify emergency access key",
                            onClick = {
                                securityMethod = SecurityMethod.UPDATE_PASSWORD
                                showSecurityPrompt = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 5: Account & Session
                SettingsSectionHeader("ACCOUNT & SESSION")
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModernSettingsItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        subtitle = "End active session for $ownerName",
                        iconTint = ErrorRed,
                        onClick = {
                            ownerManager.clearOwner()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Diagnostics toggle trigger
                Text(
                    text = "Aura 360 v2.0.1 • Neural Engine Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDebugAi = !showDebugAi }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                if (showDebugAi) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AiEngineSettings(settings, aiManager)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun ModernSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ModernToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun AiEngineSettings(
    settings: AppearanceSettings,
    aiManager: AIManager
) {
    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val health by remember(aiManager) { derivedStateOf { aiManager.getProviderHealth() } }

    Surface(
        shape = RoundedCornerShape(CornerRadiusLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "NEURAL PROVIDER HEALTH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                                text = testStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
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
        Surface(
            shape = RoundedCornerShape(CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Update Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Set a new access key for your account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                AuraTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password",
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                AuraTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm New Password",
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                    Text(
                        text = "Passwords do not match",
                        color = ErrorRed,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                AuthStatusText(authState)

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraOutlinedButton(
                        text = "Cancel",
                        modifier = Modifier.weight(1f)
                    ) { onDismiss() }

                    AuraButton(
                        text = "Update",
                        modifier = Modifier.weight(1f),
                        isLoading = authState is AuthState.Loading
                    ) {
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
        Surface(
            shape = RoundedCornerShape(CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Verify Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Please confirm your current password to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                AuraTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Current Password",
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                AuthStatusText(authState)

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraOutlinedButton(
                        text = "Cancel",
                        modifier = Modifier.weight(1f)
                    ) { onDismiss() }

                    AuraButton(
                        text = "Verify",
                        modifier = Modifier.weight(1f),
                        isLoading = authState is AuthState.Loading
                    ) {
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
