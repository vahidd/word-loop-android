package com.codewiz.wordloop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordStatus
import com.codewiz.wordloop.ui.components.EmptyStateCard
import com.codewiz.wordloop.ui.components.FilterChip
import com.codewiz.wordloop.ui.components.SectionHeader
import com.codewiz.wordloop.ui.components.WordListRow
import com.codewiz.wordloop.ui.suggestions.SuggestionsContent
import com.codewiz.wordloop.ui.suggestions.SuggestionsStyle
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.ui.today.BoxWithBackground
import com.codewiz.wordloop.util.AppConstants
import kotlinx.coroutines.launch

sealed interface LibraryFilter {
    data object None : LibraryFilter
    data object RecentlyFailed : LibraryFilter
    data class Status(val status: WordStatus) : LibraryFilter
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    store: WordLoopStore,
    onOpenWord: (LearnedWord, List<LearnedWord>) -> Unit,
) {
    val words by store.words.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<LibraryFilter>(LibraryFilter.None) }
    var languageFilter by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val languages = words.map { it.language }.distinct().sorted()
    val scope = rememberCoroutineScope()

    val currentFilter = filter
    val filtered = words
        .filter {
            when (val selected = currentFilter) {
                LibraryFilter.None -> true
                LibraryFilter.RecentlyFailed -> it.wordStatus == WordStatus.DIFFICULT
                is LibraryFilter.Status -> it.wordStatus == selected.status
            }
        }
        .filter { languageFilter == null || it.language == languageFilter }
        .filter {
            search.isBlank() || it.word.contains(search, true) || it.shortMeaning.contains(search, true)
        }
    val active = words.count { it.wordStatus != WordStatus.ARCHIVED }
    val showSuggestions = words.size < AppConstants.Suggestions.LIBRARY_WORD_THRESHOLD &&
        search.isBlank() && filter is LibraryFilter.None

    BoxWithBackground {
        Column(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WlDesign.screenPadding)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("Library"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = tr("All Statuses"))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("All Statuses")) }, onClick = { filter = LibraryFilter.None; menu = false })
                    DropdownMenuItem(text = { Text(tr("Recently Failed")) }, onClick = { filter = LibraryFilter.RecentlyFailed; menu = false })
                    WordStatus.entries.filter { it != WordStatus.NEW }.forEach { status ->
                        DropdownMenuItem(text = { Text(tr(status.displayName)) }, onClick = { filter = LibraryFilter.Status(status); menu = false })
                    }
                }
            }
            if (words.isEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(WlDesign.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
                ) {
                    item { SuggestionsContent(store, SuggestionsStyle.ONBOARDING) }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = WlDesign.screenPadding, end = WlDesign.screenPadding, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(tr("Search words")) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                        )
                    }
                    item { Text(tr("%lld active word(s) in your library", active), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)) }
                    if (languages.size > 1) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(tr("All Languages"), languageFilter == null) { languageFilter = null } }
                                items(languages) { language ->
                                    FilterChip("$language · ${words.count { it.language == language }}", languageFilter == language) {
                                        languageFilter = language
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = tr("No matches"),
                                message = if (search.isBlank()) tr("Try a different filter to see more words.")
                                else tr("No words match \"%@\".", search),
                                icon = Icons.Default.Search,
                                accent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                    } else {
                        item { SectionHeader(tr("Words"), tr("%lld shown", filtered.size)) }
                        items(filtered, key = { it.id }) { word ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        scope.launch { store.deleteWord(word) }
                                        true
                                    } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        scope.launch {
                                            if (word.wordStatus == WordStatus.ARCHIVED) store.unarchiveWord(word)
                                            else store.archiveWord(word)
                                        }
                                        true
                                    } else false
                                },
                            )
                            SwipeToDismissBox(state = dismissState, backgroundContent = {}) {
                                WordListRow(
                                    word = word,
                                    showsLanguage = languages.size > 1,
                                    onClick = { onOpenWord(word, filtered) },
                                )
                            }
                        }
                    }
                    if (showSuggestions) {
                        item { SuggestionsContent(store, SuggestionsStyle.SECTION) }
                    }
                }
            }
        }
    }
}
