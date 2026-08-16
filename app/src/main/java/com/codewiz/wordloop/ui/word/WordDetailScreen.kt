package com.codewiz.wordloop.ui.word

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.data.audio.PronunciationPlayer
import com.codewiz.wordloop.data.review.ReviewRequestManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.intervalDescription
import com.codewiz.wordloop.ui.components.ChipFlow
import com.codewiz.wordloop.ui.components.InfoPill
import com.codewiz.wordloop.ui.components.SectionCard
import com.codewiz.wordloop.ui.components.WordStatusBadge
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.accentColor
import com.codewiz.wordloop.ui.theme.accentHeroGradient
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.ui.today.BoxWithBackground
import com.codewiz.wordloop.util.AppConstants
import com.codewiz.wordloop.util.relativeDateLabel
import kotlinx.coroutines.launch

@Composable
fun WordDetailScreen(
    initial: LearnedWord,
    list: List<LearnedWord>,
    store: WordLoopStore,
    player: PronunciationPlayer,
    reviewRequests: ReviewRequestManager,
    userId: String?,
    onPractice: (LearnedWord) -> Unit,
    showDone: Boolean = false,
    applyStatusBars: Boolean = true,
    onDone: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val words by store.words.collectAsState()
    val wordList = list.ifEmpty { listOf(initial) }
    var index by remember { mutableIntStateOf(wordList.indexOfFirst { it.id == initial.id }.coerceAtLeast(0)) }
    val current = wordList.getOrNull(index) ?: initial
    val display = words.firstOrNull { it.id == current.id } ?: current
    val native = store.userProfile.value?.nativeLanguage
    val showNative = !native.isNullOrBlank() && !native.equals("English", true) && display.meaningsInNativeLang.isNotEmpty()
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { reviewRequests.recordWordDetailViewed() }

    BoxWithBackground(applyStatusBars = applyStatusBars) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (applyStatusBars) Modifier.navigationBarsPadding() else Modifier),
        ) {
            if (showDone && onDone != null) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Word Added"), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onDone) { Text(tr("Done")) }
                }
            } else if (onBack != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = tr("Back"),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .padding(6.dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .clipToBounds()
                    .verticalScroll(rememberScrollState())
                    .padding(WlDesign.screenPadding)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
            ) {
                SelectionContainer {
                    Text(display.word, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(WlDesign.heroShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WordStatusBadge(display.wordStatus)
                                display.difficulty?.let {
                                    Text(
                                        it.replaceFirstChar(Char::titlecase),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(display.wordStatus.accentColor().copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            InfoPill(display.language, Icons.Default.Language)
                            display.partOfSpeech?.let {
                                Text(it, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            }
                        }
                        Box(
                            Modifier
                                .size(44.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(accentHeroGradient())
                                .clickable { player.play(display) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = tr("Pronunciation"), tint = Color.White)
                        }
                    }
                    if (display.quizzes.isNotEmpty()) {
                        TextButton(onClick = { onPractice(display) }) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Text(tr("Practice Quiz"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SectionCard(tr("Meaning")) {
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(display.shortMeaning, fontWeight = FontWeight.Medium)
                            Text(display.detailedMeaning, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                if (display.otherMeanings.isNotEmpty()) {
                    SectionCard(tr("Other Meanings")) {
                        display.otherMeanings.forEach { meaning ->
                            Text(meaning.shortMeaning, fontWeight = FontWeight.Medium)
                            Text(meaning.partOfSpeech, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
                            Text(meaning.detailedMeaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                if (showNative) {
                    SectionCard(tr("Meanings in %@", native!!)) {
                        display.meaningsInNativeLang.forEach { Text("•  $it") }
                    }
                }
                if (display.pronunciationIpa != null || display.pronunciationSimple != null) {
                    SectionCard(tr("Pronunciation")) {
                        display.pronunciationIpa?.let { Text("${tr("IPA:")} $it") }
                        display.pronunciationSimple?.let { Text("${tr("Simple:")} $it") }
                    }
                }
                if (display.examples.isNotEmpty()) {
                    SectionCard(tr("Examples")) {
                        display.examples.forEach { example ->
                            Text(example.sentence, fontStyle = FontStyle.Italic)
                            Text(example.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        TextButton(
                            onClick = {
                                generating = true
                                scope.launch {
                                    runCatching { store.generateMoreExamples(display) }
                                    generating = false
                                }
                            },
                            enabled = !generating,
                        ) {
                            if (generating) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.AddCircle, contentDescription = null)
                            Text(if (generating) tr("Generating…") else tr("More Examples"))
                        }
                    }
                }
                if (display.synonyms.isNotEmpty() || display.antonyms.isNotEmpty()) {
                    SectionCard(tr("Synonyms & Antonyms")) {
                        if (display.synonyms.isNotEmpty()) {
                            Text(tr("Synonyms"), style = MaterialTheme.typography.labelMedium)
                            ChipFlow(display.synonyms, Color(0xFF34C759))
                        }
                        if (display.antonyms.isNotEmpty()) {
                            Text(tr("Antonyms"), style = MaterialTheme.typography.labelMedium)
                            ChipFlow(display.antonyms, Color(0xFFFF3B30))
                        }
                    }
                }
                if (display.commonPhrases.isNotEmpty()) {
                    SectionCard(tr("Common Phrases")) {
                        display.commonPhrases.forEach { phrase ->
                            Text(phrase.phrase, fontWeight = FontWeight.Medium)
                            Text(phrase.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                display.memoryHint?.takeIf { it.isNotBlank() }?.let { hint ->
                    SectionCard(tr("Memory Hint")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFF9500))
                            Text(hint, color = Color(0xFFFF9500))
                        }
                    }
                }
                if (AppConstants.isTestUser(userId)) {
                    SectionCard("Review Status") {
                        Text("Consecutive Successes  ${display.repetitionCount}")
                        Text("Current Interval  ${intervalDescription(display.interval)}")
                        Text("Easiness Factor  ${"%.2f".format(display.easinessFactor)}")
                        Text("Correct  ${display.correctAnswersCount}")
                        Text("Wrong  ${display.wrongAnswersCount}")
                        display.nextReviewDate?.let { Text("Next Review  ${relativeDateLabel(it)}") }
                    }
                }
            }
            if (wordList.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = tr("Previous"),
                        modifier = Modifier.clickable(enabled = index > 0) { if (index > 0) index-- },
                    )
                    Text("${index + 1}/${wordList.size}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = tr("Next"),
                        modifier = Modifier.clickable(enabled = index < wordList.lastIndex) { if (index < wordList.lastIndex) index++ },
                    )
                }
            }
        }
    }
}
