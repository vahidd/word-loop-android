package com.codewiz.wordloop.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codewiz.wordloop.BuildConfig
import com.codewiz.wordloop.data.auth.AuthException
import com.codewiz.wordloop.data.auth.AuthRepository
import com.codewiz.wordloop.data.push.PushManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val store: WordLoopStore,
    private val pushManager: PushManager,
) : ViewModel() {
    val user: StateFlow<FirebaseUser?> = authRepository.authState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        authRepository.currentUser,
    )

    private val _isChecking = MutableStateFlow(true)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isGuest: Boolean get() = authRepository.isGuest
    val displayEmail: String? get() = authRepository.displayEmail
    val userId: String? get() = authRepository.userId

    init {
        viewModelScope.launch {
            user.collect { _isChecking.value = false }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signIn(email: String, password: String) = runAuth { authRepository.signIn(email, password) }
    fun signUp(email: String, password: String) = runAuth { authRepository.signUp(email, password) }
    fun continueAsGuest() = runAuth { authRepository.continueAsGuest() }
    fun sendPasswordReset(email: String) = runAuth { authRepository.sendPasswordReset(email) }
    fun signInWithApple(activity: Activity) = runAuth { authRepository.signInWithApple(activity) }

    fun signInWithGoogle(activity: Activity) = runAuth {
        val credentialManager = CredentialManager.create(activity)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            authRepository.signInWithGoogle(google.idToken)
        } else {
            error("Google Sign-In did not return a valid token.")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { pushManager.signOut() }
            authRepository.signOut()
            store.clearLocalState()
        }
    }

    private fun runAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                block()
            } catch (cancelled: GetCredentialCancellationException) {
                // user dismissed
            } catch (error: AuthException) {
                if (error.message?.isNotBlank() == true) _error.value = error.message
            } catch (error: Exception) {
                val message = AuthRepository.friendlyMessage(error)
                if (message.isNotBlank()) _error.value = message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
