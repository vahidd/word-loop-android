package com.codewiz.wordloop.data.analytics

import com.codewiz.wordloop.data.api.TrackEventApi
import com.codewiz.wordloop.data.api.TrackEventBody
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
    private val auth: FirebaseAuth,
    private val trackEventApi: TrackEventApi,
) {
    enum class Event(val raw: String) {
        APP_REFRESH("app_refresh"),
        WORD_ADDED("word_added"),
        WORD_OF_THE_DAY_ADDED("word_of_the_day_added"),
        REVIEW_SUBMITTED("review_submitted"),
        SIGN_OUT("sign_out"),
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun identifyCurrentUser() {
        val uid = auth.currentUser?.uid
        analytics.setUserId(uid)
        crashlytics.setUserId(uid.orEmpty())
    }

    fun clearIdentity() {
        analytics.setUserId(null)
        crashlytics.setUserId("")
    }

    fun log(event: Event) {
        analytics.logEvent(event.raw, null)
    }

    fun record(error: Throwable) {
        crashlytics.recordException(error)
    }

    fun trackAppOpen(appVersion: String, buildNumber: String, appId: String) {
        scope.launch {
            runCatching {
                trackEventApi.track(
                    TrackEventBody(
                        event = "app_open",
                        meta = mapOf(
                            "appVersion" to appVersion,
                            "buildNumber" to buildNumber,
                            "appId" to appId,
                        ),
                    ),
                )
            }
        }
    }
}
