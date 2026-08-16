package com.codewiz.wordloop.ui.addword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.ui.components.CircleIconButton
import com.codewiz.wordloop.ui.components.FilterChip
import com.codewiz.wordloop.ui.components.LoadingOverlay
import com.codewiz.wordloop.ui.components.WordListRow
import com.codewiz.wordloop.ui.theme.OrangeAccent
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWordContent(
    state: AddWordUiState,
    languages: List<String>,
    onWordChange: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onUseSuggestion: () -> Unit,
    onViewExisting: (LearnedWord) -> Unit,
    onRegenerate: (LearnedWord) -> Unit,
    onCancelExisting: () -> Unit,
) {
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(WlDesign.screenPadding)
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                CircleIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close"))
                }
            }
            when {
                state.existingSenses.isNotEmpty() -> ExistingSenses(
                    word = state.word,
                    senses = state.existingSenses,
                    onView = onViewExisting,
                    onRegenerate = onRegenerate,
                    onCancel = onCancelExisting,
                )
                else -> {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(12.dp),
                    )
                    Text(tr("Learn a new word"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "AI builds meanings, examples, and quizzes for your library.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(WlDesign.cardShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = state.word,
                            onValueChange = onWordChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus),
                            placeholder = { Text("Type any word") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        )
                        if (languages.size <= 1) {
                            val only = languages.firstOrNull() ?: state.selectedLanguage
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(only, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(tr("Language"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                languages.forEach { language ->
                                    FilterChip(language, language == state.selectedLanguage) { onLanguage(language) }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = state.word.isNotBlank() && !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, OrangeAccent),
                                    ),
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(tr("Create learning card"), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    state.unrecognizedWord?.let { unrecognized ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(WlDesign.cardShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("We couldn't find “${unrecognized.attemptedWord}”.")
                            unrecognized.suggestion?.let { suggestion ->
                                TextButton(onClick = onUseSuggestion) {
                                    Text("Did you mean $suggestion?")
                                }
                            }
                        }
                    }
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (state.isLoading) LoadingOverlay(tr("Generating your learning card..."))
    }
}

@Composable
private fun ExistingSenses(
    word: String,
    senses: List<LearnedWord>,
    onView: (LearnedWord) -> Unit,
    onRegenerate: (LearnedWord) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(tr("Already in your library"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(tr("You already have \"%@\" saved. Any other meanings it has are already covered on that card.", word))
        senses.forEach { sense ->
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    WordListRow(word = sense, showsReviewMeta = false, compact = true, onClick = { onView(sense) })
                }
                CircleIconButton(onClick = { onRegenerate(sense) }) {
                    Icon(Icons.Default.Refresh, contentDescription = tr("Regenerate"))
                }
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(tr("Cancel")) }
    }
}
