package com.codewiz.wordloop.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.data.audio.PronunciationPlayer
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.ui.components.EmptyStateCard
import com.codewiz.wordloop.ui.components.Metric
import com.codewiz.wordloop.ui.components.MetricBar
import com.codewiz.wordloop.ui.components.ScreenBackground
import com.codewiz.wordloop.ui.components.SectionHeader
import com.codewiz.wordloop.ui.components.WordListRow
import com.codewiz.wordloop.ui.theme.OrangeAccent
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import kotlinx.coroutines.launch

private val letters = listOf("A", "B", "C", "D")

@Composable
fun QuizScreen(
    words: List<LearnedWord>,
    mode: QuizViewModel.Mode,
    viewModel: QuizViewModel,
    player: PronunciationPlayer,
    onClose: () -> Unit,
    onOpenWord: (LearnedWord) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(words, mode) { viewModel.prepare(words, mode) }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when (state.phase) {
                QuizViewModel.Phase.SUMMARY -> QuizSummary(state, onClose, onOpenWord)
                else -> {
                    val item = state.currentItem
                    if (item == null) {
                        EmptyStateCard(
                            title = tr("No quizzes available"),
                            message = tr("These words don't have quiz data yet."),
                            icon = Icons.Default.FrontHand,
                            accent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            state.errorMessage?.let { message ->
                                Text(
                                    message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = WlDesign.screenPadding, vertical = 8.dp),
                                )
                            }
                            QuizHeader(
                                current = state.currentIndex,
                                total = state.items.size,
                                progress = state.progressFraction,
                                onClose = onClose,
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(WlDesign.screenPadding)
                                    .padding(bottom = if (state.phase == QuizViewModel.Phase.SHOWING_FEEDBACK) 220.dp else 32.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                QuizHero(item, onPronounce = { player.play(item.word) })
                                Text(
                                    tr("Choose your answer"),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                item.quiz.options.forEachIndexed { index, option ->
                                    AnswerOption(
                                        letter = letters.getOrElse(index) { "${index + 1}" },
                                        text = option,
                                        state = answerState(state, item, index),
                                        enabled = state.phase == QuizViewModel.Phase.ANSWERING,
                                        onClick = { scope.launch { viewModel.selectAnswer(index) } },
                                    )
                                }
                                TextButton(
                                    onClick = { scope.launch { viewModel.skipAnswer() } },
                                    enabled = state.phase == QuizViewModel.Phase.ANSWERING,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.PanTool, contentDescription = null)
                                    Text(tr("I don't know"), modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = state.phase == QuizViewModel.Phase.SHOWING_FEEDBACK,
                            enter = slideInVertically { it } + fadeIn(),
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            FeedbackSheet(
                                isCorrect = state.isCorrect == true,
                                didSkip = state.didSkip,
                                explanation = item.quiz.explanation,
                                isLast = state.isLastQuestion,
                                onContinue = { scope.launch { viewModel.continueToNext() } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizHeader(current: Int, total: Int, progress: Float, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                strokeCap = StrokeCap.Round,
            )
            Text("${current + 1} of $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(44.dp)) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f),
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF007AFF), OrangeAccent)),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f, cap = StrokeCap.Round),
                )
            }
            Text("${(progress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuizHero(item: QuizViewModel.QuizItem, onPronounce: () -> Unit) {
    val type = item.quiz.type.lowercase()
    val label = when (type) {
        "context", "usage" -> "Real-world scenario"
        "meaning" -> "What does it mean?"
        "translation" -> "Pick the translation"
        else -> "Your challenge"
    }
    val showQuestion = type == "context" || type == "usage"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.heroShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        OrangeAccent.copy(alpha = 0.85f),
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(item.word.word, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onPronounce),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
        Text(label, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        if (showQuestion) {
            Text(item.quiz.question, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private enum class AnswerVisual { IDLE, CORRECT, INCORRECT, DIMMED }

private fun answerState(state: QuizViewModel.UiState, item: QuizViewModel.QuizItem, index: Int): AnswerVisual {
    if (state.phase != QuizViewModel.Phase.SHOWING_FEEDBACK) return AnswerVisual.IDLE
    if (index == item.quiz.correctOptionIndex) return AnswerVisual.CORRECT
    if (index == state.selectedOptionIndex) return AnswerVisual.INCORRECT
    return AnswerVisual.DIMMED
}

@Composable
private fun AnswerOption(letter: String, text: String, state: AnswerVisual, enabled: Boolean, onClick: () -> Unit) {
    val border = when (state) {
        AnswerVisual.CORRECT -> Color(0xFF34C759)
        AnswerVisual.INCORRECT -> Color(0xFFFF3B30)
        else -> Color.Transparent
    }
    val alpha = if (state == AnswerVisual.DIMMED) 0.4f else 1f
    val letterBg = when (letter) {
        "A" -> Color(0xFF007AFF).copy(alpha = 0.15f)
        "B" -> Color(0xFFAF52DE).copy(alpha = 0.15f)
        "C" -> Color(0xFFFF9500).copy(alpha = 0.15f)
        else -> Color(0xFF5AC8FA).copy(alpha = 0.15f)
    }
    val letterFg = when (letter) {
        "A" -> Color(0xFF007AFF)
        "B" -> Color(0xFFAF52DE)
        "C" -> Color(0xFFFF9500)
        else -> Color(0xFF32ADE6)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.rowShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
            .border(if (border == Color.Transparent) 0.dp else 2.dp, border, WlDesign.rowShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(letterBg),
            contentAlignment = Alignment.Center,
        ) { Text(letter, color = letterFg, fontWeight = FontWeight.Bold) }
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        when (state) {
            AnswerVisual.CORRECT -> Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34C759))
            AnswerVisual.INCORRECT -> Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF3B30))
            else -> {}
        }
    }
}

@Composable
private fun FeedbackSheet(
    isCorrect: Boolean,
    didSkip: Boolean,
    explanation: String,
    isLast: Boolean,
    onContinue: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .align(Alignment.CenterHorizontally),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when {
                    isCorrect -> "Nice work!"
                    didSkip -> "No worries!"
                    else -> "Not quite"
                },
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF34C759) else OrangeAccent,
            )
            Text(
                when {
                    isCorrect -> "You nailed this one."
                    didSkip -> "Here's what to learn."
                    else -> "Here's what to remember."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Text(explanation, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCorrect) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                if (isLast) tr("See Results") else tr("Next Question"),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun QuizSummary(
    state: QuizViewModel.UiState,
    onDone: () -> Unit,
    onOpenWord: (LearnedWord) -> Unit,
) {
    val result = state.result
    val ratio = if (result.totalQuestions == 0) 0.0 else result.correctCount.toDouble() / result.totalQuestions
    val color = when {
        ratio >= 0.8 -> Color(0xFFFFCC00)
        ratio >= 0.5 -> Color(0xFF007AFF)
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WlDesign.screenPadding)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
    ) {
        Text(
            if (state.mode == QuizViewModel.Mode.PRACTICE) tr("Practice Complete") else tr("Review Complete"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(WlDesign.heroShape)
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
            Text(
                QuizViewModel.resultTitle(result.correctCount, result.totalQuestions),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("${result.correctCount} of ${result.totalQuestions} correct", color = Color.White.copy(alpha = 0.85f))
        }
        val metrics = buildList {
            add(Metric("${result.correctCount}", "Correct", Icons.Default.CheckCircle, Color(0xFF34C759)))
            add(Metric("${result.wrongCount}", "Wrong", Icons.Default.Close, Color(0xFFFF3B30)))
            if (result.skippedCount > 0) add(Metric("${result.skippedCount}", "Skipped", Icons.Default.PanTool, Color.Gray))
            if (state.mode == QuizViewModel.Mode.REVIEW) add(Metric("+${result.xpEarned}", "XP", Icons.Default.Check, OrangeAccent))
        }
        MetricBar(metrics)
        if (result.failedWords.isNotEmpty()) {
            SectionHeader("Missed words")
            result.failedWords.forEach { failed ->
                WordListRow(failed.word, showsReviewMeta = false, onClick = { onOpenWord(failed.word) })
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(tr("Done")) }
    }
}
