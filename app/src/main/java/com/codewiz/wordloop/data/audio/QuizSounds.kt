package com.codewiz.wordloop.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.codewiz.wordloop.R
import com.codewiz.wordloop.data.prefs.UserPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class QuizSounds @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: UserPrefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val correctId = pool.load(context, R.raw.correct, 1)
    private val wrongId = pool.load(context, R.raw.wrong, 1)

    fun play(correct: Boolean) {
        scope.launch {
            if (!prefs.soundEffectsEnabledValue()) return@launch
            pool.play(if (correct) correctId else wrongId, 1f, 1f, 1, 0, 1f)
        }
    }
}
