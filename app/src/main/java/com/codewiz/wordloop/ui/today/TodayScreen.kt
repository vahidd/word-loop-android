package com.codewiz.wordloop.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.data.audio.PronunciationPlayer
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordStatus
import com.codewiz.wordloop.ui.components.EmptyStateCard
import com.codewiz.wordloop.ui.components.GradientHero
import com.codewiz.wordloop.ui.components.Metric
import com.codewiz.wordloop.ui.components.MetricBar
import com.codewiz.wordloop.ui.components.ScreenBackground
import com.codewiz.wordloop.ui.components.SectionHeader
import com.codewiz.wordloop.ui.components.WordListRow
import com.codewiz.wordloop.ui.suggestions.SuggestionsContent
import com.codewiz.wordloop.ui.suggestions.SuggestionsStyle
import com.codewiz.wordloop.ui.theme.OrangeAccent
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.util.AppConstants
import com.codewiz.wordloop.util.formattedLongDate
import com.codewiz.wordloop.util.greetingForHour
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    store: WordLoopStore,
    player: PronunciationPlayer,
    onOpenWord: (LearnedWord) -> Unit,
    onStartReview: () -> Unit,
) {
    val words by store.words.collectAsState()
    val due by store.dueWords.collectAsState()
    val progress by store.progress.collectAsState()
    val loading by store.isLoading.collectAsState()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val mastered = words.count { it.wordStatus == WordStatus.MASTERED }

    LaunchedEffect(Unit) {
        store.loadSuggestions()
        while (true) {
            delay(60_000)
            store.refreshAll()
        }
    }

    BoxWithBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = WlDesign.screenPadding,
                end = WlDesign.screenPadding,
                top = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
        ) {
            item {
                Column {
                    Text(tr(greetingForHour(hour)), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(formattedLongDate(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            item {
                when {
                    due.isNotEmpty() -> ReviewHero(dueCount = due.size, streak = progress?.currentStreak ?: 0, onStart = onStartReview)
                    words.isNotEmpty() -> EmptyStateCard(
                        title = tr("All caught up!"),
                        message = if ((progress?.currentStreak ?: 0) > 0) {
                            tr("Your %lld-day streak is safe. Come back when new reviews are due.", progress?.currentStreak ?: 0)
                        } else {
                            tr("No reviews due right now. Add a word or check back later.")
                        },
                        icon = Icons.Default.CheckCircle,
                        accent = Color(0xFF34C759),
                    )
                    loading && words.isEmpty() -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    else -> GradientHero {
                        Text(tr("Your library is empty"), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                        Text(tr("Ready to start learning?"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            tr("We picked words for your level — tap + below to add them, or use the + button to add your own."),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            if (words.size < AppConstants.Suggestions.TODAY_WORD_THRESHOLD) {
                item { SuggestionsContent(store, SuggestionsStyle.SECTION) }
            }
            item { WordOfTheDaySection(store, player) }
            if (words.isNotEmpty()) {
                item {
                    MetricBar(
                        listOf(
                            Metric("${due.size}", tr("Due"), Icons.Default.Schedule, if (due.isEmpty()) Color(0xFF34C759) else OrangeAccent),
                            Metric("${progress?.currentStreak ?: 0}", tr("Streak"), Icons.Default.LocalFireDepartment, OrangeAccent),
                            Metric("$mastered", tr("Mastered"), Icons.Default.CheckCircle, Color(0xFF34C759)),
                        ),
                    )
                }
            }
            if (due.isNotEmpty()) {
                item { SectionHeader(tr("Up next"), "${due.size}") }
                items(due, key = { it.id }) { word ->
                    WordListRow(word = word, showsReviewMeta = false, onClick = { onOpenWord(word) })
                }
            }
        }
    }
}

@Composable
private fun ReviewHero(dueCount: Int, streak: Int, onStart: () -> Unit) {
    GradientHero {
        Text("✨  ${tr("Ready to review")}", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
        Text(tr("%lld word(s) waiting", dueCount), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            if (streak > 0) tr("%lld-day streak — keep it going!", streak)
            else tr("A quick session keeps vocabulary fresh."),
            color = Color.White.copy(alpha = 0.85f),
        )
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(tr("Start Review"), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("$dueCount", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BoxWithBackground(
    applyStatusBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Box(
            Modifier
                .fillMaxSize()
                .then(if (applyStatusBars) Modifier.statusBarsPadding() else Modifier),
        ) {
            content()
        }
    }
}
