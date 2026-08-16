package com.codewiz.wordloop.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.codewiz.wordloop.data.prefs.UserPrefs
import dagger.hilt.android.EntryPointAccessors

class WordOfTheDayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val word = runCatching {
            val prefs = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetDeps::class.java,
            ).prefs()
            prefs.readProfileCache().learningLanguages.firstOrNull()
        }.getOrNull()
        provideContent {
            GlanceTheme {
                WidgetContent(subtitle = word ?: "Word Loop")
            }
        }
    }
}

@Composable
private fun WidgetContent(subtitle: String) {
    Column(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.primaryContainer)
            .padding(16.dp),
    ) {
        Text("Word of the Day", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Text("Open Word Loop", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Text(subtitle, style = TextStyle(fontSize = 13.sp))
    }
}

class WordOfTheDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordOfTheDayWidget()
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WidgetDeps {
    fun prefs(): UserPrefs
}
