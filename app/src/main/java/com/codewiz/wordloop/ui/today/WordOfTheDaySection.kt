package com.codewiz.wordloop.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codewiz.wordloop.data.audio.PronunciationPlayer
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.WordOfTheDay
import com.codewiz.wordloop.ui.theme.WlDesign
import com.codewiz.wordloop.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WordOfTheDaySection(store: WordLoopStore, player: PronunciationPlayer) {
    val active by store.wordOfTheDayActive.collectAsState()
    val words by store.wordOfTheDay.collectAsState()
    val library by store.words.collectAsState()
    if (!active || words.isEmpty()) return
    var selectedId by remember { mutableStateOf(words.first().id) }
    val selected = words.firstOrNull { it.id == selectedId } ?: words.first()
    var phase by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val inLibrary = library.any { it.normalizedWord == selected.normalizedWord && it.language == selected.language }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(WlDesign.heroShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), WlDesign.heroShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Text(tr("Word of the Day").uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        if (words.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(words, key = { it.id }) { suggestion ->
                    val selectedChip = suggestion.id == selected.id
                    Text(
                        suggestion.language,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedChip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable { selectedId = suggestion.id }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (selectedChip) Color.White else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(selected.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val pron = listOfNotNull(selected.pronunciation?.ipa, selected.pronunciation?.simple).joinToString(" · ")
                if (pron.isNotBlank()) Text(pron, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = tr("Pronunciation"),
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { player.play(selected) }
                    .padding(8.dp),
            )
        }
        if (selected.shortMeaning.isNotBlank()) Text(selected.shortMeaning)
        selected.examples.firstOrNull()?.let { example ->
            Text(example.sentence, fontStyle = FontStyle.Italic)
            Text(example.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        if (!inLibrary || phase != null) {
            Button(
                onClick = {
                    if (phase != null) return@Button
                    phase = "adding"
                    error = null
                    scope.launch {
                        val result = runCatching { store.createWord(selected.word, selected.language) }
                        val ok = result.isSuccess ||
                            library.any { it.normalizedWord == selected.normalizedWord && it.language == selected.language }
                        if (!ok) {
                            error = result.exceptionOrNull()?.message ?: "Couldn't add that word. Try again."
                            phase = null
                            return@launch
                        }
                        phase = "added"
                        delay(1600)
                        phase = null
                    }
                },
                enabled = phase != "adding",
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (phase == "adding") CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (phase == "added") tr("Added") else tr("Learn this word"))
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
