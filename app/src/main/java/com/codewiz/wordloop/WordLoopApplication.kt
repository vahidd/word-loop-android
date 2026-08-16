package com.codewiz.wordloop

import android.app.Application
import com.codewiz.wordloop.data.push.PushManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WordLoopApplication : Application() {
    @Inject lateinit var pushManager: PushManager

    override fun onCreate() {
        super.onCreate()
        pushManager.createChannel()
    }
}
