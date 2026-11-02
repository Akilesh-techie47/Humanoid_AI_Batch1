package com.humanoidai.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanoidai.R
import com.humanoidai.ui.theme.*

enum class LoginTab { PHONE, EMAIL }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableStateOf(LoginTab.PHONE) }
    var isSignUp by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken as? String
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken, onLoginSuccess)
                } else {
                    authViewModel.signOut()
                }
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    7 -> "Network error. Please check your connection."
                    12501 -> "Sign-in cancelled."
                    10 -> "Configuration error. Please contact support."
                    else -> "Google sign-in failed (${e.statusCode})"
                }
                authViewModel.setError(msg)
            }
        }
    }

    val signInWithGoogle = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Modern App Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Aura 360 Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aura 360",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Your AI companion with vision and voice intelligence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Modern Social Login Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraOutlinedButton(
                        text = "Continue with Google",
                        leadingIcon = Icons.Outlined.AccountCircle,
                        onClick = { signInWithGoogle() }
                    )

                    AuraOutlinedButton(
                        text = "Continue as Guest",
                        leadingIcon = Icons.Outlined.PersonOutline,
                        onClick = { authViewModel.signInAnonymously(onLoginSuccess) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "  OR  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tab Switcher Pill
                Surface(
                    shape = RoundedCornerShape(CornerRadiusPill),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        TabButton(
                            text = "Phone Number",
                            selected = selectedTab == LoginTab.PHONE,
                            modifier = Modifier.weight(1f)
                        ) { selectedTab = LoginTab.PHONE }

                        TabButton(
                            text = "Email & Password",
                            selected = selectedTab == LoginTab.EMAIL,
                            modifier = Modifier.weight(1f)
                        ) { selectedTab = LoginTab.EMAIL }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Auth Form Card
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        when (selectedTab) {
                            LoginTab.PHONE -> PhoneOtpForm(authViewModel, onLoginSuccess)
                            LoginTab.EMAIL -> EmailPasswordForm(authViewModel, isSignUp, { isSignUp = it }, onLoginSuccess)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadiusPill))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun PhoneOtpForm(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var countryCode by remember { mutableStateOf("+91") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!otpSent) {
            Text(
                text = "Enter your phone number to receive a one-time verification code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraTextField(
                    value = countryCode,
                    onValueChange = { if (it.startsWith("+") || it.isEmpty()) countryCode = it },
                    label = "Code",
                    modifier = Modifier.width(84.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                AuraTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() } },
                    label = "Phone Number",
                    placeholder = "10 digit number",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        } else {
            Text(
                text = "Enter the 6-digit verification code sent to $countryCode $phone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AuraTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = "Verification Code",
                placeholder = "6-digit code",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        AuraButton(
            text = if (otpSent) "Verify & Continue" else "Send Verification Code",
            isLoading = authState is AuthState.Loading,
            onClick = {
                if (!otpSent) {
                    if (phone.length >= 10) {
                        val fullNumber = "${countryCode}${phone}"
                        authViewModel.sendOtp(
                            phoneNumber = fullNumber,
                            activity = context as? Activity,
                            onCodeSent = { otpSent = true },
                            onAutoSignIn = onLoginSuccess
                        )
                    } else {
                        authViewModel.sendOtp("", null, {}, {})
                    }
                } else {
                    authViewModel.verifyOtp(otp, onLoginSuccess)
                }
            }
        )

        if (otpSent) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { otpSent = false; otp = "" }) {
                    Text(
                        text = "Change Number",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(
                    onClick = {
                        val fullNumber = "${countryCode}${phone}"
                        authViewModel.sendOtp(fullNumber, context as? Activity, {}, onLoginSuccess)
                    }
                ) {
                    Text(
                        text = "Resend Code",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        AuthStatusMessage(authState)
    }
}

@Composable
private fun EmailPasswordForm(
    authViewModel: AuthViewModel,
    isSignUp: Boolean,
    onSignUpToggle: (Boolean) -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        AuraTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "name@example.com",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        AuraButton(
            text = if (isSignUp) "Create Account" else "Sign In",
            isLoading = authState is AuthState.Loading,
            onClick = {
                if (isSignUp) {
                    authViewModel.signUpWithEmail(email, password, onLoginSuccess)
                } else {
                    authViewModel.signInWithEmail(email, password, onLoginSuccess)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { onSignUpToggle(!isSignUp) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Create one now",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        AuthStatusMessage(authState)
    }
}

@Composable
private fun AuthStatusMessage(state: AuthState) {
    when (state) {
        is AuthState.Error -> {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(CornerRadiusSmall),
                color = ErrorRed.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.message,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        else -> {}
    }
}
