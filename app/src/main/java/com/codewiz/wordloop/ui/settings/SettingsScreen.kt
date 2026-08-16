package com.codewiz.wordloop.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.BuildConfig
import com.codewiz.wordloop.data.auth.AuthRepository
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.codewiz.wordloop.data.push.PushManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.ReviewMode
import com.codewiz.wordloop.ui.components.GradientHero
import com.codewiz.wordloop.ui.components.SettingsIconBadge
import com.codewiz.wordloop.ui.components.SettingsSection
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.ui.today.BoxWithBackground
import com.codewiz.wordloop.util.AppConstants
import com.codewiz.wordloop.util.AppUiLanguage
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    store: WordLoopStore,
    prefs: UserPrefs,
    pushManager: PushManager,
    auth: AuthRepository,
    isGuest: Boolean,
    displayEmail: String?,
    userId: String?,
    onSignOut: () -> Unit,
    onUpgrade: () -> Unit,
    onChangeLanguage: (AppUiLanguage) -> Unit,
    onOpenLearningLanguages: () -> Unit,
    onOpenNativeLanguage: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val profile by store.userProfile.collectAsState()
    val sound by prefs.soundEffectsEnabled.collectAsState(initial = true)
    val reminders by prefs.reviewRemindersEnabled.collectAsState(initial = false)
    val marketing by prefs.marketingEnabled.collectAsState(initial = false)
    val wotd by prefs.wordOfTheDayEnabled.collectAsState(initial = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmGuestOut by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var langMenu by remember { mutableStateOf(false) }
    var modeMenu by remember { mutableStateOf(false) }
    var backendMenu by remember { mutableStateOf(false) }
    var backendLabel by remember { mutableStateOf("Production") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val data = store.exportWords()
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                message = "Backup exported."
            }.onFailure { message = "Could not export backup: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("empty")
                val result = store.importWords(bytes)
                message = "Backup imported. ${result.insertedWords} added, ${result.skippedWords} skipped."
            }.onFailure { message = "Could not import backup: ${it.message}" }
        }
    }

    BoxWithBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = WlDesign.screenPadding, end = WlDesign.screenPadding, top = WlDesign.screenPadding, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
        ) {
            item { Text(tr("Settings"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
            item {
                if (isGuest) {
                    GradientHero {
                        Text(tr("Guest"), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Create an account to save your words and sync across devices.", color = Color.White.copy(alpha = 0.85f))
                        androidx.compose.material3.Button(
                            onClick = onUpgrade,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(tr("Create Free Account"), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    GradientHero {
                        Text(displayEmail ?: "Signed in", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(tr("Synced across devices"), color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
            item {
                SettingsSection(tr("Language"), tr("Choose the language for menus, buttons, and other app text.")) {
                    Box {
                        SettingsRow(Icons.Default.Language, Color(0xFF007AFF), tr("App Language"), trailing = {
                            Text((AppUiLanguage.from(profile?.appLanguage) ?: AppUiLanguage.ENGLISH).pickerLabel, color = MaterialTheme.colorScheme.primary)
                        }, onClick = { langMenu = true })
                        DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                            AppUiLanguage.entries.forEach { language ->
                                DropdownMenuItem(text = { Text(language.pickerLabel) }, onClick = {
                                    langMenu = false
                                    onChangeLanguage(language)
                                })
                            }
                        }
                    }
                }
            }
            item {
                SettingsSection(
                    tr("Notifications"),
                    tr("Review reminders are sent when words are due. Word of the Day sends one new word each day once you have a few words. Product updates require a separate opt-in."),
                ) {
                    ToggleRow(Icons.Default.Notifications, Color(0xFFFF3B30), tr("Review Reminders"), reminders) { enabled ->
                        if (enabled) {
                            onRequestNotificationPermission { granted ->
                                scope.launch {
                                    if (granted) pushManager.registerPreferences(true, marketing, wotd)
                                    else prefs.setReviewRemindersEnabled(false)
                                }
                            }
                        } else scope.launch { pushManager.registerPreferences(false, marketing, wotd) }
                    }
                    ToggleRow(Icons.Default.AutoAwesome, Color(0xFFAF52DE), tr("Word of the Day"), wotd) { enabled ->
                        if (enabled) {
                            onRequestNotificationPermission { granted ->
                                scope.launch {
                                    if (granted) pushManager.registerPreferences(reminders, marketing, true)
                                    else prefs.setWordOfTheDayEnabled(false)
                                }
                            }
                        } else scope.launch { pushManager.registerPreferences(reminders, marketing, false) }
                    }
                    ToggleRow(Icons.Default.Campaign, Color(0xFFFF9500), tr("Product Updates"), marketing) { enabled ->
                        if (enabled) {
                            onRequestNotificationPermission { granted ->
                                scope.launch {
                                    if (granted) pushManager.registerPreferences(reminders, true, wotd)
                                    else prefs.setMarketingEnabled(false)
                                }
                            }
                        } else scope.launch { pushManager.registerPreferences(reminders, false, wotd) }
                    }
                }
            }
            item {
                SettingsSection(tr("Audio"), tr("Play a sound after each quiz answer.")) {
                    ToggleRow(Icons.Default.VolumeUp, Color(0xFFA620D9), tr("Sound Effects"), sound) {
                        scope.launch { prefs.setSoundEffectsEnabled(it) }
                    }
                }
            }
            item {
                SettingsSection(
                    tr("Learning"),
                    tr("Standard allows review intervals to stretch to months; Intensive caps them at 14 days for more frequent practice. Changes take effect as each word comes up for review."),
                ) {
                    Box {
                        SettingsRow(Icons.Default.Psychology, Color(0xFF338CD6), tr("Review Mode"), trailing = {
                            Text((profile?.reviewModeEnum ?: ReviewMode.STANDARD).name.lowercase().replaceFirstChar(Char::titlecase), color = MaterialTheme.colorScheme.primary)
                        }, onClick = { modeMenu = true })
                        DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                            ReviewMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(tr(mode.name.lowercase().replaceFirstChar(Char::titlecase))) }, onClick = {
                                    modeMenu = false
                                    scope.launch {
                                        store.updateUserProfile(
                                            com.codewiz.wordloop.data.api.UpdateUserProfileBody(reviewMode = mode.raw),
                                        )
                                    }
                                })
                            }
                        }
                    }
                }
            }
            item {
                SettingsSection(tr("Defaults"), tr("Choose the languages you're learning. These appear when you add words.")) {
                    SettingsRow(Icons.Default.Language, Color(0xFF30B0C7), tr("Word Languages"), trailing = {
                        Text(profile?.learningLanguages?.joinToString(" · ").orEmpty(), color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }, onClick = onOpenLearningLanguages)
                    SettingsRow(Icons.Default.Person, Color(0xFF5856D6), tr("Your Native Language"), trailing = {
                        Text(profile?.nativeLanguage ?: "Not set", color = MaterialTheme.colorScheme.primary)
                    }, onClick = onOpenNativeLanguage)
                }
            }
            item {
                SettingsSection(tr("Data")) {
                    SettingsRow(Icons.Default.Upload, Color(0xFF007AFF), tr("Export Backup")) {
                        exportLauncher.launch("WordLoop-Words-${LocalDate.now()}.json")
                    }
                    SettingsRow(Icons.Default.Download, Color(0xFF34C759), tr("Import Backup")) {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                    SettingsRow(Icons.Default.Delete, Color(0xFFFF3B30), tr("Reset All Data"), destructive = true) {
                        confirmReset = true
                    }
                }
            }
            item {
                SettingsSection(tr("About")) {
                    SettingsRow(Icons.Default.Info, Color.Gray, tr("Version"), trailing = {
                        Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    })
                    SettingsRow(Icons.Default.Star, Color(0xFFFFCC00), tr("Rate Word Loop")) {
                        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}"))
                        val web = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"),
                        )
                        runCatching { context.startActivity(market) }
                            .recoverCatching { context.startActivity(web) }
                            .onFailure { message = it.message }
                    }
                }
            }
            item {
                SettingsSection(tr("Account")) {
                    SettingsRow(Icons.AutoMirrored.Filled.Logout, Color(0xFFFF9500), tr("Sign Out"), destructive = true) {
                        if (isGuest) confirmGuestOut = true else onSignOut()
                    }
                    if (!isGuest) {
                        SettingsRow(Icons.Default.Delete, Color(0xFFFF3B30), tr("Delete Account"), destructive = true) {
                            confirmDelete = true
                        }
                    }
                }
            }
            if (AppConstants.isTestUser(userId)) {
                item {
                    SettingsSection("Developer", "Internal testing tools. Visible to test users and debug builds.") {
                        SettingsRow(Icons.Default.Storage, Color.Gray, "Backend", trailing = { Text(backendLabel) }, onClick = { backendMenu = true })
                        DropdownMenu(expanded = backendMenu, onDismissRequest = { backendMenu = false }) {
                            DropdownMenuItem(text = { Text("Production") }, onClick = {
                                backendMenu = false
                                backendLabel = "Production"
                                scope.launch {
                                    prefs.setBaseUrl(AppConstants.PRODUCTION_BASE_URL)
                                    store.refreshAll()
                                }
                            })
                            DropdownMenuItem(text = { Text("Dev") }, onClick = {
                                backendMenu = false
                                backendLabel = "Dev"
                                scope.launch {
                                    prefs.setBaseUrl(AppConstants.DEV_BASE_URL)
                                    store.refreshAll()
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        Confirm("Reset All Data", tr("This will permanently delete all saved words and progress. This cannot be undone."), tr("Reset")) {
            confirmReset = false
            scope.launch { runCatching { store.resetAllData() } }
        }
    }
    if (confirmDelete) {
        Confirm(tr("Delete Account"), tr("This permanently deletes your account and all data. This cannot be undone."), tr("Delete Account")) {
            confirmDelete = false
            scope.launch {
                runCatching { store.deleteAccount() }
                    .onFailure { message = "Failed to delete account: ${it.message}" }
                    .onSuccess { onSignOut() }
            }
        }
    }
    if (confirmGuestOut) {
        Confirm(tr("Sign Out"), tr("You're using a guest account. Signing out will erase your words and progress. Create an account first to keep them."), tr("Sign Out")) {
            confirmGuestOut = false
            onSignOut()
        }
    }
    message?.let {
        AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text(tr("OK")) } }, text = { Text(it) })
    }
}

@Composable
private fun Confirm(title: String, body: String, confirm: String, onConfirm: () -> Unit) {
    var open by remember { mutableStateOf(true) }
    if (!open) return
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { open = false; onConfirm() }) { Text(confirm, color = Color.Red) } },
        dismissButton = { TextButton(onClick = { open = false }) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconBadge(icon, color)
        Text(title, color = if (destructive) Color.Red else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconBadge(icon, color)
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
