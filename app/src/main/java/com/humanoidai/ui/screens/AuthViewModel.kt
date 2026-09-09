package com.humanoidai.ui.screens

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

// -----------------------------------------------------------------
// AuthState
// -----------------------------------------------------------------
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String, val code: String? = null) : AuthState()
    object RequiresReAuth : AuthState()
}

// -----------------------------------------------------------------
// AuthViewModel
// -----------------------------------------------------------------
// Wraps Firebase Authentication for Aura 360°.
// Handles Phone OTP and Email/Password flows.
// -----------------------------------------------------------------
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var _justAuthenticated = false
    val justAuthenticated: Boolean get() = _justAuthenticated

    fun consumeAuthFlag(): Boolean {
        val flag = _justAuthenticated
        _justAuthenticated = false
        return flag
    }

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // ---------------- PHONE OTP ----------------

    fun sendOtp(
        phoneNumber: String,
        activity: Activity? = null,
        onCodeSent: () -> Unit,
        onAutoSignIn: () -> Unit = {}
    ) {
        if (phoneNumber.isBlank()) {
            _authState.value = AuthState.Error("Enter a valid phone number")
            return
        }

        _authState.value = AuthState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential, onAutoSignIn)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = AuthState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                _authState.value = AuthState.Idle
                onCodeSent()
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)

        if (activity != null) {
            optionsBuilder.setActivity(activity)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    fun verifyOtp(code: String, onSuccess: () -> Unit) {
        val verificationId = storedVerificationId
        if (verificationId == null) {
            _authState.value = AuthState.Error("Request an OTP first")
            return
        }
        if (code.isBlank()) {
            _authState.value = AuthState.Error("Enter the OTP")
            return
        }

        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential, onSuccess)
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                _justAuthenticated = true
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
    }

    // ---------------- GOOGLE / APPLE ----------------

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        signInWithCredential(credential, onSuccess, "Google login failed")
    }

    fun signInWithApple(activity: Activity, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        val provider = OAuthProvider.newBuilder("apple.com")
        
        auth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener {
                _justAuthenticated = true
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Apple login failed")
            }
    }

    private fun signInWithCredential(
        credential: AuthCredential,
        onSuccess: () -> Unit,
        errorMessage: String
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                _justAuthenticated = true
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(mapFirebaseError(e, errorMessage))
            }
    }

    // ---------------- EMAIL / PASSWORD ----------------

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Enter email and password")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _justAuthenticated = true
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(mapFirebaseError(e, "Login failed"))
            }
    }

    fun signUpWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Enter email and password")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _justAuthenticated = true
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(mapFirebaseError(e, "Sign up failed"))
            }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Enter your email address")
            return
        }
        _authState.value = AuthState.Loading
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _authState.value = AuthState.Idle
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
    }

    fun updateAccountPassword(newPassword: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Error("No authenticated user found")
            return
        }
        if (newPassword.length < 6) {
            _authState.value = AuthState.Error("New password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading
        user.updatePassword(newPassword)
            .addOnSuccessListener {
                _authState.value = AuthState.Idle
                onSuccess()
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    _authState.value = AuthState.RequiresReAuth
                } else {
                    _authState.value = AuthState.Error(mapFirebaseError(e, "Failed to update password"))
                }
            }
    }

    fun reauthenticate(password: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            _authState.value = AuthState.Error("Re-authentication failed: No user email")
            return
        }

        _authState.value = AuthState.Loading
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                _authState.value = AuthState.Idle
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(mapFirebaseError(e, "Re-authentication failed"))
            }
    }

    private fun mapFirebaseError(e: Exception, defaultMessage: String? = null): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "Account not found or disabled."
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials provided."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthRecentLoginRequiredException -> "Sensitive operation. Please sign in again."
            is FirebaseException -> e.localizedMessage ?: (defaultMessage ?: "Authentication error")
            else -> defaultMessage ?: "An unexpected error occurred"
        }
    }

    // ---------------- SESSION CHECK ----------------

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}
