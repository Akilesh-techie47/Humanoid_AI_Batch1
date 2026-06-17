package com.humanoidai.ui.screens

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
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
    data class Error(val message: String) : AuthState()
}

// -----------------------------------------------------------------
// AuthViewModel
// -----------------------------------------------------------------
// Wraps Firebase Authentication for:
//  - Phone OTP (sendOtp / verifyOtp)
//  - Email/Password (signInWithEmail / signUpWithEmail)
//
// NOTE: For Phone Auth you must pass an Activity to PhoneAuthOptions.
// LoginScreen should be hosted inside MainActivity so LocalContext
// resolves to an Activity (see usage notes below).
// -----------------------------------------------------------------
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // ---------------- PHONE OTP ----------------

    /**
     * Sends OTP to the given phone number.
     * `activity` is required by Firebase PhoneAuthOptions for reCAPTCHA fallback.
     * `onCodeSent` is called once the OTP has been dispatched.
     */
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
                // Auto-retrieval: sign in directly without manual OTP entry
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

    /**
     * Verifies the OTP entered by the user against the stored verificationId.
     */
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
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Invalid OTP")
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
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Login failed")
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
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
    }

    // ---------------- SESSION CHECK ----------------

    /** Returns true if a user is already signed in (e.g. on app relaunch). */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}
