package com.codewiz.wordloop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewiz.wordloop.data.analytics.AnalyticsTracker
import com.codewiz.wordloop.data.push.PushManager
import com.codewiz.wordloop.ui.navigation.WordLoopRoot
import com.codewiz.wordloop.ui.theme.WordLoopTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var analytics: AnalyticsTracker

    var pendingPushType by mutableStateOf<String?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingPushType = intent?.getStringExtra(PushManager.EXTRA_TYPE) ?: intent?.data?.let { "review_reminder" }
        analytics.trackAppOpen(
            appVersion = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.VERSION_CODE.toString(),
            appId = BuildConfig.APPLICATION_ID,
        )
        setContent {
            WordLoopTheme {
                val pushType = pendingPushType
                LaunchedEffect(pushType) { /* consumed in root */ }
                WordLoopRoot(
                    pendingPushType = pushType,
                    onPushConsumed = { pendingPushType = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPushType = intent.getStringExtra(PushManager.EXTRA_TYPE)
            ?: intent.data?.let { "review_reminder" }
    }
}
