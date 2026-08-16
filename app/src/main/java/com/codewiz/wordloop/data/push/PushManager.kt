package com.codewiz.wordloop.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.codewiz.wordloop.BuildConfig
import com.codewiz.wordloop.data.api.RegisterDeviceBody
import com.codewiz.wordloop.data.api.UnregisterDeviceBody
import com.codewiz.wordloop.data.api.UpdateNotificationPreferencesBody
import com.codewiz.wordloop.data.api.WordLoopApi
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

@Singleton
class PushManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: WordLoopApi,
    private val prefs: UserPrefs,
    private val messaging: FirebaseMessaging,
) {
    private val mutex = Mutex()
    private var registeredKey: String? = null

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Word Loop",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun registerPreferences(
        reviewReminders: Boolean,
        marketing: Boolean,
        wordOfTheDay: Boolean = false,
    ) {
        prefs.setReviewRemindersEnabled(reviewReminders)
        prefs.setMarketingEnabled(marketing)
        prefs.setWordOfTheDayEnabled(wordOfTheDay)
        runCatching {
            api.updateNotificationPreferences(
                UpdateNotificationPreferencesBody(
                    reviewRemindersEnabled = reviewReminders,
                    marketingEnabled = marketing,
                    wordOfTheDayEnabled = wordOfTheDay,
                    timezone = TimeZone.getDefault().id,
                ),
            )
        }
        if (reviewReminders || marketing || wordOfTheDay) {
            registerCurrentToken()
        }
    }

    suspend fun syncOnForeground() {
        if (prefs.anyNotificationPreferenceEnabled()) {
            registerCurrentToken()
        }
        runCatching {
            val remote = api.getNotificationPreferences().data ?: return
            prefs.setReviewRemindersEnabled(remote.reviewRemindersEnabled)
            prefs.setMarketingEnabled(remote.marketingEnabled)
            prefs.setWordOfTheDayEnabled(remote.wordOfTheDayEnabled)
            if (remote.timezone != TimeZone.getDefault().id) {
                api.updateNotificationPreferences(
                    UpdateNotificationPreferencesBody(
                        reviewRemindersEnabled = remote.reviewRemindersEnabled,
                        marketingEnabled = remote.marketingEnabled,
                        wordOfTheDayEnabled = remote.wordOfTheDayEnabled,
                        timezone = TimeZone.getDefault().id,
                    ),
                )
            }
            if (remote.reviewRemindersEnabled || remote.marketingEnabled || remote.wordOfTheDayEnabled) {
                registerCurrentToken()
            }
        }
    }

    suspend fun registerCurrentToken() {
        mutex.withLock {
            val token = runCatching { messaging.token.await() }.getOrNull() ?: return
            val key = "${prefs.baseUrl()}|$token"
            if (registeredKey == key) return
            val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            runCatching {
                api.registerDevice(
                    RegisterDeviceBody(
                        fcmToken = token,
                        platform = "android",
                        appVersion = version,
                    ),
                )
                registeredKey = key
            }
        }
    }

    suspend fun signOut() {
        val token = runCatching { messaging.token.await() }.getOrNull()
        if (token != null) {
            runCatching { api.unregisterDevice(UnregisterDeviceBody(token)) }
        }
        registeredKey = null
        runCatching { messaging.deleteToken().await() }
    }

    companion object {
        const val CHANNEL_ID = "word_loop_default"
        const val EXTRA_TYPE = "type"
        const val TYPE_REVIEW = "review_reminder"
        const val TYPE_WOTD = "word_of_the_day"
    }
}
