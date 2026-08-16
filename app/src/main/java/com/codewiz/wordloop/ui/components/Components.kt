package com.codewiz.wordloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordStatus
import com.codewiz.wordloop.domain.model.intervalDescription
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.accentColor
import com.codewiz.wordloop.ui.theme.accentHeroGradient
import com.codewiz.wordloop.ui.theme.tr
import com.codewiz.wordloop.util.relativeDateLabel

@Composable
fun ScreenBackground(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(1200f, 80f),
                    radius = 900f,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFF9500).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(80f, 40f),
                    radius = 800f,
                ),
            ),
    )
}

@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, WlDesign.heroShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            .clip(WlDesign.heroShape)
            .background(accentHeroGradient())
            .padding(22.dp),
        content = content,
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(WlDesign.cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}

@Composable
fun EmptyStateCard(title: String, message: String, icon: ImageVector, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun FilterChip(title: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
    }
    Text(
        title,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun WordStatusBadge(status: WordStatus) {
    val color = status.accentColor()
    val icon = when (status) {
        WordStatus.NEW -> Icons.Default.AutoAwesome
        WordStatus.LEARNING -> Icons.Default.Book
        WordStatus.DIFFICULT -> Icons.Default.Error
        WordStatus.MASTERED -> Icons.Default.CheckCircle
        WordStatus.ARCHIVED -> Icons.Default.Inventory2
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(tr(status.displayName), color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WordListRow(
    word: LearnedWord,
    showsReviewMeta: Boolean = true,
    showsLanguage: Boolean = false,
    showsChevron: Boolean = true,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val status = word.wordStatus
    Row(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.rowShape)
            .background(if (compact) Color.Transparent else MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(if (compact) 36.dp else 44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(status.accentColor()),
        )
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(word.word, fontWeight = FontWeight.SemiBold, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                if (showsLanguage) {
                    Text(
                        word.language,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            val subtitle = word.partOfSpeech
                ?.takeIf { it.isNotBlank() }
                ?.let { "${it.replaceFirstChar(Char::titlecase)} · ${word.shortMeaning}" }
                ?: word.shortMeaning
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showsReviewMeta && word.isReviewable && !word.nextReviewDate.isNullOrBlank()) {
                Text(
                    "${relativeDateLabel(word.nextReviewDate)} · ${intervalDescription(word.interval)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
        WordStatusBadge(status)
        if (showsChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
fun MetricBar(metrics: List<Metric>) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WlDesign.metricRadius))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(WlDesign.metricRadius))
            .padding(vertical = 16.dp),
    ) {
        metrics.forEachIndexed { index, metric ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(metric.icon, contentDescription = null, tint = metric.tint)
                Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    }
}

data class Metric(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun LoadingOverlay(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoPill(text: String, icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(items: List<String>, tint: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Text(
                item,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SettingsIconBadge(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(WlDesign.cardShape)
                .background(MaterialTheme.colorScheme.surface),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
