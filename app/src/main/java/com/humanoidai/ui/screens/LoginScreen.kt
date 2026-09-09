package com.humanoidai.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// LoginScreen
// -----------------------------------------------------------------

enum class LoginTab { PHONE, EMAIL }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableStateOf(LoginTab.PHONE) }
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
                    authViewModel.signOut() // Clear loading
                }
            } catch (e: ApiException) {
                val msg = when(e.statusCode) {
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
    
    if (authState is AuthState.Loading) {
        FishLoadingAnimation()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 60.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo & Branding
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_humanoid_logo),
                        contentDescription = "Aura 360 Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "AURA 360",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Fisheye contextual proactive assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))

                // Social Auth Buttons
                SocialAuthSection(
                    onGoogleClick = { signInWithGoogle() },
                    onAppleClick = { authViewModel.signInWithApple(context as Activity, onLoginSuccess) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text(
                        " OR ", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        style = MaterialTheme.typography.labelSmall, 
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tab Switcher
                AuraPanel {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
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

                    Spacer(modifier = Modifier.height(20.dp))

                    when (selectedTab) {
                        LoginTab.PHONE -> PhoneOtpForm(authViewModel, onLoginSuccess)
                        LoginTab.EMAIL -> EmailPasswordForm(authViewModel, onLoginSuccess)
                    }
                }
            }
            
            ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun SocialAuthSection(onGoogleClick: () -> Unit, onAppleClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuraOutlinedButton(text = "Continue with Google", onClick = onGoogleClick)
        AuraOutlinedButton(text = "Continue with Apple", onClick = onAppleClick)
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
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
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
                "Verify your mobile number to continue",
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
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                AuraTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() } },
                    label = "Mobile Number",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        } else {
            Text(
                "Enter the 6-digit code sent to $countryCode $phone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AuraTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = "Verification Code",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuraButton(
            text = if (otpSent) "Verify & Login" else "Send OTP",
            isLoading = authState is AuthState.Loading
        ) {
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
                    // Internal error handling would show state error if we wanted, 
                    // but let's just use the VM to handle it if they press it.
                    authViewModel.sendOtp("", null, {}, {}) 
                }
            } else {
                authViewModel.verifyOtp(otp, onLoginSuccess)
            }
        }

        if (otpSent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { otpSent = false; otp = "" }) {
                    Text("Change Number", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                
                TextButton(onClick = { 
                    val fullNumber = "${countryCode}${phone}"
                    authViewModel.sendOtp(fullNumber, context as? Activity, { /* Resent */ }, onLoginSuccess)
                }) {
                    Text("Resend Code", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        AuthStatusText(authState)
    }
}

@Composable
private fun EmailPasswordForm(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        AuraTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuraButton(
            text = if (isSignUp) "Create Account" else "Sign In",
            isLoading = authState is AuthState.Loading
        ) {
            if (isSignUp) {
                authViewModel.signUpWithEmail(email, password, onLoginSuccess)
            } else {
                authViewModel.signInWithEmail(email, password, onLoginSuccess)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { isSignUp = !isSignUp }
        )

        AuthStatusText(authState)
    }
}

@Composable
private fun AuthStatusText(state: AuthState) {
    when (state) {
        is AuthState.Error -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.message,
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        else -> {}
    }
}
