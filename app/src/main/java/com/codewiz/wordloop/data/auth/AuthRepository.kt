package com.codewiz.wordloop.data.auth

import android.app.Activity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) {
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isGuest: Boolean get() = auth.currentUser?.isAnonymous == true
    val userId: String? get() = auth.currentUser?.uid
    val displayEmail: String? get() = auth.currentUser?.email ?: auth.currentUser?.displayName

    suspend fun currentIdToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return user.getIdToken(forceRefresh).await()?.token
    }

    suspend fun signIn(email: String, password: String) {
        wrap { auth.signInWithEmailAndPassword(email, password).await() }
    }

    suspend fun signUp(email: String, password: String) {
        wrap {
            val current = auth.currentUser
            if (current != null && current.isAnonymous) {
                val credential = EmailAuthProvider.getCredential(email, password)
                current.linkWithCredential(credential).await()
            } else {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
        }
    }

    suspend fun continueAsGuest() {
        wrap { auth.signInAnonymously().await() }
    }

    suspend fun sendPasswordReset(email: String) {
        wrap { auth.sendPasswordResetEmail(email).await() }
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        wrap { signInOrLink(credential) }
    }

    suspend fun signInWithApple(activity: Activity) {
        val provider = OAuthProvider.newBuilder("apple.com")
            .addCustomParameter("locale", "en")
        wrap {
            val pending = auth.pendingAuthResult
            if (pending != null) {
                pending.await()
            } else {
                val current = auth.currentUser
                if (current != null && current.isAnonymous) {
                    try {
                        current.startActivityForLinkWithProvider(activity, provider.build()).await()
                    } catch (error: Exception) {
                        auth.startActivityForSignInWithProvider(activity, provider.build()).await()
                    }
                } else {
                    auth.startActivityForSignInWithProvider(activity, provider.build()).await()
                }
            }
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private suspend fun signInOrLink(credential: AuthCredential) {
        val current = auth.currentUser
        if (current == null || !current.isAnonymous) {
            auth.signInWithCredential(credential).await()
            return
        }
        try {
            current.linkWithCredential(credential).await()
        } catch (error: Exception) {
            try {
                auth.signInWithCredential(credential).await()
            } catch (_: Exception) {
                throw error
            }
        }
    }

    private inline fun <T> wrap(block: () -> T): T {
        try {
            return block()
        } catch (error: Exception) {
            throw AuthException(friendlyMessage(error), error)
        }
    }

    companion object {
        fun friendlyMessage(error: Throwable): String {
            val code = (error as? FirebaseAuthException)?.errorCode
            return when (code) {
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
                "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
                "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait and try again."
                "ERROR_WEB_CONTEXT_CANCELED", "ERROR_CANCELED" -> ""
                else -> {
                    val message = error.message.orEmpty()
                    if (message.contains("canceled", ignoreCase = true) ||
                        message.contains("cancelled", ignoreCase = true)
                    ) {
                        ""
                    } else {
                        "Something went wrong. Please try again."
                    }
                }
            }
        }
    }
}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
