package com.codewiz.wordloop.ui.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordSuggestion
import com.codewiz.wordloop.ui.components.FilterChip
import com.codewiz.wordloop.ui.components.SectionHeader
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import kotlinx.coroutines.launch

enum class SuggestionsStyle { ONBOARDING, SECTION }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionsContent(
    store: WordLoopStore,
    style: SuggestionsStyle,
    onAdded: ((LearnedWord) -> Unit)? = null,
) {
    val profile by store.userProfile.collectAsState()
    val languages = profile?.learningLanguages.orEmpty()
    var selected by remember { mutableStateOf(languages.firstOrNull().orEmpty()) }
    val suggestions by store.suggestions.collectAsState()
    val loading by store.isLoadingSuggestions.collectAsState()
    val words by store.words.collectAsState()
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(setOf<String>()) }
    var added by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(languages) {
        if (selected.isEmpty() || selected !in languages) selected = languages.firstOrNull().orEmpty()
        if (suggestions.isEmpty()) store.loadSuggestions()
    }

    fun key(word: String, language: String) = "$word|$language"
    fun owned(suggestion: WordSuggestion, language: String) =
        words.any { it.word == suggestion.word && it.language == language }

    val current = suggestions[selected].orEmpty()
    val visible = current.filter { !owned(it, selected) || key(it.word, selected) in added }
    val hasAnything = languages.any { language ->
        suggestions[language].orEmpty().any { !owned(it, language) || key(it.word, language) in added }
    }

    if (style == SuggestionsStyle.SECTION && (loading || !hasAnything)) return

    Column(verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing)) {
        if (style == SuggestionsStyle.ONBOARDING) {
            Text(tr("Start Your Vocabulary Journey"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                tr("Here are some words to get you started. Tap + to add any to your library."),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            Column {
                SectionHeader(tr("Suggested for you"))
                Text(
                    tr("Picked for your level. Tap + to add them to your library."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        if (languages.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { language ->
                    FilterChip(language, language == selected) { selected = language }
                }
            }
        }
        when {
            loading && style == SuggestionsStyle.ONBOARDING -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            visible.isEmpty() -> Text(
                if (style == SuggestionsStyle.ONBOARDING) "No suggestions right now."
                else tr("You've added all suggestions for this language."),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            else -> SuggestionGrid(
                items = visible,
                language = selected,
                adding = adding,
                added = added,
                onAdd = { suggestion ->
                    val chip = key(suggestion.word, selected)
                    adding = adding + chip
                    scope.launch {
                        runCatching { store.createWord(suggestion.word, selected) }
                            .onSuccess { created ->
                                added = added + chip
                                onAdded?.invoke(created)
                            }
                        adding = adding - chip
                    }
                },
            )
        }
    }
}

@Composable
private fun SuggestionGrid(
    items: List<WordSuggestion>,
    language: String,
    adding: Set<String>,
    added: Set<String>,
    onAdd: (WordSuggestion) -> Unit,
) {
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { suggestion ->
                    val chip = "${suggestion.word}|$language"
                    SuggestionCard(
                        suggestion = suggestion,
                        busy = chip in adding,
                        done = chip in added,
                        onAdd = { onAdd(suggestion) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: WordSuggestion,
    busy: Boolean,
    done: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(suggestion.word, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(enabled = !busy && !done, onClick = onAdd)
                    .padding(6.dp),
            ) {
                when {
                    busy -> CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                    done -> Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    else -> Icon(Icons.Default.Add, contentDescription = tr("Add"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(suggestion.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
