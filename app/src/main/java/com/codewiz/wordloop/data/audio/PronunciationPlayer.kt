package com.codewiz.wordloop.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordOfTheDay
import com.codewiz.wordloop.util.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class PronunciationPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = AtomicBoolean(false)
    private var playCount = 0
    private var lastWordId: String? = null
    private var downloadJob: Job? = null

    private val cacheDir: File by lazy {
        File(context.cacheDir, "PronunciationAudio").apply { mkdirs() }
    }

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady.set(status == TextToSpeech.SUCCESS)
        }
    }

    fun play(word: LearnedWord) {
        playInternal(word.id, word.word, word.language, word.pronunciationAudioUrl)
    }

    fun play(wordOfTheDay: WordOfTheDay) {
        playInternal(
            wordOfTheDay.id,
            wordOfTheDay.word,
            wordOfTheDay.language,
            wordOfTheDay.pronunciation?.audioUrl,
        )
    }

    fun stop() {
        downloadJob?.cancel()
        mediaPlayer?.runCatching { if (isPlaying) stop(); reset(); release() }
        mediaPlayer = null
        tts?.stop()
    }

    private fun playInternal(id: String, text: String, language: String, audioUrl: String?) {
        if (id != lastWordId) {
            playCount = 0
            lastWordId = id
        }
        playCount += 1
        val speed = when (playCount) {
            1 -> 1.0f
            2 -> 0.75f
            else -> 0.5f
        }
        if (!audioUrl.isNullOrBlank()) {
            playRemote(audioUrl, text, language, speed)
        } else {
            playTts(text, language, speed)
        }
    }

    private fun playRemote(url: String, text: String, language: String, speed: Float) {
        stop()
        val cached = cachedFile(url)
        if (cached.exists()) {
            startPlayback(cached, speed)
            return
        }
        downloadJob = scope.launch {
            val fallback = launch {
                delay(8_000)
                playTts(text, language, speed)
            }
            val downloaded = withContext(Dispatchers.IO) {
                runCatching { download(url, cached) }.getOrDefault(false)
            }
            if (downloaded && cached.exists()) {
                fallback.cancel()
                startPlayback(cached, speed)
            } else if (!fallback.isCompleted) {
                fallback.cancel()
                playTts(text, language, speed)
            }
        }
    }

    private fun startPlayback(file: File, speed: Float) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setDataSource(file.absolutePath)
            setOnPreparedListener {
                playbackParams = playbackParams.setSpeed(speed)
                start()
            }
            setOnErrorListener { _, _, _ ->
                playTts(lastWordId.orEmpty(), AppConstants.DEFAULT_WORD_LANGUAGE, speed)
                true
            }
            prepareAsync()
        }
    }

    private fun playTts(text: String, language: String, speed: Float) {
        val engine = tts ?: return
        if (!ttsReady.get()) return
        val bcp47 = AppConstants.languageBcp47[language]
        if (bcp47 != null) {
            engine.language = Locale.forLanguageTag(bcp47)
        }
        engine.setSpeechRate(speed)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    private fun download(url: String, dest: File): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        return try {
            if (connection.responseCode !in 200..299) return false
            connection.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.exists() && dest.length() > 0
        } finally {
            connection.disconnect()
        }
    }

    private fun cachedFile(url: String): File {
        var hash = 5381L
        url.forEach { hash = (hash * 33) + it.code }
        val ext = url.substringAfterLast('.', "audio").takeWhile { it.isLetterOrDigit() }.ifEmpty { "audio" }
        return File(cacheDir, "${hash.toULong()}.$ext")
    }
}
