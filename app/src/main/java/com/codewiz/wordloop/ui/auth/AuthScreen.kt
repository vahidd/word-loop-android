package com.codewiz.wordloop.ui.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.R
import com.codewiz.wordloop.ui.components.CircleIconButton
import com.codewiz.wordloop.ui.components.GradientHero
import com.codewiz.wordloop.ui.components.LoadingOverlay
import com.codewiz.wordloop.ui.components.ScreenBackground
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.util.AppConstants

@Composable
fun AuthScreen(
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onGoogle: (Activity) -> Unit,
    onApple: (Activity) -> Unit,
    onGuest: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailSignUp: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    compact: Boolean = false,
) {
    val activity = LocalContext.current as Activity
    var showEmail by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(WlDesign.screenPadding)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (!compact) AuthHero()
            AuthSignInPanel(
                error = error,
                onEmail = { showEmail = true },
                onGoogle = { onGoogle(activity) },
                onApple = { onApple(activity) },
                onGuest = if (compact) null else onGuest,
            )
        }
        if (isLoading) LoadingOverlay(tr("Signing in..."))
    }
    if (showEmail) {
        EmailSheet(
            error = error,
            onDismiss = {
                showEmail = false
                onClearError()
            },
            onClearError = onClearError,
            onSignIn = onEmailSignIn,
            onSignUp = onEmailSignUp,
            onForgot = onForgotPassword,
        )
    }
}

@Composable
private fun AuthHero() {
    GradientHero {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painterResource(R.drawable.app_logo),
                contentDescription = AppConstants.APP_NAME,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Text(AppConstants.APP_NAME, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Words worth remembering.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AuthSignInPanel(
    error: String?,
    onEmail: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onGuest: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProviderButton("Continue with Email", container = MaterialTheme.colorScheme.primary, content = Color.White, onClick = onEmail)
        ProviderButton(
            title = "Continue with Google",
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface,
            leading = {
                Image(painterResource(R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(18.dp))
            },
            onClick = onGoogle,
        )
        ProviderButton("Continue with Apple", container = Color.Black, content = Color.White, onClick = onApple)
        if (onGuest != null) {
            TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
                Text("Continue as Guest")
            }
        }
        if (!error.isNullOrBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProviderButton(
    title: String,
    container: Color,
    content: Color,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(8.dp))
        }
        Text(title, fontWeight = FontWeight.SemiBold)
    }
}

private enum class EmailMode { SIGN_IN, SIGN_UP, FORGOT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailSheet(
    error: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onForgot: (String) -> Unit,
) {
    var mode by remember { mutableStateOf(EmailMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var resetSent by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CircleIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close"))
                }
            }
            if (mode == EmailMode.FORGOT && resetSent) {
                Text("Check your email", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("If an account exists for ${email.trim()}, you'll receive password reset instructions shortly.")
                Button(onClick = { mode = EmailMode.SIGN_IN; resetSent = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to Sign In")
                }
            } else {
                val title = when (mode) {
                    EmailMode.SIGN_IN -> "Welcome back"
                    EmailMode.SIGN_UP -> "Create account"
                    EmailMode.FORGOT -> "Reset password"
                }
                val subtitle = when (mode) {
                    EmailMode.SIGN_IN -> "Sign in with your email and password."
                    EmailMode.SIGN_UP -> "Start building your vocabulary."
                    EmailMode.FORGOT -> "Enter your email and we'll send you a reset link."
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true,
                )
                if (mode != EmailMode.FORGOT) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                    )
                }
                if (mode == EmailMode.SIGN_UP) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                }
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                val action = when (mode) {
                    EmailMode.SIGN_IN -> "Sign In"
                    EmailMode.SIGN_UP -> "Create Account"
                    EmailMode.FORGOT -> "Send Reset Link"
                }
                Button(
                    onClick = {
                        val trimmed = email.trim()
                        when (mode) {
                            EmailMode.FORGOT -> {
                                onForgot(trimmed)
                                resetSent = true
                            }
                            EmailMode.SIGN_IN -> onSignIn(trimmed, password)
                            EmailMode.SIGN_UP -> onSignUp(trimmed, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && (mode == EmailMode.FORGOT || password.length >= 6) &&
                        (mode != EmailMode.SIGN_UP || password == confirm),
                ) { Text(action) }
                if (mode == EmailMode.SIGN_IN) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { mode = EmailMode.SIGN_UP; onClearError() }) { Text("Create account") }
                        TextButton(onClick = { mode = EmailMode.FORGOT; onClearError() }) { Text("Forgot password?") }
                    }
                }
            }
        }
    }
}
