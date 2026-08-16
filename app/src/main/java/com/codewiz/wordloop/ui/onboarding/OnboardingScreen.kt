package com.codewiz.wordloop.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.data.push.PushManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LanguageProficiency
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.util.AppConstants
import kotlinx.coroutines.launch

private enum class Step { FEATURES, LANGUAGES, PROFICIENCY, NATIVE, NOTIFICATIONS }

@Composable
fun OnboardingScreen(
    store: WordLoopStore,
    pushManager: PushManager,
    onRequestNotificationPermission: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val profile = store.userProfile.value
    var step by remember { mutableStateOf(Step.FEATURES) }
    var languages by remember {
        mutableStateOf(profile?.learningLanguages?.ifEmpty { listOf("English") } ?: listOf("English"))
    }
    var proficiency by remember { mutableStateOf(profile?.proficiencyByLanguage ?: emptyMap()) }
    var native by remember { mutableStateOf(profile?.nativeLanguage.orEmpty()) }
    var reminders by remember { mutableStateOf(false) }
    var marketing by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var proficiencyIndex by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .align(Alignment.CenterHorizontally),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step != Step.FEATURES || picker != null) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Back"),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (picker != null) picker = null
                            else step = when (step) {
                                Step.LANGUAGES -> Step.FEATURES
                                Step.PROFICIENCY -> if (proficiencyIndex > 0) {
                                    proficiencyIndex--; step
                                } else Step.LANGUAGES
                                Step.NATIVE -> Step.PROFICIENCY.also { proficiencyIndex = languages.lastIndex.coerceAtLeast(0) }
                                Step.NOTIFICATIONS -> Step.NATIVE
                                Step.FEATURES -> step
                            }
                        }
                        .padding(6.dp),
                )
            }
            LinearProgressIndicator(
                progress = { (step.ordinal + 1) / Step.entries.size.toFloat() },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
            )
        }

        if (picker != null) {
            LanguagePicker(
                title = if (picker == "learning") "Add a language" else "Your native language",
                search = search,
                onSearch = { search = it },
                selected = if (picker == "learning") languages else listOfNotNull(native.ifBlank { null }),
                onSelect = { language ->
                    if (picker == "learning") {
                        if (language !in languages) languages = languages + language
                    } else native = language
                    picker = null
                    search = ""
                },
            )
        } else {
            when (step) {
                Step.FEATURES -> {
                    Text("Your vocabulary, built from the words that matter to you.", style = MaterialTheme.typography.titleMedium)
                }
                Step.LANGUAGES -> {
                    Text(tr("What are you learning?"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Choose one or a few. You can always change this later.")
                    languages.forEach { language ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(language)
                            if (languages.size > 1) {
                                TextButton(onClick = { languages = languages - language }) { Text(tr("Delete")) }
                            }
                        }
                    }
                    TextButton(onClick = { picker = "learning" }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(tr("Add another language"))
                    }
                }
                Step.PROFICIENCY -> {
                    val language = languages.getOrElse(proficiencyIndex) { languages.first() }
                    Text(tr("How is your %@?", language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LanguageProficiency.entries.forEach { level ->
                        val selected = (proficiency[language] ?: "beginner") == level.raw
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { proficiency = proficiency + (language to level.raw) }
                                .padding(14.dp),
                        ) {
                            Text(tr(level.displayName), fontWeight = FontWeight.SemiBold)
                            Text(level.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Step.NATIVE -> {
                    Text(tr("What feels natural?"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Word Loop will use your native language for helpful hints and explanations.")
                    if (native.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(native, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(onClick = { picker = "native" }) {
                        Text(if (native.isBlank()) "Choose my native language" else "Change language")
                    }
                }
                Step.NOTIFICATIONS -> {
                    Text("Stay in the loop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Review Reminders"))
                        Switch(checked = reminders, onCheckedChange = { reminders = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Product Updates"))
                        Switch(checked = marketing, onCheckedChange = { marketing = it })
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    when (step) {
                        Step.FEATURES -> step = Step.LANGUAGES
                        Step.LANGUAGES -> {
                            proficiencyIndex = 0
                            step = Step.PROFICIENCY
                        }
                        Step.PROFICIENCY -> {
                            if (proficiencyIndex >= languages.lastIndex) step = Step.NATIVE
                            else proficiencyIndex++
                        }
                        Step.NATIVE -> if (native.isNotBlank()) step = Step.NOTIFICATIONS
                        Step.NOTIFICATIONS -> {
                            saving = true
                            scope.launch {
                                runCatching {
                                    store.saveOnboardingProfile(languages, proficiency, native, true)
                                    if (reminders || marketing) {
                                        onRequestNotificationPermission { granted ->
                                            scope.launch {
                                                if (granted) {
                                                    pushManager.registerPreferences(reminders, marketing, false)
                                                }
                                            }
                                        }
                                    }
                                }.onFailure { error = it.message }
                                saving = false
                            }
                        }
                    }
                },
                enabled = !saving && when (step) {
                    Step.LANGUAGES, Step.PROFICIENCY -> languages.isNotEmpty()
                    Step.NATIVE -> native.isNotBlank()
                    else -> true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val title = when (step) {
                    Step.FEATURES -> "Personalize Word Loop"
                    Step.LANGUAGES -> "Set my levels"
                    Step.PROFICIENCY -> if (proficiencyIndex >= languages.lastIndex) "Continue" else "Next language"
                    Step.NATIVE -> "Continue"
                    Step.NOTIFICATIONS -> if (saving) "Starting..." else "Start learning"
                }
                Text(title)
                Icon(if (step == Step.NOTIFICATIONS) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun LanguagePicker(
    title: String,
    search: String,
    onSearch: (String) -> Unit,
    selected: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search all languages") }, singleLine = true)
        val options = AppConstants.supportedLanguages.filter { it.contains(search, true) }
        LazyColumn(Modifier.height(360.dp)) {
            items(options) { language ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(language) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(language)
                    if (language in selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
