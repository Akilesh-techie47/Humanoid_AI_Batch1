package com.humanoidai.ui.screens

import android.Manifest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanoidai.permission.PermissionManager
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// LoginScreen
// -----------------------------------------------------------------
// First screen shown to the user (start destination in NavGraph).
// Two tabs: "Phone" (OTP) and "Email" (password).
// On successful auth, calls onLoginSuccess() which navigates to Splash/Dashboard.
// -----------------------------------------------------------------

enum class LoginTab { PHONE, EMAIL }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    voiceEngine: com.humanoidai.voice.VoiceEngine
) {
    var selectedTab by remember { mutableStateOf(LoginTab.PHONE) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val alpha = remember { Animatable(0f) }

    // Permission Launcher
    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        }
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(1200))
        voiceEngine.speak("Welcome to Humanoid AI. Please sign in using your phone or email.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---- Logo / Title ----
            Text(
                text = "Humanoid AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Tab Switcher ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "Phone",
                    selected = selectedTab == LoginTab.PHONE,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = LoginTab.PHONE }

                TabButton(
                    text = "Email",
                    selected = selectedTab == LoginTab.EMAIL,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = LoginTab.EMAIL }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Tab Content ----
            when (selectedTab) {
                LoginTab.PHONE -> PhoneOtpForm(authViewModel, onLoginSuccess, microphoneManager) {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                LoginTab.EMAIL -> EmailPasswordForm(authViewModel, onLoginSuccess, microphoneManager) {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// Tab Button
// -----------------------------------------------------------------
@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val bgColor = if (selected) AccentCyan else Color.Transparent
    val textColor = if (selected) Color.Black else TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 10.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// -----------------------------------------------------------------
// Phone OTP Form
// -----------------------------------------------------------------
@Composable
private fun PhoneOtpForm(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    onPermissionRequired: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val isListening by microphoneManager.isListening.collectAsState()

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        StyledTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone number (+91...)",
            keyboardType = KeyboardType.Phone,
            enabled = !otpSent,
            microphoneManager = microphoneManager,
            isListening = isListening,
            onMicClick = {
                if (com.humanoidai.permission.PermissionManager.hasRecordAudioPermission(context)) {
                    if (isListening) microphoneManager.stopListening()
                    else microphoneManager.startListening(onFinalResult = { 
                        phone = it 
                        // Automatically stop listening for single-field input
                        microphoneManager.stopListening()
                    })
                } else {
                    onPermissionRequired()
                }
            }
        )

        if (otpSent) {
            Spacer(modifier = Modifier.height(12.dp))
            StyledTextField(
                value = otp,
                onValueChange = { otp = it },
                label = "Enter OTP",
                keyboardType = KeyboardType.Number,
                microphoneManager = microphoneManager,
                isListening = isListening,
                onMicClick = {
                    if (com.humanoidai.permission.PermissionManager.hasRecordAudioPermission(context)) {
                        if (isListening) microphoneManager.stopListening()
                        else microphoneManager.startListening(onFinalResult = { 
                            otp = it 
                            microphoneManager.stopListening()
                        })
                    } else {
                        onPermissionRequired()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = if (otpSent) "Verify OTP" else "Send OTP",
            enabled = authState !is AuthState.Loading
        ) {
            if (!otpSent) {
                authViewModel.sendOtp(
                    phoneNumber = phone,
                    activity = context as? android.app.Activity,
                    onCodeSent = { otpSent = true },
                    onAutoSignIn = onLoginSuccess
                )
            } else {
                authViewModel.verifyOtp(otp, onLoginSuccess)
            }
        }

        AuthStatusText(authState)
    }
}

// -----------------------------------------------------------------
// Email/Password Form
// -----------------------------------------------------------------
@Composable
private fun EmailPasswordForm(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    onPermissionRequired: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val isListening by microphoneManager.isListening.collectAsState()

    if (showResetDialog) {
        PasswordResetDialog(
            email = email,
            onDismiss = { showResetDialog = false },
            onResetRequested = { resetEmail ->
                authViewModel.sendPasswordReset(resetEmail) {
                    showResetDialog = false
                    // Could show a toast here
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        StyledTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            microphoneManager = microphoneManager,
            isListening = isListening,
            onMicClick = {
                if (com.humanoidai.permission.PermissionManager.hasRecordAudioPermission(context)) {
                    if (isListening) microphoneManager.stopListening()
                    else microphoneManager.startListening(onFinalResult = { 
                        email = it 
                        microphoneManager.stopListening()
                    })
                } else {
                    onPermissionRequired()
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        StyledTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            microphoneManager = microphoneManager,
            isListening = isListening,
            onMicClick = {
                if (com.humanoidai.permission.PermissionManager.hasRecordAudioPermission(context)) {
                    if (isListening) microphoneManager.stopListening()
                    else microphoneManager.startListening(onFinalResult = { 
                        password = it 
                        microphoneManager.stopListening()
                    })
                } else {
                    onPermissionRequired()
                }
            },
            trailingIconContent = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle Password Visibility",
                        tint = AccentCyan
                    )
                }
            }
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(12.dp))
            StyledTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Re-enter Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                microphoneManager = microphoneManager,
                isListening = isListening,
                onMicClick = {
                    if (PermissionManager.hasRecordAudioPermission(context)) {
                        if (isListening) microphoneManager.stopListening()
                        else microphoneManager.startListening(onFinalResult = { 
                            confirmPassword = it 
                            microphoneManager.stopListening()
                        })
                    } else {
                        onPermissionRequired()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = if (isSignUp) "Create Account" else "Login",
            enabled = authState !is AuthState.Loading
        ) {
            if (isSignUp) {
                if (password != confirmPassword) {
                    // This could be handled by AuthState.Error internally if I wanted to update ViewModel,
                    // but simple UI check is faster for user.
                    // authViewModel.setLocalError("Passwords do not match") // If method existed
                    // For now, I'll just check it here.
                    return@PrimaryButton
                }
                authViewModel.signUpWithEmail(email, password, onLoginSuccess)
            } else {
                authViewModel.signInWithEmail(email, password, onLoginSuccess)
            }
        }

        if (isSignUp && password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text("Passwords do not match", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isSignUp) {
            Text(
                "Forgot password?",
                color = AccentCyan,
                fontSize = 13.sp,
                modifier = Modifier.clickable { showResetDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = if (isSignUp) "Already have an account? Login" else "New here? Create an account",
            color = AccentCyan,
            fontSize = 13.sp,
            modifier = Modifier.clickable { 
                isSignUp = !isSignUp 
                confirmPassword = ""
            }
        )

        AuthStatusText(authState)
    }
}

@Composable
private fun PasswordResetDialog(
    email: String,
    onDismiss: () -> Unit,
    onResetRequested: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(email) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", color = Color.White) },
        text = {
            Column {
                Text("Enter your email to receive a reset link.", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = AccentCyan,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onResetRequested(resetEmail) }) {
                Text("Send Link", color = AccentCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = BackgroundDark
    )
}

// -----------------------------------------------------------------
// Shared UI helpers
// -----------------------------------------------------------------
@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager? = null,
    isListening: Boolean = false,
    onMicClick: () -> Unit = {},
    trailingIconContent: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = TextSecondary,
            cursorColor = AccentCyan,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            disabledContainerColor = SurfaceDark,
            disabledTextColor = TextSecondary,
            disabledBorderColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingIconContent?.invoke()
                if (microphoneManager != null) {
                    IconButton(onClick = onMicClick) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            null,
                            tint = if (isListening) Color.Red else AccentCyan
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCyan,
            disabledContainerColor = AccentCyan.copy(alpha = 0.4f)
        )
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, color = Color.Black, fontWeight = FontWeight.SemiBold)
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
                color = Color(0xFFFF5C5C),
                fontSize = 12.sp
            )
        }
        else -> {}
    }
}
