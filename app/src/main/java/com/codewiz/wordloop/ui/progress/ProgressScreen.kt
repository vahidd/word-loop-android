package com.codewiz.wordloop.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.WordStatus
import com.codewiz.wordloop.ui.components.Metric
import com.codewiz.wordloop.ui.components.MetricBar
import com.codewiz.wordloop.ui.components.SectionCard
import com.codewiz.wordloop.ui.components.SectionHeader
import com.codewiz.wordloop.ui.theme.OrangeAccent
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.ui.theme.xpHeroGradient
import com.codewiz.wordloop.ui.today.BoxWithBackground

@Composable
fun ProgressScreen(store: WordLoopStore) {
    val words by store.words.collectAsState()
    val progress by store.progress.collectAsState()
    val learning = words.count { it.wordStatus == WordStatus.LEARNING }
    val difficult = words.count { it.wordStatus == WordStatus.DIFFICULT }
    val mastered = words.count { it.wordStatus == WordStatus.MASTERED }
    val correct = progress?.totalCorrectAnswers ?: 0
    val wrong = progress?.totalWrongAnswers ?: 0
    val accuracy = if (correct + wrong == 0) 0.0 else correct.toDouble() / (correct + wrong) * 100
    val accuracyColor = when {
        accuracy >= 80 -> Color(0xFF34C759)
        accuracy >= 50 -> OrangeAccent
        else -> Color(0xFFFF3B30)
    }

    BoxWithBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = WlDesign.screenPadding, end = WlDesign.screenPadding, top = WlDesign.screenPadding, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(WlDesign.sectionSpacing),
        ) {
            item {
                Column {
                    Text(tr("Progress"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(tr("Your learning journey at a glance"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(com.codewiz.wordloop.ui.theme.WlDesign.heroShape)
                        .background(xpHeroGradient())
                        .padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Text(tr("Total XP"), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                    }
                    Text("${progress?.xp ?: 0}", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    val sessions = progress?.totalReviewSessions ?: 0
                    Text(tr("%lld review session%@ completed", sessions, if (sessions == 1) "" else "s"), color = Color.White.copy(alpha = 0.85f))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(tr("Library breakdown"))
                    MetricBar(
                        listOf(
                            Metric("${words.size}", tr("Total"), Icons.Default.TextFields, Color(0xFF007AFF)),
                            Metric("$learning", tr("Learning"), Icons.Default.Book, OrangeAccent),
                            Metric("$difficult", tr("Difficult"), Icons.Default.Error, Color(0xFFFF3B30)),
                            Metric("$mastered", tr("Mastered"), Icons.Default.CheckCircle, Color(0xFF34C759)),
                        ),
                    )
                }
            }
            item {
                SectionCard(tr("Accuracy")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(120.dp)) {
                                drawArc(Color.LightGray.copy(alpha = 0.3f), -90f, 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                drawArc(accuracyColor, -90f, (accuracy / 100.0 * 360).toFloat(), false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${accuracy.toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("$correct correct answers", color = Color(0xFF34C759))
                            Text("$wrong wrong answers", color = Color(0xFFFF3B30))
                            Text(tr("Accuracy updates as you complete reviews."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(tr("Streaks & activity"))
                    MetricBar(
                        listOf(
                            Metric("${progress?.currentStreak ?: 0}", tr("Current"), Icons.Default.LocalFireDepartment, OrangeAccent),
                            Metric("${progress?.bestStreak ?: 0}", tr("Best"), Icons.Default.EmojiEvents, Color(0xFFFFCC00)),
                            Metric("${progress?.totalReviewSessions ?: 0}", tr("Reviews"), Icons.Default.Repeat, Color(0xFF007AFF)),
                        ),
                    )
                }
            }
        }
    }
}

